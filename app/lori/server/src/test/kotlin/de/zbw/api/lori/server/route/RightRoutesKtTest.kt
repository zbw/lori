package de.zbw.api.lori.server.route

import com.google.gson.Gson
import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import de.zbw.api.lori.server.ServicePoolWithProbes
import de.zbw.api.lori.server.config.LoriConfiguration
import de.zbw.api.lori.server.type.SamlUtils
import de.zbw.api.lori.server.type.toBusiness
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.lori.model.AccessStateRest
import de.zbw.lori.model.RightRest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test
import java.lang.reflect.Type
import java.sql.SQLException
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class RightRoutesKtTest {
    @Test
    fun testGetMetadataOK() {
        // given
        val rightId = "someId"
        val expected = TEST_RIGHT
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                every { getRightsByIds(listOf(rightId)) } returns listOf(expected.toBusiness())
            }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.get("/api/v1/right/$rightId")
            val content: String = response.bodyAsText()
            val groupListType: Type = object : TypeToken<RightRest>() {}.type
            val received: RightRest = GSON.fromJson(content, groupListType)
            assertThat(received, `is`(expected))
        }
    }

    @Test
    fun testGetMetadataNotFound() {
        // given
        val testId = "someId"
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                every { getRightsByIds(listOf(testId)) } returns emptyList()
            }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.get("/api/v1/right/$testId")
            assertThat(
                "Should return NotFound",
                response.status,
                `is`(HttpStatusCode.NotFound),
            )
        }
    }

    @Test
    fun testGetMetadataInternal() {
        // given
        val rightId = "someId"
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                every { getRightsByIds(listOf(rightId)) } throws SQLException()
            }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.get("/api/v1/right/$rightId")
            assertThat(
                "Should return InternalServerError",
                response.status,
                `is`(HttpStatusCode.InternalServerError),
            )
        }
    }

    @Test
    fun testDeleteRightOK() {
        // given
        val rightId = "123"
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                every { deleteRight(rightId) } returns 1
                every { deleteItemEntriesByRightId(rightId) } returns 5
            }
        val servicePool = getServicePool(backend)

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.delete("/api/v1/right/$rightId")
            assertThat("Should return OK", response.status, `is`(HttpStatusCode.OK))
            verify(exactly = 1) { backend.deleteItemEntriesByRightId(rightId) }
            verify(exactly = 1) { backend.deleteRight(rightId) }
        }
    }

    @Test
    fun testDeleteRightNotFound() {
        // given
        val rightId = "123"
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                every { deleteRight(rightId) } returns 0
                every { deleteItemEntriesByRightId(rightId) } returns 0
            }
        val servicePool = getServicePool(backend)

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.delete("/api/v1/right/$rightId")
            assertThat("Should return Conflict", response.status, `is`(HttpStatusCode.NotFound))
            verify(exactly = 1) { backend.deleteItemEntriesByRightId(rightId) }
            verify(exactly = 1) { backend.deleteRight(rightId) }
        }
    }

    @Test
    fun testDeleteRightException() {
        // given
        val rightId = "123"
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                every { deleteRight(rightId) } throws SQLException()
            }
        val servicePool = getServicePool(backend)

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.delete("/api/v1/right/$rightId")
            assertThat(
                "Should return 500 because of internal SQL exception",
                response.status,
                `is`(HttpStatusCode.InternalServerError),
            )
        }
    }

    @Test
    fun testPostRightOK() {
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                every { insertRight(any()) } returns "5"
            }
        val servicePool = getServicePool(backend)

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response =
                client.post("/api/v1/right") {
                    header(HttpHeaders.Accept, ContentType.Application.Json)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(jsonAsString(TEST_RIGHT))
                }
            assertThat("Should return 200", response.status, `is`(HttpStatusCode.OK))
        }
    }

    @Test
    fun testPostRightInternalError() {
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                every { insertRight(any()) } throws SQLException()
            }
        val servicePool = getServicePool(backend)
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response =
                client.post("/api/v1/right") {
                    header(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(jsonAsString(TEST_RIGHT))
                }
            assertThat(
                "Should return 500 because of internal SQL exception",
                response.status,
                `is`(HttpStatusCode.InternalServerError),
            )
        }
    }

    @Test
    fun testPostRightBadRequest() {
        val backend = mockk<LoriServerBackend>(relaxed = true)
        val servicePool = getServicePool(backend)

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response =
                client.post("/api/v1/right") {
                    header(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(jsonAsString(ItemRoutesKtTest.TEST_ITEM))
                }
            assertThat(
                "Should return 400 because of wrong JSON data.",
                response.status,
                `is`(HttpStatusCode.BadRequest),
            )
        }
    }

    @Test
    fun testPutRightNoContent() {
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                every { rightContainsId(TEST_RIGHT.rightId!!) } returns true
                every { upsertRight(any()) } returns 1
            }
        val servicePool = getServicePool(backend)

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response =
                client.put("/api/v1/right") {
                    header(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(jsonAsString(TEST_RIGHT))
                }
            assertThat("Should return NO_CONTENT", response.status, `is`(HttpStatusCode.NoContent))
        }
    }

    @Test
    fun testPutRightCreated() {
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                every { rightContainsId(TEST_RIGHT.rightId!!) } returns false
                every { insertRight(any()) } returns "1"
            }
        val servicePool = getServicePool(backend)

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response =
                client.put("/api/v1/right") {
                    header(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(jsonAsString(TEST_RIGHT))
                }
            assertThat("Should return CREATED", response.status, `is`(HttpStatusCode.Created))
        }
    }

    @Test
    fun testPutRightBadRequest() {
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                every { rightContainsId(TEST_RIGHT.rightId!!) } returns true
                every { upsertRight(any()) } returns 1
            }
        val servicePool = getServicePool(backend)

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response =
                client.put("/api/v1/right") {
                    header(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(jsonAsString(ItemRoutesKtTest.TEST_ITEM))
                }
            assertThat("Should return BAD_REQUEST", response.status, `is`(HttpStatusCode.BadRequest))
        }
    }

    @Test
    fun testPutRightInternalError() {
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                every { rightContainsId(TEST_RIGHT.rightId!!) } throws SQLException()
            }
        val servicePool = getServicePool(backend)

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response =
                client.put("/api/v1/right") {
                    header(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(jsonAsString(TEST_RIGHT))
                }
            assertThat(
                "Should return Internal Server Error",
                response.status,
                `is`(HttpStatusCode.InternalServerError),
            )
        }
    }

    companion object {
        private val TODAY: LocalDate = LocalDate.of(2022, 3, 1)

        private val CONFIG =
            LoriConfiguration(
                grpcPort = 9092,
                httpPort = 8080,
                sqlUser = "postgres",
                sqlPassword = "postgres",
                sqlUrl = "jdbc:someurl",
                digitalArchiveAddress = "https://archiveaddress",
                digitalArchiveUsername = "testuser",
                digitalArchivePassword = "password",
                digitalArchiveBasicAuth = "basicauth",
                jwtAudience = "0.0.0.0:8080/ui",
                jwtIssuer = "0.0.0.0:8080",
                jwtRealm = "Lori ui",
                jwtSecret = "foobar",
                duoSenderEntityId = "someId",
                sessionSignKey = "8BADF00DDEADBEAFDEADBAADDEADBAAD",
                sessionEncryptKey = "CAFEBABEDEADBEAFDEADBAADDEFEC8ED",
                stage = "dev",
                handleURL = "https://testdarch.zbw.eu/econis-archiv/handle/",
            )

        val TEST_RIGHT =
            RightRest(
                rightId = "rightId",
                accessState = AccessStateRest.open,
                createdBy = "user1",
                createdOn =
                    OffsetDateTime.of(
                        2022,
                        3,
                        1,
                        1,
                        1,
                        0,
                        0,
                        ZoneOffset.UTC,
                    ),
                endDate = TODAY,
                isTemplate = false,
                lastUpdatedBy = "user2",
                lastUpdatedOn =
                    OffsetDateTime.of(
                        2022,
                        3,
                        2,
                        1,
                        1,
                        0,
                        0,
                        ZoneOffset.UTC,
                    ),
                licenceContract = "some contract",
                nonStandardOpenContentLicence = true,
                nonStandardOpenContentLicenceURL = "https://nonstandardoclurl.de",
                notesGeneral = "Some general notes",
                notesFormalRules = "Some formal rule notes",
                notesProcessDocumentation = "Some process documentation",
                notesManagementRelated = "Some management related notes",
                openContentLicence = "some licence",
                restrictedOpenContentLicence = false,
                startDate = TODAY.minusDays(1),
                zbwUserAgreement = true,
            )

        val GSON: Gson =
            Gson()
                .newBuilder()
                .registerTypeAdapter(
                    LocalDate::class.java,
                    JsonDeserializer { json, _, _ ->
                        LocalDate.parse(json.asString, DateTimeFormatter.ISO_LOCAL_DATE)
                    },
                ).registerTypeAdapter(
                    OffsetDateTime::class.java,
                    JsonDeserializer { json, _, _ ->
                        ZonedDateTime
                            .parse(json.asString, DateTimeFormatter.ISO_ZONED_DATE_TIME)
                            .toOffsetDateTime()
                    },
                ).registerTypeAdapter(
                    OffsetDateTime::class.java,
                    JsonSerializer<OffsetDateTime> { obj, _, _ ->
                        JsonPrimitive(obj.toString())
                    },
                ).registerTypeAdapter(
                    LocalDate::class.java,
                    JsonSerializer<LocalDate> { obj, _, _ ->
                        JsonPrimitive(obj.toString())
                    },
                ).create()

        fun jsonAsString(any: Any): String = GSON.toJson(any)

        val tracer: Tracer = OpenTelemetry.noop().getTracer("de.zbw.api.lori.server.DatabaseConnectorTest")

        fun getServicePool(
            backend: LoriServerBackend,
            samlUtils: SamlUtils = mockk(),
        ) = ServicePoolWithProbes(
            services =
                listOf(
                    mockk {
                        every { isReady() } returns true
                        every { isHealthy() } returns true
                    },
                ),
            config = CONFIG,
            backend = backend,
            tracer = tracer,
            samlUtils = samlUtils,
        )
    }
}
