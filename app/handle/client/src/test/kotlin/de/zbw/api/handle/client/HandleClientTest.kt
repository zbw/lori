package de.zbw.api.handle.client

import de.zbw.api.handle.client.config.HandleClientConfiguration
import de.zbw.handle.api.AddHandleRequest
import de.zbw.handle.api.AddHandleResponse
import de.zbw.handle.api.AddHandleValuesRequest
import de.zbw.handle.api.AddHandleValuesResponse
import de.zbw.handle.api.DeleteHandleRequest
import de.zbw.handle.api.DeleteHandleResponse
import de.zbw.handle.api.HandleServiceGrpcKt
import io.mockk.coEvery
import io.mockk.coVerify
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
    fun testClientAddHandle() {
        runBlocking {
            // given
            val expected = AddHandleResponse.getDefaultInstance()
            val stub = mockk<HandleServiceGrpcKt.HandleServiceCoroutineStub> {
                coEvery { addHandle(any()) } returns expected
                every { withDeadlineAfter(any(), any()) } returns this
            }
            val client = HandleClient(
                configuration = HandleClientConfiguration(port = 10000, address = "foo", deadlineInMilli = 2000L),
                channel = mockk(),
                stub = stub
            )
            // when
            val received = client.addHandle(AddHandleRequest.getDefaultInstance())
            // then
            assertThat(received, `is`(expected))
            coVerify(exactly = 1) { stub.addHandle(AddHandleRequest.getDefaultInstance()) }
        }
    }

    @Test
    fun testClientAddHandleValues() {
        runBlocking {

            val expected = AddHandleValuesResponse.getDefaultInstance()
            val client = HandleClient(
                configuration = HandleClientConfiguration(port = 10000, address = "foo", deadlineInMilli = 2000L),
                channel = mockk(),
                stub = mockk() {
                    coEvery { addHandleValues(any()) } returns expected
                    every { withDeadlineAfter(any(), any()) } returns this
                }
            )
            val received = client.addHandleValues(AddHandleValuesRequest.getDefaultInstance())
            assertThat(received, `is`(expected))
        }
    }

    @Test
    fun testClientDeleteHandle() {
        runBlocking {

            val expected = DeleteHandleResponse.getDefaultInstance()
            val client = HandleClient(
                configuration = HandleClientConfiguration(port = 10000, address = "foo", deadlineInMilli = 2000L),
                channel = mockk(),
                stub = mockk() {
                    coEvery { deleteHandle(any()) } returns expected
                    every { withDeadlineAfter(any(), any()) } returns this
                }
            )
            val received = client.deleteHandle(DeleteHandleRequest.getDefaultInstance())
            assertThat(received, `is`(expected))
        }
    }
}
