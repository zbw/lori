package de.zbw.api.lori.server

import de.zbw.api.lori.server.connector.DAConnector
import de.zbw.api.lori.server.type.DACommunity
import de.zbw.lori.api.FullImportRequest
import de.zbw.lori.api.FullImportResponse
import io.grpc.StatusRuntimeException
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test
import java.nio.channels.UnresolvedAddressException

/**
 * Test [LoriGrpcServer].
 *
 * Created on 07-12-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class LoriGrpcServerTest {

    @Test
    fun testFullImport() {
        runBlocking {
            // given
            val token = "SOME_TOKEN"
            val importsPerCommunity = 3
            val communityIds = listOf("4", "6")

            val community = DACommunity(
                id = 5,
                name = "Some name",
                handle = null,
                type = null,
                link = "some link",
                expand = emptyList(),
                logo = null,
                parentCommunity = null,
                copyrightText = null,
                introductoryText = null,
                shortDescription = null,
                sidebarText = null,
                subcommunities = emptyList(),
                collections = listOf(
                    mockk {
                        every { id } returns 101
                    }
                ),
                countItems = 1,
            )

            val importer = mockk<DAConnector> {
                coEvery { login() } returns token
                coEvery { getCommunity(token, any()) } returns community
                coEvery { startFullImport(token, any()) } returns listOf(importsPerCommunity)
            }

            val expected = FullImportResponse.newBuilder()
                .setItemsImported(communityIds.size * importsPerCommunity)
                .build()

            val request = FullImportRequest.getDefaultInstance()
            // when
            val response = LoriGrpcServer(
                mockk() {
                    every { digitalArchiveCommunity } returns communityIds
                },
                mockk(),
                importer,
                tracer,
            ).fullImport(request)

            // then
            assertThat(response, `is`(expected))
        }
    }

    @Test(expectedExceptions = [StatusRuntimeException::class])
    fun testFullImportLoginError() {
        runBlocking {
            // given
            val importer = mockk<DAConnector> {
                coEvery { login() } throws UnresolvedAddressException()
            }

            val request = FullImportRequest.getDefaultInstance()
            // when
            LoriGrpcServer(
                mockk(),
                mockk(),
                importer,
                tracer,
            ).fullImport(request)
        }
    }

    companion object {
        private val tracer: Tracer = OpenTelemetry.noop().getTracer("de.zbw.api.lori.server.LoriGrpcServerTest")
    }
}
