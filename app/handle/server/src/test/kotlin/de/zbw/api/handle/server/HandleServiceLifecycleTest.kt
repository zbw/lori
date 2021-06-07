package de.zbw.api.handle.server

import de.zbw.business.handle.server.HandleCommunicator
import de.zbw.persistence.handle.server.FlywayMigrator
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.handle.hdllib.AbstractMessage
import net.handle.hdllib.AbstractResponse
import net.handle.hdllib.GenericResponse
import net.handle.hdllib.ListHandlesResponse
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

/**
 * Testing [HandleServiceLifecycleTest].
 *
 * Created on 06-03-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class HandleServiceLifecycleTest {

    @DataProvider(name = DATA_FOR_READY_CHECK)
    fun createDataForReadiness() =
        arrayOf(
            arrayOf(
                ListHandlesResponse(),
                true,
            ),
            arrayOf(
                GenericResponse(AbstractMessage.OC_LIST_HANDLES, AbstractMessage.RC_AUTHEN_ERROR),
                false,
            ),
        )

    @Test(dataProvider = DATA_FOR_READY_CHECK)
    fun testIsReady(
        response: AbstractResponse,
        isReady: Boolean,
    ) {
        // given
        val communicator = mockk<HandleCommunicator>() {
            every {
                listHandleValues()
            } returns response
        }

        val handleService = HandleServiceLifecycle(
            mockk(),
            communicator,
            mockk(),
        )

        // when + then
        assertThat(handleService.isReady(), `is`(isReady))
    }

    @Test
    fun testStart() {
        // given
        val migrator = mockk<FlywayMigrator>() {
            every { migrate() } returns mockk()
        }
        val handleService = HandleServiceLifecycle(
            mockk(),
            mockk(),
            migrator,
        )

        // when
        handleService.start()

        // then
        verify(exactly = 1) { migrator.migrate() }
    }

    companion object {
        const val DATA_FOR_READY_CHECK = "DATA_FOR_READY_CHECK"
    }
}
