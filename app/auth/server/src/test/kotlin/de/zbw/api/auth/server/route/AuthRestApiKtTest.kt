package de.zbw.api.auth.server.route

import com.google.gson.Gson
import de.zbw.api.auth.server.ServicePoolWithProbes
import de.zbw.api.auth.server.config.AuthConfiguration
import de.zbw.auth.model.SignUpUserData
import de.zbw.business.auth.server.AuthBackend
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.testng.annotations.Test

/**
 * Testing the REST-API.
 *
 * Created on 09-23-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class AuthRestApiKtTest {

    @Test
    fun testPOSTSignUpSuccess() {
        // given
        val backend = mockk<AuthBackend>(relaxed = true) {
            every { isUsernameAvailable(any()) } returns true
        }
        val servicePool = ServicePoolWithProbes(
            services = listOf(
                mockk {
                    every { isReady() } returns true
                    every { isHealthy() } returns true
                }
            ),
            config = CONFIG,
            backend = backend
        )

        // when
        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Post, "/api/v1/auth/signup") {
                    addHeader(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(jsonAsString(SIGNUP_DATA))
                }
            ) {
                // then
                MatcherAssert.assertThat(
                    "Should return OK",
                    response.status(),
                    CoreMatchers.`is`(HttpStatusCode.OK)
                )
                verify(exactly = 1) { backend.registerNewUser(SIGNUP_DATA) }
            }
        }
    }

    @Test
    fun testPOSTSignUp400() {
        // given
        val backend = mockk<AuthBackend>(relaxed = true) {
            every { isUsernameAvailable(any()) } returns false
        }
        val servicePool = ServicePoolWithProbes(
            services = listOf(
                mockk {
                    every { isReady() } returns true
                    every { isHealthy() } returns true
                }
            ),
            config = CONFIG,
            backend = backend
        )

        // when
        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Post, "/api/v1/auth/signup") {
                    addHeader(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(jsonAsString(SIGNUP_DATA))
                }
            ) {
                // then
                MatcherAssert.assertThat(
                    "400 expected because user name is not available",
                    response.status(),
                    CoreMatchers.`is`(HttpStatusCode.BadRequest)
                )
                verify(exactly = 0) { backend.registerNewUser(SIGNUP_DATA) }
            }
        }
    }

    companion object {
        val CONFIG = AuthConfiguration(
            grpcPort = 9092,
            httpPort = 8080,
            sqlUser = "postgres",
            sqlPassword = "postgres",
            sqlUrl = "jdbc:someurl",
        )

        val SIGNUP_DATA = SignUpUserData(
            name = "user",
            password = "foobar",
            email = "bla@domain.com",
        )

        fun jsonAsString(any: Any): String = Gson().toJson(any)
    }
}
