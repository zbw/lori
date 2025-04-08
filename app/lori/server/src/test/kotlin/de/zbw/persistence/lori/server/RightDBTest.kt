package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.BasisAccessState
import de.zbw.business.lori.server.type.BasisStorage
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.persistence.lori.server.ItemDBTest.Companion.NOW
import de.zbw.persistence.lori.server.ItemDBTest.Companion.TEST_RIGHT
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.opentelemetry.api.OpenTelemetry
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.postgresql.util.PSQLException
import org.testng.Assert
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.time.Instant
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Testing [RightDB].
 *
 * Created on 03-17-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class RightDBTest : DatabaseTest() {
    private val dbConnector =
        DatabaseConnector(
            connectionPool = ConnectionPool(testDataSource),
            tracer = OpenTelemetry.noop().getTracer("foo"),
        )

    @BeforeMethod
    fun beforeTest() {
        mockkStatic(Instant::class)
        every { Instant.now() } returns NOW.toInstant()
    }

    @AfterMethod
    fun afterTest() {
        unmockkAll()
    }

    @Test
    fun testRightRoundtrip() =
        runBlocking {
            // given
            val initialRight = TEST_RIGHT

            // Insert
            // when
            val generatedRightId = dbConnector.rightDB.insertRight(initialRight)
            val receivedRights: List<ItemRight> = dbConnector.rightDB.getRightsByIds(listOf(generatedRightId))

            // then
            assertThat(receivedRights.first(), `is`(initialRight.copy(rightId = generatedRightId, lastAppliedOn = null)))
            assertTrue(dbConnector.rightDB.rightContainsId(generatedRightId))

            // upsert

            // given
            val updatedRight =
                TEST_UPDATED_RIGHT.copy(
                    rightId = generatedRightId,
                )
            mockkStatic(Instant::class)
            every { Instant.now() } returns NOW.plusDays(1).toInstant()

            // when
            val updatedRights = dbConnector.rightDB.upsertRight(updatedRight)

            // then
            assertThat(updatedRights, `is`(1))
            val receivedUpdatedRights: List<ItemRight> = dbConnector.rightDB.getRightsByIds(listOf(generatedRightId))
            assertThat(
                receivedUpdatedRights.first(),
                `is`(updatedRight.copy(lastUpdatedOn = NOW.plusDays(1), lastAppliedOn = null)),
            )

            // delete
            // when
            val deletedItems = dbConnector.rightDB.deleteRightsByIds(listOf(generatedRightId))

            // then
            assertThat(deletedItems, `is`(1))

            // when + then
            assertThat(dbConnector.rightDB.getRightsByIds(listOf(generatedRightId)), `is`(emptyList()))
            assertFalse(dbConnector.rightDB.rightContainsId(generatedRightId))
        }

    @Test
    fun testGetRightsByTemplateName() =
        runBlocking {
            val templateName1 = "foobar"
            val templateName2 = "baz"
            val rightId1 = dbConnector.rightDB.insertRight(TEST_RIGHT.copy(isTemplate = true, templateName = templateName1))
            val rightId2 = dbConnector.rightDB.insertRight(TEST_RIGHT.copy(isTemplate = true, templateName = templateName2))
            val receivedRightIds =
                dbConnector.rightDB
                    .getRightsByTemplateNames(listOf(templateName2, templateName1))
                    .map { it.rightId }
                    .toSet()

            assertThat(
                receivedRightIds,
                `is`(setOf(rightId1, rightId2)),
            )
            // Clean up for other tests
            dbConnector.rightDB.deleteRightsByIds(listOf(rightId2, rightId1))
        }

    @Test
    fun testTemplateExceptions() =
        runBlocking {
            val templateName1 = "foobar"
            val templateName2 = "baz"
            val rightId1 = dbConnector.rightDB.insertRight(TEST_RIGHT.copy(isTemplate = true, templateName = templateName1))

            // Create Template which is an exception of the first one.
            val rightId2 =
                dbConnector.rightDB.insertRight(
                    TEST_RIGHT.copy(
                        isTemplate = true,
                        templateName = templateName2,
                        exceptionFrom = rightId1,
                    ),
                )

            val exceptionRights: ItemRight? = dbConnector.rightDB.getExceptionByRightId(rightId1)

            assertThat(
                exceptionRights!!.rightId,
                `is`(rightId2),
            )

            // Clean up for other tests
            dbConnector.rightDB.deleteRightsByIds(listOf(rightId2))
            dbConnector.rightDB.deleteRightsByIds(listOf(rightId1))
        }

    @Test(expectedExceptions = [PSQLException::class])
    fun testUniqueConstraintOnTemplates() =
        runBlocking {
            val templateName1 = "foobar"
            dbConnector.rightDB.insertRight(TEST_RIGHT.copy(isTemplate = true, templateName = templateName1))
            dbConnector.rightDB.insertRight(TEST_RIGHT.copy(isTemplate = true, templateName = templateName1))
            Assert.fail()
        }

    @Test
    fun testTemplateException() =
        runBlocking {
            val templateNameUpper = "upper"
            val upperID =
                dbConnector.rightDB.insertRight(
                    TEST_RIGHT.copy(
                        isTemplate = true,
                        lastAppliedOn = null,
                        templateName = templateNameUpper,
                    ),
                )
            val templateNameException = "exception"
            val exceptionTemplate1 =
                TEST_RIGHT.copy(isTemplate = true, lastAppliedOn = null, templateName = templateNameException)
            val excID1 = dbConnector.rightDB.insertRight(exceptionTemplate1)

            assertFalse(
                dbConnector.rightDB.isException(upperID),
            )
            dbConnector.rightDB.addExceptionToTemplate(
                rightIdExceptions = listOf(excID1),
                rightIdTemplate = upperID,
            )
            assertFalse(
                dbConnector.rightDB.isException(upperID),
            )
            assertTrue(
                dbConnector.rightDB.isException(excID1),
            )
            val result = dbConnector.rightDB.getExceptionByRightId(upperID)
            assertThat(
                result,
                `is`(
                    exceptionTemplate1.copy(rightId = excID1, exceptionFrom = upperID),
                ),
            )
        }

    companion object {
        val TEST_UPDATED_RIGHT =
            ItemRight(
                rightId = "testupdated",
                accessState = AccessState.RESTRICTED,
                authorRightException = false,
                basisAccessState = BasisAccessState.ZBW_POLICY,
                basisStorage = BasisStorage.LICENCE_CONTRACT,
                createdBy = TEST_RIGHT.createdBy,
                createdOn = TEST_RIGHT.createdOn,
                endDate = TEST_RIGHT.endDate!!.plusDays(1),
                exceptionFrom = null,
                groups = TEST_RIGHT.groups,
                groupIds = TEST_RIGHT.groups?.map { it.groupId },
                isTemplate = true,
                lastUpdatedBy = "user4",
                lastAppliedOn = TEST_RIGHT.lastAppliedOn,
                lastUpdatedOn = TEST_RIGHT.lastUpdatedOn,
                licenceContract = "foo licence",
                notesGeneral = "Some more general notes",
                notesFormalRules = "Some more formal rule notes",
                notesProcessDocumentation = "Some more process documentation",
                notesManagementRelated = "Some more management related notes",
                restrictedOpenContentLicence = true,
                startDate = TEST_RIGHT.startDate.minusDays(10),
                templateDescription = "description foo",
                templateName = "name foo",
                zbwUserAgreement = true,
            )
    }
}
