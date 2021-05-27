package de.zbw.api.handle.server

import io.grpc.BindableService
import io.grpc.Server
import io.mockk.mockk
import org.testng.Assert.assertFalse
import org.testng.annotations.Test

/**
 * Testing [GrpcServer].
 *
 * Created on 05-14-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class GrpcServerTest() {

    @Test
    fun testReadyness() {
        // given
        val grpcServer = de.zbw.api.handle.server.GrpcServer(
            port = 9000,
            services = listOf<BindableService>(),
            server = mockk<Server>(relaxed = true),
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
