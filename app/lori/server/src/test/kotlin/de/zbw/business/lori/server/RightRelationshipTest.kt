package de.zbw.business.lori.server

import de.zbw.lori.model.RelationshipRest
import de.zbw.persistence.lori.server.ConnectionPool
import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.DatabaseTest
import de.zbw.persistence.lori.server.ItemDBTest.Companion.TEST_RIGHT
import io.mockk.mockk
import io.opentelemetry.api.OpenTelemetry
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.testng.annotations.Test

class RightRelationshipTest : DatabaseTest() {
    private val backend =
        LoriServerBackend(
            DatabaseConnector(
                connectionPool = ConnectionPool(testDataSource),
                tracer = OpenTelemetry.noop().getTracer("foo"),
            ),
            mockk(),
        )

    @Test
    fun testRelationship() =
        runBlocking {
            val predecessorRight =
                TEST_RIGHT.copy(
                    rightId = "predecessor",
                    lastAppliedOn = null,
                    templateName = "pre",
                    isTemplate = true,
                    exceptionOfId = null,
                    hasExceptionId = null,
                )

            val middleRight =
                TEST_RIGHT.copy(
                    rightId = "middle",
                    lastAppliedOn = null,
                    templateName = "middle",
                    isTemplate = true,
                    exceptionOfId = null,
                    hasExceptionId = null,
                )

            val successorRight =
                TEST_RIGHT.copy(
                    rightId = "successor",
                    lastAppliedOn = null,
                    templateName = "suc",
                    isTemplate = true,
                    exceptionOfId = null,
                    hasExceptionId = null,
                )
            val preRightId = backend.insertRight(predecessorRight)
            val middleRightId = backend.insertRight(middleRight)
            val sucRightId = backend.insertRight(successorRight)

            // Add Predecessor
            backend.addRelationship(
                RelationshipRest(
                    relationship = RelationshipRest.Relationship.predecessor,
                    sourceRightId = middleRightId,
                    targetRightId = preRightId,
                ),
            )
            // Add Successor
            backend.addRelationship(
                RelationshipRest(
                    relationship = RelationshipRest.Relationship.successor,
                    sourceRightId = middleRightId,
                    targetRightId = sucRightId,
                ),
            )

            assertThat(
                backend.getRightById(middleRightId)!!.successorId!!,
                `is`(sucRightId),
            )
            assertThat(
                backend.getRightById(sucRightId)!!.predecessorId!!,
                `is`(middleRightId),
            )
        }
}
