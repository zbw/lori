package de.zbw.api.lori.server

import de.zbw.api.lori.server.config.LoriConfiguration
import de.zbw.business.lori.server.LoriServerBackend
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import org.testng.Assert.assertEquals
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

/**
 * Testing [ServicePoolWithProbes].
 *
 * Created on 07-12-2021.
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

        val servicePool = getServicePool(mockk(), ready, healthy)
        testApplication {
            application(
                servicePool.application()
            )
            val readyResonse = client.get("/ready")
            if (ready) {
                assertEquals(HttpStatusCode.OK, readyResonse.status)
            } else {
                assertEquals(HttpStatusCode.InternalServerError, readyResonse.status)
            }
            val healthzResponse = client.get("/healthz")
            if (healthy) {
                assertEquals(HttpStatusCode.OK, healthzResponse.status)
            } else {
                assertEquals(HttpStatusCode.InternalServerError, healthzResponse.status)
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
                config = TEST_CONFIG,
                backend = mockk(),
                tracer = mockk(),
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

        val TEST_CONFIG = LoriConfiguration(
            grpcPort = 9092,
            httpPort = 8080,
            sqlUrl = "jdbc:localhost",
            sqlUser = "postgres",
            sqlPassword = "postgres",
            digitalArchiveAddress = "https://archiveaddress",
            digitalArchiveCommunity = listOf("5678"),
            digitalArchiveUsername = "testuser",
            digitalArchivePassword = "password",
            digitalArchiveBasicAuth = "basicauth",
            jwtAudience = "0.0.0.0:8080/ui",
            jwtIssuer = "0.0.0.0:8080",
            jwtRealm = "Lori ui",
            jwtSecret = "foobar",
        )

        private val tracer: Tracer = OpenTelemetry.noop().getTracer("de.zbw.api.lori.server.ServiceWithProbesTest")
        fun getServicePool(backend: LoriServerBackend, isReady: Boolean, isHealthy: Boolean) = ServicePoolWithProbes(
            services = listOf(
                mockk {
                    every { isReady() } returns isReady
                    every { isHealthy() } returns isHealthy
                }
            ),
            config = TEST_CONFIG,
            backend = backend,
            tracer = tracer,
        )
    }
}
