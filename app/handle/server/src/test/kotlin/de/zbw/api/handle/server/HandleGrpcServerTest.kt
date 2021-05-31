package de.zbw.api.handle.server

import de.zbw.api.handle.server.config.HandleConfiguration
import de.zbw.business.handle.server.HandleCommunicator
import de.zbw.handle.api.AddHandleRequest
import de.zbw.handle.api.AddHandleValuesRequest
import de.zbw.handle.api.AddHandleValuesResponse
import de.zbw.handle.api.DeleteHandleRequest
import de.zbw.handle.api.DeleteHandleResponse
import io.grpc.StatusRuntimeException
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import net.handle.hdllib.AbstractMessage
import net.handle.hdllib.CreateHandleResponse
import net.handle.hdllib.ErrorResponse
import net.handle.hdllib.GenericResponse
import net.handle.hdllib.HandleException
import net.handle.hdllib.Util
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test

/**
 * Test [HandleGrpcServer].
 *
 * Created on 05-14-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class HandleGrpcServerTest {

    @Test
    fun testAddHandle() {
        runBlocking {
            // given
            val expectedHandle = "1234/5678"
            val communicator = mockk<HandleCommunicator>() {
                every {
                    addHandle(any())
                } returns CreateHandleResponse(Util.encodeString(expectedHandle))
            }
            val handleServer = HandleGrpcServer(
                EXAMPLE_CONFIG,
                communicator,
            )

            // when
            val response = handleServer.addHandle(AddHandleRequest.getDefaultInstance())

            // then
            assertThat(response.createdHandle, `is`(expectedHandle))
        }
    }

    @Test
    fun testAddHandleValue() {
        runBlocking {
            // given
            val communicator = mockk<HandleCommunicator>() {
                every {
                    addHandleValues(any())
                } returns GenericResponse(101, AbstractMessage.RC_SUCCESS)
            }
            val handleServer = HandleGrpcServer(
                EXAMPLE_CONFIG,
                communicator,
            )

            // when
            val response = handleServer.addHandleValues(AddHandleValuesRequest.getDefaultInstance())

            // then
            assertThat(response, `is`(AddHandleValuesResponse.getDefaultInstance()))
        }
    }

    @Test
    fun testDeleteHandle() {
        runBlocking {
            // given
            val communicator = mockk<HandleCommunicator>() {
                every {
                    deleteHandle(any())
                } returns GenericResponse(101, AbstractMessage.RC_SUCCESS)
            }

            val handleServer = HandleGrpcServer(
                EXAMPLE_CONFIG,
                communicator,
            )

            // when
            val response = handleServer.deleteHandle(DeleteHandleRequest.getDefaultInstance())

            // then
            assertThat(response, `is`(DeleteHandleResponse.getDefaultInstance()))
        }
    }

    @Test(expectedExceptions = [StatusRuntimeException::class])
    fun testHandleErrorResponse() {
        runBlocking {
            // given
            val communicator = mockk<HandleCommunicator>() {
                every {
                    addHandle(any())
                } returns ErrorResponse(1, 101, Util.encodeString("Error"))
            }
            val handleServer = HandleGrpcServer(
                EXAMPLE_CONFIG,
                communicator,
            )
            // when
            handleServer.addHandle(AddHandleRequest.getDefaultInstance())
            // then boom
        }
    }

    @Test(expectedExceptions = [StatusRuntimeException::class])
    fun testHandleInternalError() {
        runBlocking {
            // given
            val communicator = mockk<HandleCommunicator>() {
                every {
                    addHandle(any())
                } throws HandleException(1)
            }
            val handleServer = HandleGrpcServer(
                EXAMPLE_CONFIG,
                communicator,
            )
            // when
            handleServer.addHandle(AddHandleRequest.getDefaultInstance())
            // then boom
        }
    }

    companion object {
        val EXAMPLE_CONFIG: HandleConfiguration = HandleConfiguration(9092, 8082, "password", "5678")
    }
}
