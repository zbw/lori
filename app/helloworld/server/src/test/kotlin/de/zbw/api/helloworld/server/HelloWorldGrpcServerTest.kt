package de.zbw.api.helloworld.server

import de.zbw.helloworld.api.SayHelloRequest
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test

/**
 * Test [HelloWorldGrpcServer].
 *
 * Created on 04-22-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class HelloWorldGrpcServerTest {

    @Test
    fun testSayHello() {
        runBlocking {
            val request = SayHelloRequest
                .newBuilder()
                .setName("foo")
                .build()

            val response = HelloWorldGrpcServer().sayHello(request)
            assertThat(response.message, `is`("Hello ${request.name}"))
        }
    }
}
