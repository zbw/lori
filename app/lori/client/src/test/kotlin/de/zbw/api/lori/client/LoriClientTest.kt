package de.zbw.api.lori.client

import de.zbw.api.lori.client.config.LoriClientConfiguration
import de.zbw.lori.api.ApplyTemplatesRequest
import de.zbw.lori.api.ApplyTemplatesResponse
import de.zbw.lori.api.CheckForRightErrorsRequest
import de.zbw.lori.api.CheckForRightErrorsResponse
import de.zbw.lori.api.FullImportRequest
import de.zbw.lori.api.FullImportResponse
import io.grpc.Channel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.opentelemetry.api.OpenTelemetry
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
class LoriClientTest {
    @Test
    fun testFullImport() {
        runBlocking {
            val expected = FullImportResponse.getDefaultInstance()
            val client =
                LoriClient(
                    configuration = LoriClientConfiguration(port = 10000, address = "foo", deadlineInMilli = 2000L),
                    channel = mockk<Channel>(),
                    stub =
                        mockk {
                            coEvery { fullImport(any(), any()) } returns expected
                            every { withDeadlineAfter(any(), any()) } returns this
                        },
                    openTelemetry = OpenTelemetry.noop(),
                )
            val received = client.fullImport((FullImportRequest.getDefaultInstance()))
            assertThat(received, `is`(expected))
        }
    }

    @Test
    fun testCheckForErrors() {
        runBlocking {
            val expected = CheckForRightErrorsResponse.getDefaultInstance()
            val client =
                LoriClient(
                    configuration = LoriClientConfiguration(port = 10000, address = "foo", deadlineInMilli = 2000L),
                    channel = mockk<Channel>(),
                    stub =
                        mockk {
                            coEvery { checkForRightErrors(any(), any()) } returns expected
                            every { withDeadlineAfter(any(), any()) } returns this
                        },
                    openTelemetry = OpenTelemetry.noop(),
                )
            val received = client.checkForErrors((CheckForRightErrorsRequest.getDefaultInstance()))
            assertThat(received, `is`(expected))
        }
    }

    @Test
    fun testApplyTemplates() {
        runBlocking {
            val expected = ApplyTemplatesResponse.getDefaultInstance()
            val client =
                LoriClient(
                    configuration = LoriClientConfiguration(port = 10000, address = "foo", deadlineInMilli = 2000L),
                    channel = mockk<Channel>(),
                    stub =
                        mockk {
                            coEvery { applyTemplates(any(), any()) } returns expected
                            every { withDeadlineAfter(any(), any()) } returns this
                        },
                    openTelemetry = OpenTelemetry.noop(),
                )
            val received = client.applyTemplates((ApplyTemplatesRequest.getDefaultInstance()))
            assertThat(received, `is`(expected))
        }
    }
}
