package de.zbw.api.helloworld.server

import io.grpc.BindableService
import io.grpc.Server
import io.mockk.mockk
import org.testng.Assert.assertFalse
import org.testng.annotations.Test

/**
 * Testing [GrpcServer].
 *
 * Created on 04-26-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class GrpcServerTest() {

    @Test
    fun testReadyness() {
        // given
        val grpcServer = GrpcServer(
            services = listOf<BindableService>(),
            server = mockk<Server>(relaxed = true)
        )

        // when
        grpcServer.start()

        // then
        assert(grpcServer.isReady())
        assert(grpcServer.isHealthy())

        // when
        grpcServer.stop()
        assertFalse(grpcServer.isReady())
        assertFalse(grpcServer.isHealthy())
    }
}
