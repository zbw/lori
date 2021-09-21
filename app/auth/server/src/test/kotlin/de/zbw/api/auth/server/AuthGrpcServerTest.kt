package de.zbw.api.auth.server

import de.zbw.auth.api.SayHelloRequest
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test

/**
 * Test [AuthGrpcServer].
 *
 * Created on 09-21-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class AuthGrpcServerTest {

    @Test
    fun testSayHello() {
        runBlocking {
            val request = SayHelloRequest
                .newBuilder()
                .setName("foo")
                .build()

            val response = AuthGrpcServer().sayHello(request)
            assertThat(response.message, `is`("Hello ${request.name}"))
        }
    }
}
