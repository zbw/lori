package de.zbw.api.lori.server.route

import io.ktor.server.auth.Authentication
import io.ktor.server.auth.AuthenticationContext
import io.ktor.server.auth.AuthenticationProvider
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.mockk

val jwtPrincipal = mockk<JWTPrincipal>()
class MockAuthProvider(config: Config) : AuthenticationProvider(config) {
    override suspend fun onAuthenticate(context: AuthenticationContext) {
        context.principal(jwtPrincipal)
    }
}

class DummyConfig(name: String) : AuthenticationProvider.Config(name)
fun ApplicationTestBuilder.moduleAuthForTests() {
    install(Authentication) {
        register(MockAuthProvider(DummyConfig("auth-login")))
    }
}
