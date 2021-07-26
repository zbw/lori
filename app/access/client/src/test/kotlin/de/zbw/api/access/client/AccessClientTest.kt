package de.zbw.api.access.client

import de.zbw.access.api.AddAccessInformationRequest
import de.zbw.access.api.AddAccessInformationResponse
import de.zbw.access.api.GetAccessInformationRequest
import de.zbw.access.api.GetAccessInformationResponse
import de.zbw.api.access.client.config.AccessClientConfiguration
import io.grpc.Channel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test

/**
 * Testing [AccessClient].
 *
 * Created on 07-12-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class AccessClientTest() {
    @Test
    fun testAddAccessInformation() {
        runBlocking {
            val expected = AddAccessInformationResponse.getDefaultInstance()
            val client = AccessClient(
                configuration = AccessClientConfiguration(port = 10000, address = "foo", deadlineInMilli = 2000L),
                channel = mockk<Channel>(),
                stub = mockk() {
                    coEvery { addAccessInformation(any()) } returns expected
                    every { withDeadlineAfter(any(), any()) } returns this
                }
            )
            val received = client.addAccessInformation((AddAccessInformationRequest.getDefaultInstance()))
            assertThat(received, `is`(expected))
        }
    }

    @Test
    fun testGetAccessInformation() {
        runBlocking {
            val expected = GetAccessInformationResponse.getDefaultInstance()
            val client = AccessClient(
                configuration = AccessClientConfiguration(port = 10000, address = "foo", deadlineInMilli = 2000L),
                channel = mockk<Channel>(),
                stub = mockk() {
                    coEvery { getAccessInformation(any()) } returns expected
                    every { withDeadlineAfter(any(), any()) } returns this
                }
            )
            val received = client.getAccessInformation((GetAccessInformationRequest.getDefaultInstance()))
            assertThat(received, `is`(expected))
        }
    }
}
