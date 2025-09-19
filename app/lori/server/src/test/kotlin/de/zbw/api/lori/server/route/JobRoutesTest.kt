package de.zbw.api.lori.server.route

import com.google.gson.reflect.TypeToken
import de.zbw.api.lori.server.route.RightRoutesKtTest.Companion.GSON
import de.zbw.api.lori.server.route.RightRoutesKtTest.Companion.getServicePool
import de.zbw.api.lori.server.route.RightRoutesKtTest.Companion.jsonAsString
import de.zbw.api.lori.server.type.toRest
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.export.ExportJobService
import de.zbw.business.lori.server.type.ExportFormat
import de.zbw.business.lori.server.type.ExportJob
import de.zbw.business.lori.server.type.ExportJobStatus
import de.zbw.lori.model.ExportFormatRest
import de.zbw.lori.model.ItemSearch
import de.zbw.lori.model.JobCreatedRest
import de.zbw.lori.model.JobStatusUpdateRest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test
import java.lang.reflect.Type
import java.util.UUID

class JobRoutesTest {
    @Test
    fun testPostCreate() {
        val expectedExportJob =
            ExportJob(
                id = UUID.randomUUID(),
                status = ExportJobStatus.QUEUED,
                createdBy = "me",
                searchTerm = "foobar",
                format = ExportFormat.CSV,
                errorMessage = null,
                filePath = null,
            )
        val backend = mockk<LoriServerBackend>(relaxed = true)
        val servicePool =
            getServicePool(
                backend,
                exportJobService =
                    mockk<ExportJobService> {
                        every {
                            createJob(
                                any(),
                                any(),
                                any(),
                            )
                        } returns expectedExportJob
                    },
            )

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response: HttpResponse =
                client.post("/api/v1/export/jobs?format=${ExportFormatRest.csv}") {
                    header(HttpHeaders.Accept, ContentType.Application.Json)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        jsonAsString(
                            ItemSearch(
                                searchTerm = expectedExportJob.searchTerm,
                            ),
                        ),
                    )
                }
            assertThat("Should return 201", response.status, `is`(HttpStatusCode.Created))

            val content: String = response.bodyAsText()
            val returnType: Type = object : TypeToken<JobCreatedRest>() {}.type
            val received: JobCreatedRest = GSON.fromJson(content, returnType)

            assertThat(
                received.jobId,
                `is`(expectedExportJob.id.toString()),
            )
        }

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response: HttpResponse =
                client.post("/api/v1/export/jobs?format=${ExportFormatRest.csv}") {
                    header(HttpHeaders.Accept, ContentType.Application.Json)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
            assertThat(
                "Should return 500 because no SearchTerm was provided",
                response.status,
                `is`(HttpStatusCode.InternalServerError),
            )
        }
    }

    @Test
    fun testGetJobStatus() {
        val expectedExportJob =
            ExportJob(
                id = UUID.randomUUID(),
                status = ExportJobStatus.QUEUED,
                createdBy = "me",
                searchTerm = "foobar",
                format = ExportFormat.CSV,
                errorMessage = null,
                filePath = null,
            )

        testApplication {
            val backend =
                mockk<LoriServerBackend>(relaxed = true) {
                    coEvery {
                        getJobById(expectedExportJob.id)
                    } returns expectedExportJob
                }
            val servicePool =
                getServicePool(
                    backend,
                )

            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response: HttpResponse =
                client.get("/api/v1/export/jobs/${expectedExportJob.id}") {
                    header(HttpHeaders.Accept, ContentType.Application.Json)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        jsonAsString(
                            ItemSearch(
                                searchTerm = expectedExportJob.searchTerm,
                            ),
                        ),
                    )
                }
            assertThat("Should return 200", response.status, `is`(HttpStatusCode.OK))

            val content: String = response.bodyAsText()
            val returnType: Type = object : TypeToken<JobStatusUpdateRest>() {}.type
            val received: JobStatusUpdateRest = GSON.fromJson(content, returnType)

            assertThat(
                received.jobId!!,
                `is`(expectedExportJob.id.toString()),
            )
            assertThat(
                received.status,
                `is`(expectedExportJob.status.toRest()),
            )
        }

        // No value found
        testApplication {
            val backend =
                mockk<LoriServerBackend>(relaxed = true) {
                    coEvery {
                        getJobById(expectedExportJob.id)
                    } returns null
                }
            val servicePool =
                getServicePool(
                    backend,
                )

            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response: HttpResponse =
                client.get("/api/v1/export/jobs/${expectedExportJob.id}") {
                    header(HttpHeaders.Accept, ContentType.Application.Json)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        jsonAsString(
                            ItemSearch(
                                searchTerm = expectedExportJob.searchTerm,
                            ),
                        ),
                    )
                }
            assertThat("Should return 404", response.status, `is`(HttpStatusCode.NotFound))
        }
    }
}
