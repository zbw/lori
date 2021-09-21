package de.zbw.api.auth.client

import de.zbw.api.auth.client.config.AuthClientConfiguration
import de.zbw.auth.api.SayHelloRequest
import de.zbw.auth.api.SayHelloResponse
import io.grpc.Channel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test

/**
 * Testing [AuthClient].
 *
 * Created on 09-21-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class AuthClientTest() {
    @Test
    fun testClientSayHello() {
        runBlocking {

            val expected = SayHelloResponse.getDefaultInstance()
            val client = AuthClient(
                configuration = AuthClientConfiguration(port = 10000, address = "foo", deadlineInMilli = 2000L),
                channel = mockk<Channel>(),
                stub = mockk() {
                    coEvery { sayHello(any()) } returns expected
                    every { withDeadlineAfter(any(), any()) } returns this
                }
            )
            val received = client.sayHello(SayHelloRequest.getDefaultInstance())
            assertThat(received, `is`(expected))
        }
    }
}
