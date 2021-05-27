package de.zbw.api.handle.client

import de.zbw.api.handle.client.config.HandleClientConfiguration
import de.zbw.handle.api.AddHandleRequest
import de.zbw.handle.api.AddHandleResponse
import io.grpc.Channel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test

/**
 * Testing the [HandleClient].
 *
 * Created on 05-14-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class HandleClientTest {
    @Test
    fun testClientSayHello() {
        runBlocking {

            val expected = AddHandleResponse.getDefaultInstance()
            val client = HandleClient(
                configuration = HandleClientConfiguration(port = 10000, address = "foo", deadlineInMilli = 2000L),
                channel = mockk<Channel>(),
                stub = mockk() {
                    coEvery { addHandle(any()) } returns expected
                    every { withDeadlineAfter(any(), any()) } returns this
                }
            )
            val received = client.addHandle(AddHandleRequest.getDefaultInstance())
            assertThat(received, `is`(expected))
        }
    }
}
