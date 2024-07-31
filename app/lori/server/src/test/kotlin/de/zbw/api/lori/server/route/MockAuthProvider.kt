package de.zbw.api.lori.server.route

import de.zbw.api.lori.server.type.UserSession
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.AuthenticationContext
import io.ktor.server.auth.AuthenticationProvider
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.every
import io.mockk.mockk

val jwtPrincipal =
    mockk<UserSession> {
        every { email } returns "foo@bar.com"
    }

class MockAuthProvider(
    config: Config,
) : AuthenticationProvider(config) {
    override suspend fun onAuthenticate(context: AuthenticationContext) {
        context.principal(jwtPrincipal)
    }
}

class DummyConfig(
    name: String,
) : AuthenticationProvider.Config(name)

fun ApplicationTestBuilder.moduleAuthForTests() {
    install(Authentication) {
        register(MockAuthProvider(DummyConfig("auth-login")))
    }
}
