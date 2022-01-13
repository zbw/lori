package de.zbw.api.access.client

import de.zbw.access.api.AddItemRequest
import de.zbw.access.api.AddItemResponse
import de.zbw.access.api.GetItemRequest
import de.zbw.access.api.GetItemResponse
import de.zbw.api.access.client.config.LoriClientConfiguration
import io.grpc.Channel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test

/**
 * Testing [LoriClient].
 *
 * Created on 07-12-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class LoriClientTest() {
    @Test
    fun testAddAccessInformation() {
        runBlocking {
            val expected = AddItemResponse.getDefaultInstance()
            val client = LoriClient(
                configuration = LoriClientConfiguration(port = 10000, address = "foo", deadlineInMilli = 2000L),
                channel = mockk<Channel>(),
                stub = mockk() {
                    coEvery { addItem(any(), any()) } returns expected
                    every { withDeadlineAfter(any(), any()) } returns this
                }
            )
            val received = client.addItem((AddItemRequest.getDefaultInstance()))
            assertThat(received, `is`(expected))
        }
    }

    @Test
    fun testGetAccessInformation() {
        runBlocking {
            val expected = GetItemResponse.getDefaultInstance()
            val client = LoriClient(
                configuration = LoriClientConfiguration(port = 10000, address = "foo", deadlineInMilli = 2000L),
                channel = mockk<Channel>(),
                stub = mockk() {
                    coEvery { getItem(any(), any()) } returns expected
                    every { withDeadlineAfter(any(), any()) } returns this
                }
            )
            val received = client.getItem((GetItemRequest.getDefaultInstance()))
            assertThat(received, `is`(expected))
        }
    }
}
