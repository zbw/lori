package de.zbw.api.handle.server

import de.zbw.business.handle.server.HandleCommunicator
import de.zbw.handle.api.AddHandleRequest
import de.zbw.handle.api.AddHandleValuesRequest
import de.zbw.handle.api.AddHandleValuesResponse
import de.zbw.handle.api.DeleteHandleRequest
import de.zbw.handle.api.DeleteHandleResponse
import de.zbw.handle.api.ListHandleValuesRequest
import de.zbw.handle.api.ModifyHandleValuesRequest
import de.zbw.handle.api.ModifyHandleValuesResponse
import io.grpc.StatusRuntimeException
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import net.handle.hdllib.AbstractMessage
import net.handle.hdllib.AuthenticationInfo
import net.handle.hdllib.CreateHandleResponse
import net.handle.hdllib.ErrorResponse
import net.handle.hdllib.GenericResponse
import net.handle.hdllib.HandleException
import net.handle.hdllib.ListHandlesRequest
import net.handle.hdllib.ListHandlesResponse
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
            val communicator = mockk<HandleCommunicator> {
                every {
                    addHandle(any())
                } returns CreateHandleResponse(Util.encodeString(expectedHandle))
            }
            val handleServer = HandleGrpcServer(
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
            val handleClient = mockk<HandleCommunicator> {
                every {
                    addHandleValues(any())
                } returns GenericResponse(101, AbstractMessage.RC_SUCCESS)
            }
            val handleServer = HandleGrpcServer(
                handleClient,
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
            val handleClient = mockk<HandleCommunicator> {
                every {
                    deleteHandle(any())
                } returns GenericResponse(101, AbstractMessage.RC_SUCCESS)
            }

            val handleServer = HandleGrpcServer(
                handleClient,
            )

            // when
            val response = handleServer.deleteHandle(DeleteHandleRequest.getDefaultInstance())

            // then
            assertThat(response, `is`(DeleteHandleResponse.getDefaultInstance()))
        }
    }

    @Test
    fun testModifyHandle() {
        runBlocking {
            // given
            val handleClient = mockk<HandleCommunicator> {
                every {
                    modifyHandleValues(any())
                } returns GenericResponse(101, AbstractMessage.RC_SUCCESS)
            }

            val handleServer = HandleGrpcServer(
                handleClient,
            )

            // when
            val response = handleServer.modifyHandleValues(ModifyHandleValuesRequest.getDefaultInstance())

            // then
            assertThat(response, `is`(ModifyHandleValuesResponse.getDefaultInstance()))
        }
    }

    @Test
    fun testListHandle() {
        runBlocking {
            // given
            val expectedHandles = listOf(
                "5678/1",
                "5678/2",
            )
            val handleClient = mockk<HandleCommunicator> {
                every {
                    listHandleValues(any())
                } returns ListHandlesResponse(
                    ListHandlesRequest(
                        Util.encodeString("5678"),
                        mockk<AuthenticationInfo>(relaxed = true)
                    ),
                    expectedHandles.map {
                        Util.encodeString(it)
                    }.toTypedArray()
                )
            }

            val handleServer = HandleGrpcServer(
                handleClient,
            )

            // when
            val response = handleServer.listHandleValues(ListHandleValuesRequest.getDefaultInstance())

            // then
            assertThat(response.handlesList.toSet(), `is`(expectedHandles.toSet()))
        }
    }

    @Test(expectedExceptions = [StatusRuntimeException::class])
    fun testHandleErrorResponse() {
        runBlocking {
            // given
            val handleClient = mockk<HandleCommunicator> {
                every {
                    addHandle(any())
                } returns ErrorResponse(1, 101, Util.encodeString("Error"))
            }
            val handleServer = HandleGrpcServer(
                handleClient,
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
            val handleClient = mockk<HandleCommunicator> {
                every {
                    addHandle(any())
                } throws HandleException(1)
            }
            val handleServer = HandleGrpcServer(
                handleClient,
            )
            // when
            handleServer.addHandle(AddHandleRequest.getDefaultInstance())
            // then boom
        }
    }
}
