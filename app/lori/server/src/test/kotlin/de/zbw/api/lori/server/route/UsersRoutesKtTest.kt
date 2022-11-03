package de.zbw.api.lori.server.route

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.gson.reflect.TypeToken
import de.zbw.api.lori.server.ServicePoolWithProbes
import de.zbw.api.lori.server.config.LoriConfiguration
import de.zbw.api.lori.server.type.toBusiness
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.type.UserRole
import de.zbw.lori.model.AuthTokenRest
import de.zbw.lori.model.RoleRest
import de.zbw.lori.model.UserRest
import io.ktor.client.request.delete
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
import io.mockk.spyk
import io.mockk.verify
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test
import java.lang.reflect.Type
import java.sql.SQLException
import java.util.Date

class UsersRoutesKtTest {

    @Test
    fun testUsersRegisterOK() {
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { userContainsName(TEST_USER.username) } returns false
            every { insertNewUser(TEST_USER) } returns TEST_USER.username
        }
        val servicePool = getServicePool(backend)
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.post("/api/v1/users/register") {
                header(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_USER))
            }
            assertThat(
                "Should return Accepted",
                response.status,
                `is`(HttpStatusCode.Created)
            )
        }
    }

    @Test
    fun testUsersRegisterConflict() {
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { userContainsName(TEST_USER.username) } returns true
        }
        val servicePool = getServicePool(backend)
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.post("/api/v1/users/register") {
                header(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_USER))
            }
            assertThat(
                "Should return Conflict",
                response.status,
                `is`(HttpStatusCode.Conflict)
            )
        }
    }

    @Test
    fun testUsersRegisterBadRequest() {
        val backend = mockk<LoriServerBackend>(relaxed = true)
        val servicePool = getServicePool(backend)
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.post("/api/v1/users/register") {
                header(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(ItemRoutesKtTest.TEST_ITEM))
            }
            assertThat(
                "Should return BadRequest",
                response.status,
                `is`(HttpStatusCode.BadRequest)
            )
        }
    }

    @Test
    fun testUsersRegisterInternal() {
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { userContainsName(TEST_USER.username) } throws SQLException()
        }
        val servicePool = getServicePool(backend)
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.post("/api/v1/users/register") {
                header(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_USER))
            }
            assertThat(
                "Should return Internal Error",
                response.status,
                `is`(HttpStatusCode.InternalServerError)
            )
        }
    }

    @Test
    fun testUserLoginOK() {
        val expectedResponse =
            AuthTokenRest(token = "FOOBAR")
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { checkCredentials(TEST_USER) } returns true
            every { generateJWT(TEST_USER.username) } returns expectedResponse.token
        }
        val servicePool = getServicePool(backend)
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.post("/api/v1/users/login") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_USER))
            }
            assertThat(
                "Should return OK",
                response.status,
                `is`(HttpStatusCode.OK)
            )
            val content: String = response.bodyAsText()
            val groupListType: Type = object : TypeToken<AuthTokenRest>() {}.type
            val received: AuthTokenRest = RightRoutesKtTest.GSON.fromJson(content, groupListType)
            assertThat(received, `is`(expectedResponse))
        }
    }

    @Test
    fun testUserLoginUnauthorized() {
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { checkCredentials(TEST_USER) } returns false
        }
        val servicePool = getServicePool(backend)
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.post("/api/v1/users/login") {
                header(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_USER))
            }
            assertThat(
                "Should return Unauthorized",
                response.status,
                `is`(HttpStatusCode.Unauthorized)
            )
        }
    }

    @Test
    fun testUserLoginInternalError() {
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { checkCredentials(TEST_USER) } throws SQLException()
        }
        val servicePool = getServicePool(backend)
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.post("/api/v1/users/login") {
                header(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_USER))
            }
            assertThat(
                "Should return Internal Error",
                response.status,
                `is`(HttpStatusCode.InternalServerError)
            )
        }
    }

    @Test
    fun testUpdateUserProperties() {
        val backend = spyk(
            LoriServerBackend(
                dbConnector = mockk(),
                config = CONFIG,
            )
        ) {
            every { checkCredentials(TEST_USER) } returns true
            every { updateUserNonRoleProperties(any()) } returns 1
        }
        val servicePool = getServicePool(backend)
        testApplication {
            application(
                servicePool.application()
            )
            val loginResponse = client.post("/api/v1/users/login") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_USER))
            }
            assertThat(
                "Should return OK",
                loginResponse.status,
                `is`(HttpStatusCode.OK)
            )
            val content: String = loginResponse.bodyAsText()
            val groupListType: Type = object : TypeToken<AuthTokenRest>() {}.type
            val authToken = RightRoutesKtTest.GSON.fromJson<AuthTokenRest?>(content, groupListType).token
            val updateResponse = client.put("/api/v1/users/${TEST_USER.username}") {
                header(HttpHeaders.Authorization, "Bearer $authToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_USER.copy(password = "newPW")))
            }
            assertThat(
                "Return Status should be 204",
                updateResponse.status,
                `is`(HttpStatusCode.NoContent)
            )
        }
    }

    @Test
    fun testUpdateUserPropertyJWTValidationExpired() {
        // 1. Login successfully
        val backend = spyk(
            LoriServerBackend(
                dbConnector = mockk(),
                config = CONFIG,
            )
        ) {
            every { checkCredentials(TEST_USER) } returns true
            every { updateUserNonRoleProperties(any()) } returns 1
            every { generateJWT(any()) } returns JWT.create()
                .withAudience(CONFIG.jwtAudience)
                .withIssuer(CONFIG.jwtIssuer)
                .withClaim("username", TEST_USER.username)
                .withExpiresAt(Date(System.currentTimeMillis() - 60000))
                .sign(Algorithm.HMAC256(CONFIG.jwtSecret))
        }
        val servicePool = getServicePool(backend)
        testApplication {
            application(
                servicePool.application()
            )
            val loginResponse = client.post("/api/v1/users/login") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_USER))
            }
            assertThat(
                "Should return OK",
                loginResponse.status,
                `is`(HttpStatusCode.OK)
            )
            val content: String = loginResponse.bodyAsText()
            val groupListType: Type = object : TypeToken<AuthTokenRest>() {}.type
            val authToken = RightRoutesKtTest.GSON.fromJson<AuthTokenRest?>(content, groupListType).token
            val updateResponse = client.put("/api/v1/users/${TEST_USER.username}") {
                header(HttpHeaders.Authorization, "Bearer $authToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_USER.copy(password = "newPW")))
            }
            assertThat(
                "Return Status should be 401",
                updateResponse.status,
                `is`(HttpStatusCode.Unauthorized)
            )
        }
    }

    @Test
    fun testUpdateUserPropertyJWTValidationUsernameNotEqual() {
        // 1. Login successfully
        val backend = spyk(
            LoriServerBackend(
                dbConnector = mockk(),
                config = CONFIG,
            )
        ) {
            every { checkCredentials(TEST_USER) } returns true
            every { updateUserNonRoleProperties(any()) } returns 1
        }
        val servicePool = getServicePool(backend)
        testApplication {
            application(
                servicePool.application()
            )
            val loginResponse = client.post("/api/v1/users/login") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_USER))
            }
            assertThat(
                "Should return OK",
                loginResponse.status,
                `is`(HttpStatusCode.OK)
            )
            val content: String = loginResponse.bodyAsText()
            val groupListType: Type = object : TypeToken<AuthTokenRest>() {}.type
            val authToken = RightRoutesKtTest.GSON.fromJson<AuthTokenRest?>(content, groupListType).token
            val updateResponse = client.put("/api/v1/users/${TEST_USER.username}") {
                header(HttpHeaders.Authorization, "Bearer $authToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_USER.copy(username = "foobar", password = "newPW")))
            }
            assertThat(
                "Return Status should be 400",
                updateResponse.status,
                `is`(HttpStatusCode.BadRequest)
            )
        }
    }

    @Test
    fun testUpdateUserPropertyAsAdminUser() {
        // 1. Login successfully
        val backend = spyk(
            LoriServerBackend(
                dbConnector = mockk(),
                config = CONFIG,
            )
        ) {
            every { checkCredentials(TEST_USER) } returns true
            every { updateUserNonRoleProperties(any()) } returns 1
            every { getCurrentUserRole(TEST_USER.username) } returns UserRole.ADMIN
        }
        val servicePool = getServicePool(backend)
        testApplication {
            application(
                servicePool.application()
            )
            val loginResponse = client.post("/api/v1/users/login") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_USER))
            }
            assertThat(
                "Should return OK",
                loginResponse.status,
                `is`(HttpStatusCode.OK)
            )
            val content: String = loginResponse.bodyAsText()
            val groupListType: Type = object : TypeToken<AuthTokenRest>() {}.type
            val authToken = RightRoutesKtTest.GSON.fromJson<AuthTokenRest?>(content, groupListType).token

            val userToChange = TEST_USER.copy(username = "randomUser", password = "555nase")

            val updateResponse = client.put("/api/v1/users/${userToChange.username}") {
                header(HttpHeaders.Authorization, "Bearer $authToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(userToChange))
            }
            assertThat(
                "Return Status should be 204",
                updateResponse.status,
                `is`(HttpStatusCode.NoContent)
            )
            verify(exactly = 1) { backend.getCurrentUserRole(TEST_USER.username) }
        }
    }

    @Test
    fun testDeleteOwnUser() {
        val backend = spyk(
            LoriServerBackend(
                dbConnector = mockk(),
                config = CONFIG,
            )
        ) {
            every { checkCredentials(TEST_USER) } returns true
            every { deleteUser(TEST_USER.username) } returns 1
        }
        val servicePool = getServicePool(backend)
        testApplication {
            application(
                servicePool.application()
            )
            val loginResponse = client.post("/api/v1/users/login") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_USER))
            }
            assertThat(
                "Should return OK",
                loginResponse.status,
                `is`(HttpStatusCode.OK)
            )
            val content: String = loginResponse.bodyAsText()
            val groupListType: Type = object : TypeToken<AuthTokenRest>() {}.type
            val authToken = RightRoutesKtTest.GSON.fromJson<AuthTokenRest?>(content, groupListType).token
            val updateResponse = client.delete("/api/v1/users/${TEST_USER.username}") {
                header(HttpHeaders.Authorization, "Bearer $authToken")
            }
            assertThat(
                "Return Status should be 200",
                updateResponse.status,
                `is`(HttpStatusCode.OK)
            )
        }
    }

    @Test
    fun testDeleteUserAsAdmin() {
        val backend = spyk(
            LoriServerBackend(
                dbConnector = mockk(),
                config = CONFIG,
            )
        ) {
            every { checkCredentials(TEST_USER) } returns true
            every { deleteUser(any()) } returns 1
            every { getCurrentUserRole(TEST_USER.username) } returns UserRole.ADMIN
        }
        val servicePool = getServicePool(backend)
        testApplication {
            application(
                servicePool.application()
            )
            val loginResponse = client.post("/api/v1/users/login") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_USER))
            }
            assertThat(
                "Should return OK",
                loginResponse.status,
                `is`(HttpStatusCode.OK)
            )
            val content: String = loginResponse.bodyAsText()
            val groupListType: Type = object : TypeToken<AuthTokenRest>() {}.type
            val authToken = RightRoutesKtTest.GSON.fromJson<AuthTokenRest?>(content, groupListType).token

            val userToChange = TEST_USER.copy(username = "randomUser", password = "555nase")

            val updateResponse = client.delete("/api/v1/users/${userToChange.username}") {
                header(HttpHeaders.Authorization, "Bearer $authToken")
            }
            assertThat(
                "Return Status should be 200",
                updateResponse.status,
                `is`(HttpStatusCode.OK)
            )
            verify(exactly = 1) { backend.getCurrentUserRole(TEST_USER.username) }
        }
    }

    @Test
    fun testUpdateUserRole() {
        val backend = spyk(
            LoriServerBackend(
                dbConnector = mockk(),
                config = CONFIG,
            )
        ) {
            every { checkCredentials(TEST_USER) } returns true
            every { getCurrentUserRole(TEST_USER.username) } returns UserRole.ADMIN
            every { updateUserRoleProperty(any(), any()) } returns 1
        }
        val servicePool = getServicePool(backend)
        testApplication {
            application(
                servicePool.application()
            )
            // Acquire auth token
            val loginResponse = client.post("/api/v1/users/login") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_USER))
            }
            assertThat(
                "Should return OK",
                loginResponse.status,
                `is`(HttpStatusCode.OK)
            )
            val content: String = loginResponse.bodyAsText()
            val groupListType: Type = object : TypeToken<AuthTokenRest>() {}.type
            val authToken = RightRoutesKtTest.GSON.fromJson<AuthTokenRest?>(content, groupListType).token

            // test update
            // given
            val roleToUpdate = RoleRest(username = "anyuser", role = RoleRest.Role.readWrite)
            val updateResponse = client.put("/api/v1/users/admin/role") {
                header(HttpHeaders.Authorization, "Bearer $authToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(roleToUpdate))
            }
            assertThat(
                "Return Status should be 200",
                updateResponse.status,
                `is`(HttpStatusCode.OK)
            )
            verify(exactly = 1) {
                backend.updateUserRoleProperty(
                    roleToUpdate.username,
                    roleToUpdate.role.toBusiness()
                )
            }
        }
    }

    @Test
    fun testUpdateUserRoleUnauthorizedExpiredToken() {
        val backend = spyk(
            LoriServerBackend(
                dbConnector = mockk(),
                config = CONFIG,
            )
        ) {
            every { checkCredentials(TEST_USER) } returns true
            every { generateJWT(any()) } returns JWT.create()
                .withAudience(CONFIG.jwtAudience)
                .withIssuer(CONFIG.jwtIssuer)
                .withClaim("username", TEST_USER.username)
                .withExpiresAt(Date(System.currentTimeMillis() - 60000))
                .sign(Algorithm.HMAC256(CONFIG.jwtSecret))
        }
        val servicePool = getServicePool(backend)
        testApplication {
            application(
                servicePool.application()
            )
            // Acquire auth token
            val loginResponse = client.post("/api/v1/users/login") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_USER))
            }
            assertThat(
                "Should return OK",
                loginResponse.status,
                `is`(HttpStatusCode.OK)
            )
            val content: String = loginResponse.bodyAsText()
            val groupListType: Type = object : TypeToken<AuthTokenRest>() {}.type
            val authToken = RightRoutesKtTest.GSON.fromJson<AuthTokenRest?>(content, groupListType).token

            // test update
            // given
            val roleToUpdate = RoleRest(username = "anyuser", role = RoleRest.Role.readWrite)
            val updateResponse = client.put("/api/v1/users/admin/role") {
                header(HttpHeaders.Authorization, "Bearer $authToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(roleToUpdate))
            }
            assertThat(
                "Return Status should be 401",
                updateResponse.status,
                `is`(HttpStatusCode.Unauthorized)
            )
            verify(exactly = 0) {
                backend.updateUserRoleProperty(
                    roleToUpdate.username,
                    roleToUpdate.role.toBusiness()
                )
            }
        }
    }

    @Test
    fun testUpdateUserRoleUnauthorizedNotAdmin() {
        val backend = spyk(
            LoriServerBackend(
                dbConnector = mockk(),
                config = CONFIG,
            )
        ) {
            every { checkCredentials(TEST_USER) } returns true
            every { getCurrentUserRole(TEST_USER.username) } returns UserRole.READONLY
        }
        val servicePool = getServicePool(backend)
        testApplication {
            application(
                servicePool.application()
            )
            // Acquire auth token
            val loginResponse = client.post("/api/v1/users/login") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_USER))
            }
            assertThat(
                "Should return OK",
                loginResponse.status,
                `is`(HttpStatusCode.OK)
            )
            val content: String = loginResponse.bodyAsText()
            val groupListType: Type = object : TypeToken<AuthTokenRest>() {}.type
            val authToken = RightRoutesKtTest.GSON.fromJson<AuthTokenRest?>(content, groupListType).token

            // test update
            // given
            val roleToUpdate = RoleRest(username = "anyuser", role = RoleRest.Role.readWrite)
            val updateResponse = client.put("/api/v1/users/admin/role") {
                header(HttpHeaders.Authorization, "Bearer $authToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(roleToUpdate))
            }
            assertThat(
                "Return Status should be 401",
                updateResponse.status,
                `is`(HttpStatusCode.Unauthorized)
            )
            verify(exactly = 0) {
                backend.updateUserRoleProperty(
                    roleToUpdate.username,
                    roleToUpdate.role.toBusiness()
                )
            }
        }
    }

    companion object {
        private val CONFIG = LoriConfiguration(
            grpcPort = 9092,
            httpPort = 8080,
            sqlUser = "postgres",
            sqlPassword = "postgres",
            sqlUrl = "jdbc:someurl",
            digitalArchiveAddress = "https://archiveaddress",
            digitalArchiveCommunity = listOf("5678"),
            digitalArchiveUsername = "testuser",
            digitalArchivePassword = "password",
            digitalArchiveBasicAuth = "basicauth",
            jwtAudience = "0.0.0.0:8080/ui",
            jwtIssuer = "0.0.0.0:8080",
            jwtRealm = "Lori ui",
            jwtSecret = "foobar",
        )

        private val tracer: Tracer =
            OpenTelemetry.noop().getTracer("de.zbw.api.lori.server.route.UsersRoutesKtTest")

        private val TEST_USER = UserRest(
            username = "Bob",
            password = "_secret_"
        )

        fun jsonAsString(any: Any): String = RightRoutesKtTest.GSON.toJson(any)
        fun getServicePool(backend: LoriServerBackend) = ServicePoolWithProbes(
            services = listOf(
                mockk {
                    every { isReady() } returns true
                    every { isHealthy() } returns true
                }
            ),
            config = CONFIG,
            backend = backend,
            tracer = tracer,
        )
    }
}
