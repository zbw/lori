package de.zbw.business.lori.server

import de.zbw.persistence.lori.server.ConnectionPool
import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.DatabaseTest
import de.zbw.persistence.lori.server.ItemDBTest.Companion.NOW
import de.zbw.persistence.lori.server.ItemDBTest.Companion.TEST_Metadata
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.opentelemetry.api.OpenTelemetry
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test
import java.time.Instant

/**
 * Testing if Metadata will be marked as deleted depending on last_updated_on column.
 *
 * Created on 20-01-2025.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class MetadataDeleteTest : DatabaseTest() {
    private val backend =
        LoriServerBackend(
            DatabaseConnector(
                connectionPool = ConnectionPool(testDataSource),
                tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LoriServerBackendTest"),
            ),
            mockk(),
        )

    @Test
    fun testUpdateMetadataAsDeleted() =
        runBlocking {
            mockkStatic(Instant::class)
            every { Instant.now() } returns NOW.minusDays(14L).toInstant()
            val deletedMetadata =
                TEST_Metadata.copy(
                    handle = "two-weeks-old",
                    deleted = false,
                )
            backend.insertMetadataElement(deletedMetadata)
            mockkStatic(Instant::class)
            every { Instant.now() } returns NOW.toInstant()
            val upToDateMetadata =
                TEST_Metadata.copy(
                    handle = "up-to-date",
                    deleted = false,
                )
            backend.insertMetadataElement(upToDateMetadata)

            // When
            val markedAsDeleted = backend.updateMetadataAsDeleted(NOW.toInstant())

            // Then
            assertThat(
                markedAsDeleted,
                `is`(1),
            )

            val receivedMetadata = backend.getMetadataElementsByIds(listOf(deletedMetadata.handle, upToDateMetadata.handle))
            assertThat(
                receivedMetadata.toSet(),
                `is`(
                    setOf(
                        upToDateMetadata.copy(createdOn = NOW, lastUpdatedOn = NOW),
                        deletedMetadata.copy(deleted = true, createdOn = NOW.minusDays(14L), lastUpdatedOn = NOW.minusDays(14L)),
                    ),
                ),
            )
        }
}
