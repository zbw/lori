package de.zbw.api.auth.server.route

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.zbw.api.auth.server.ServicePoolWithProbes
import de.zbw.api.auth.server.config.AuthConfiguration
import de.zbw.auth.model.SignInAnswer
import de.zbw.auth.model.SignInUserData
import de.zbw.auth.model.SignUpUserData
import de.zbw.auth.model.UserRole
import de.zbw.business.auth.server.AuthBackend
import de.zbw.persistence.auth.server.transient.UserTableEntry
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.lang.reflect.Type
import java.sql.SQLException

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
                assertThat(
                    "Should return OK",
                    response.status(),
                    `is`(HttpStatusCode.OK)
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
                assertThat(
                    "400 expected because user name is not available",
                    response.status(),
                    `is`(HttpStatusCode.BadRequest)
                )
                verify(exactly = 0) { backend.registerNewUser(SIGNUP_DATA) }
            }
        }
    }

    @Test
    fun testPOSTSignInSuccess() {
        mockkObject(AuthBackend) {
            // given
            val userEntry = UserTableEntry(
                id = 1,
                name = "user",
                email = "test@domain.com",
                hash = "1234567",
            )
            every { AuthBackend.verifyPassword(SIGNIN_DATA.password, userEntry.hash) } returns true

            val expectedRoles = listOf(
                UserRole.Role.userRead,
            )
            val backend = mockk<AuthBackend>(relaxed = true) {
                every { getUserEntry(any()) } returns userEntry
                every { getUserRolesById(userEntry.id) } returns expectedRoles
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
                    handleRequest(HttpMethod.Post, "/api/v1/auth/signin") {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(jsonAsString(SIGNIN_DATA))
                    }
                ) {
                    // then
                    assertThat(
                        "Should return OK",
                        response.status(),
                        `is`(HttpStatusCode.OK)
                    )
                    val signInAnswerType: Type = object : TypeToken<SignInAnswer>() {}.type
                    val receivedJson: SignInAnswer = Gson().fromJson(response.content!!, signInAnswerType)
                    assertThat(
                        "User name not matching",
                        receivedJson.username,
                        `is`(userEntry.name)
                    )
                    assertThat(
                        "User name not matching",
                        receivedJson.email,
                        `is`(userEntry.email)
                    )
                    assertThat(
                        "User name not matching",
                        receivedJson.roles,
                        `is`(expectedRoles.map { UserRole(it) })
                    )
                }
            }
        }
    }

    @DataProvider(name = DATA_FOR_SIGN_IN_UNAUTHORIZED)
    fun createDataForSignInUnauthorized() = arrayOf(
        arrayOf(
            true,
            false,
        ),
        arrayOf(
            false,
            true,
        ),
    )

    @Test(dataProvider = DATA_FOR_SIGN_IN_UNAUTHORIZED)
    fun testPOSTSignInUnauthorized(
        validUser: Boolean,
        validPassword: Boolean,
    ) {
        mockkObject(AuthBackend) {
            // given
            val userEntry = UserTableEntry(
                id = 1,
                name = "user",
                email = "test@domain.com",
                hash = "1234567",
            )
            every { AuthBackend.verifyPassword(SIGNIN_DATA.password, userEntry.hash) } returns validPassword

            val expectedRoles = listOf(
                UserRole.Role.userRead,
            )
            val backend = mockk<AuthBackend>(relaxed = true) {
                every { getUserEntry(any()) } returns if (validUser) {
                    userEntry
                } else {
                    null
                }
                every { getUserRolesById(userEntry.id) } returns expectedRoles
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
                    handleRequest(HttpMethod.Post, "/api/v1/auth/signin") {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(jsonAsString(SIGNIN_DATA))
                    }
                ) {
                    // then
                    assertThat(
                        "Received status code does not match",
                        response.status(),
                        `is`(HttpStatusCode.Unauthorized)
                    )
                }
            }
        }
    }

    @Test(expectedExceptions = [SQLException::class])
    fun testPOSTSignInInternalError() {
        mockkObject(AuthBackend) {
            // given
            val userEntry = UserTableEntry(
                id = 1,
                name = "user",
                email = "test@domain.com",
                hash = "1234567",
            )
            every { AuthBackend.verifyPassword(SIGNIN_DATA.password, userEntry.hash) } returns true

            val expectedRoles = listOf(
                UserRole.Role.userRead,
            )
            val backend = mockk<AuthBackend>(relaxed = true) {
                every { getUserEntry(any()) } throws SQLException()
                every { getUserRolesById(userEntry.id) } returns expectedRoles
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
                handleRequest(HttpMethod.Post, "/api/v1/auth/signin") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(jsonAsString(SIGNIN_DATA))
                }
                // exception
            }
        }
    }

    companion object {
        const val DATA_FOR_SIGN_IN_UNAUTHORIZED = "DATA_FOR_SIGN_IN_UNAUTHORIZED"
        val CONFIG = AuthConfiguration(
            grpcPort = 9092,
            httpPort = 8080,
            jwtAudience = "someAudience",
            jwtIssuer = "someIssuer",
            jwtRealm = "someRealm",
            jwtSecret = "somePassword",
            sqlUser = "postgres",
            sqlPassword = "postgres",
            sqlUrl = "jdbc:someurl",
        )

        val SIGNUP_DATA = SignUpUserData(
            name = "user",
            password = "foobar",
            email = "bla@domain.com",
        )

        val SIGNIN_DATA = SignInUserData(
            name = "user",
            password = "foobar",
        )

        fun jsonAsString(any: Any): String = Gson().toJson(any)
    }
}
