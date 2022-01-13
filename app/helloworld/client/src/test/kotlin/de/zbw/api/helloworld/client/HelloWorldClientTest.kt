package de.zbw.api.helloworld.client

import de.zbw.api.helloworld.client.config.HelloWorldClientConfiguration
import de.zbw.helloworld.api.SayHelloRequest
import de.zbw.helloworld.api.SayHelloResponse
import io.grpc.Channel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test

/**
 * Testing [HelloWorldClient].
 *
 * Created on 04-28-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class HelloWorldClientTest() {
    @Test
    fun testClientSayHello() {
        runBlocking {

            val expected = SayHelloResponse.getDefaultInstance()
            val client = HelloWorldClient(
                configuration = HelloWorldClientConfiguration(port = 10000, address = "foo", deadlineInMilli = 2000L),
                channel = mockk<Channel>(),
                stub = mockk() {
                    coEvery { sayHello(any(), any()) } returns expected
                    every { withDeadlineAfter(any(), any()) } returns this
                }
            )
            val received = client.sayHello(SayHelloRequest.getDefaultInstance())
            assertThat(received, `is`(expected))
        }
    }
}
