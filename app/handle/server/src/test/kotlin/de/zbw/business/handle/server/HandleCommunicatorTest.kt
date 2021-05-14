package de.zbw.business.handle.server

import de.zbw.handle.api.AddHandleRequest
import de.zbw.handle.api.AddHandleRequest.HandleType
import io.grpc.StatusRuntimeException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import net.handle.hdllib.CreateHandleRequest
import net.handle.hdllib.CreateHandleResponse
import net.handle.hdllib.HandleResolver
import net.handle.hdllib.HandleValue
import net.handle.hdllib.Util
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.Assert.assertTrue
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.io.File

/**
 * Testing [HandleCommunicator].
 *
 * Created on 05-14-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class HandleCommunicatorTest {

    @Test
    fun testGetConfigPath() {
        val uri = HandleCommunicator.getConfigURI()!!
        val file = File(uri)

        val dirContent = file.listFiles()?.asSequence()?.map { it.name }?.toSet() ?: emptySet()

        assertThat(
            dirContent,
            `is`(
                setOf(
                    "bootstrap_handles",
                    "local_nas",
                    "resolver_site",
                    "root_info",
                )
            )
        )
    }

    @Test
    fun testCreatingResolver() {
        HandleCommunicator("my_password").resolver
        assertThat(
            System.getProperty(HandleCommunicator.HANDLE_CONFIG_DIR_PROP),
            `is`(HandleCommunicator.getConfigURI()?.path!!)
        )
    }

    @Test
    fun testAddHandle() {
        // given
        val suffix = "9999"
        val handleValueIdx = 1
        val expectedHandle = "${HandleCommunicator.PREFIX}/$suffix"
        val handleValueEmail = "mail@exampl.com"

        val slot = slot<CreateHandleRequest>()
        val resolver = mockk<HandleResolver>() {
            every {
                processRequest(capture(slot))
            } returns CreateHandleResponse(Util.encodeString(expectedHandle))
        }
        val communicator = HandleCommunicator(
            "my_password",
            resolver,
        )

        val request = AddHandleRequest.newBuilder()
            .setHandleSuffix(suffix)
            .addAllHandleValues(
                listOf(
                    AddHandleRequest.HandleValue.newBuilder()
                        .setIndex(handleValueIdx)
                        .setType(HandleType.HANDLE_TYPE_EMAIL)
                        .setValue(handleValueEmail)
                        .build()
                )
            )
            .build()

        // when
        val received = communicator.addHandle(request)

        // then
        val capturedRequest = slot.captured
        val handleValue: HandleValue = capturedRequest.values.first()

        assertThat(Util.decodeString(capturedRequest.handle), `is`(expectedHandle))
        assertThat(handleValue.index, `is`(handleValueIdx))
        assertThat(handleValue.dataAsString, `is`(handleValueEmail))
        assertThat(handleValue.typeAsString, `is`(HandleType.HANDLE_TYPE_EMAIL.convertToHandleValue()))

        verify(exactly = 1) { resolver.processRequest(capturedRequest) }

        assertTrue(received is CreateHandleResponse)
        assertThat(Util.decodeString((received as CreateHandleResponse).handle), `is`(expectedHandle))
    }

    @DataProvider(name = DATA_FOR_CONVERSION)
    private fun createDataForConversion() =
        arrayOf(
            arrayOf(
                HandleType.HANDLE_TYPE_ADMIN,
                "HS_ADMIN",
            ),
            arrayOf(
                HandleType.HANDLE_TYPE_ALIAS,
                "HS_ALIAS",
            ),
            arrayOf(
                HandleType.HANDLE_TYPE_EMAIL,
                "EMAIL",
            ),
            arrayOf(
                HandleType.HANDLE_TYPE_PUBKEY,
                "HS_PUBKEY",
            ),
            arrayOf(
                HandleType.HANDLE_TYPE_SECKEY,
                "HS_SECKEY",
            ),
            arrayOf(
                HandleType.HANDLE_TYPE_URN,
                "URN",
            ),
            arrayOf(
                HandleType.HANDLE_TYPE_URL,
                "URL",
            ),
            arrayOf(
                HandleType.HANDLE_TYPE_VLIST,
                "HS_VLIST",
            ),
        )

    @Test(dataProvider = DATA_FOR_CONVERSION)
    fun testHandleValueConversion(
        handleType: HandleType,
        expected: String,
    ) {
        assertThat(
            handleType.convertToHandleValue(),
            `is`(expected),
        )
    }

    @Test(expectedExceptions = [StatusRuntimeException::class])
    fun testHandleValueConversionError() {
        HandleType.HANDLE_TYPE_UNSPECIFIED.convertToHandleValue()
    }

    companion object {
        const val DATA_FOR_CONVERSION = "DATA_FOR_CONVERSION"
    }
}
