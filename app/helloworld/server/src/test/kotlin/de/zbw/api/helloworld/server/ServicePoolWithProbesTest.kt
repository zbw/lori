package de.zbw.api.helloworld.server

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.testng.Assert.assertEquals
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

/**
 * Testing [ServicePoolWithProbes].
 *
 * Created on 04-26-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class ServicePoolWithProbesTest {

    @DataProvider(name = DATA_FOR_TEST_APPLICATION)
    private fun createDataForTestApplication() =
        arrayOf(
            arrayOf(
                true,
                true,
            ),
            arrayOf(
                true,
                false,
            ),
            arrayOf(
                false,
                true,
            ),
            arrayOf(
                false,
                false,
            ),
        )

    @Test(dataProvider = DATA_FOR_TEST_APPLICATION)
    fun testApplicationProbes(
        ready: Boolean,
        healthy: Boolean,
    ) {
        val servicePool = ServicePoolWithProbes(
            services = listOf(
                // One service is always healthy and ready
                mockk {
                    every { isReady() } returns true
                    every { isHealthy() } returns true
                },
                mockk {
                    every { isReady() } returns ready
                    every { isHealthy() } returns healthy
                }
            ),
            port = 8082,
        )
        withTestApplication(servicePool.application()) {
            with(handleRequest(HttpMethod.Get, "/ready")) {
                if (ready) {
                    assertEquals(HttpStatusCode.OK, response.status())
                } else {
                    assertEquals(HttpStatusCode.InternalServerError, response.status())
                }
            }
            with(handleRequest(HttpMethod.Get, "/healthz")) {
                if (healthy) {
                    assertEquals(HttpStatusCode.OK, response.status())
                } else {
                    assertEquals(HttpStatusCode.InternalServerError, response.status())
                }
            }
        }
    }

    @Test
    fun testApplicationStartAndStop() {
        // given
        val services: List<ServiceLifecycle> = listOf(
            mockk {
                every { start() } returns Unit
                every { stop() } returns Unit
            },
            mockk {
                every { start() } returns Unit
                every { stop() } returns Unit
            }
        )
        val serverMock: NettyApplicationEngine = mockk(relaxed = true)
        val servicePool = spyk(
            ServicePoolWithProbes(
                services = services,
                port = 8082,
            )
        ) {
            every { getHttpServer() } returns serverMock
        }

        // when
        servicePool.start()
        servicePool.stop()

        // then
        services.forEach {
            verify(exactly = 1) { it.start() }
            verify(exactly = 1) { it.stop() }
        }
        verify(exactly = 1) { serverMock.start(true) }
        verify(exactly = 1) { serverMock.stop(any(), any()) }
    }

    companion object {
        const val DATA_FOR_TEST_APPLICATION = "DATA_FOR_TEST_APPLICATION"
    }
}
