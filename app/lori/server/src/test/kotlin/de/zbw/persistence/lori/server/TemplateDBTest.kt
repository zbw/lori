package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.Template
import de.zbw.persistence.lori.server.ItemDBTest.Companion.TEST_RIGHT
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.opentelemetry.api.OpenTelemetry
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import java.time.Instant

/**
 * Testing [TemplateDB].
 *
 * Created on 04-19-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class TemplateDBTest : DatabaseTest() {
    private val dbConnector = DatabaseConnector(
        connection = dataSource.connection,
        tracer = OpenTelemetry.noop().getTracer("foo"),
    ).templateDB

    @BeforeClass
    fun beforeTests() {
        mockkStatic(Instant::class)
        every { Instant.now() } returns ItemDBTest.NOW.toInstant()
    }

    @AfterClass
    fun afterTests() {
        unmockkAll()
    }

    @Test
    fun testTemplateRoundtrip() {
        // Case: Create and Read
        // when
        val generatedIds = dbConnector.insertTemplate(TEST_TEMPLATE)
        val receivedTemplates = dbConnector.getTemplatesByIds(listOf(generatedIds.templateId))
        val expected = TEST_TEMPLATE.copy(
            templateId = generatedIds.templateId,
            right = TEST_RIGHT.copy(rightId = generatedIds.rightId)
        )
        // then
        assertThat(
            receivedTemplates.first().toString(),
            `is`(expected.toString())
        )

        // Case: Update
        // when
        val expectedUpdated = TEST_TEMPLATE.copy(
            templateId = generatedIds.templateId,
            description = "fooo",
            right = expected.right.copy(licenceContract = "bar")
        )
        val updatedNumber: Int = dbConnector.updateTemplateById(
            generatedIds.templateId, expectedUpdated
        )
        // then
        assertThat(updatedNumber, `is`(1))
        assertThat(
            dbConnector.getTemplatesByIds(listOf(generatedIds.templateId)).first().toString(),
            `is`(expectedUpdated.toString())
        )

        // Case: Delete
        // when
        val countDeleted = dbConnector.deleteTemplateById(generatedIds.templateId)
        // then
        assertThat(countDeleted, `is`(1))
        assertThat(
            dbConnector.getTemplatesByIds(listOf(generatedIds.templateId)),
            `is`(emptyList())
        )
    }

    @Test
    fun testTemplateGetList() {
        dbConnector.insertTemplate(TEST_TEMPLATE.copy(templateName = "aa"))
        dbConnector.insertTemplate(TEST_TEMPLATE.copy(templateName = "ab"))
        dbConnector.insertTemplate(TEST_TEMPLATE.copy(templateName = "ac"))
        val ids4 = dbConnector.insertTemplate(TEST_TEMPLATE.copy(templateName = "ad"))
        val ids5 = dbConnector.insertTemplate(TEST_TEMPLATE.copy(templateName = "ae"))
        val expected = listOf(
            TEST_TEMPLATE.copy(
                templateName = "ad",
                templateId = ids4.templateId,
                right = TEST_RIGHT.copy(rightId = ids4.rightId)
            ),
            TEST_TEMPLATE.copy(
                templateName = "ae",
                templateId = ids5.templateId,
                right = TEST_RIGHT.copy(rightId = ids5.rightId)
            ),
        )

        val received: List<Template> = dbConnector.getTemplateList(2, 3)
        assertThat(
            received,
            `is`(expected),
        )
    }

    companion object {
        val TEST_TEMPLATE = Template(
            templateId = 1,
            templateName = "test",
            description = "some description",
            right = TEST_RIGHT
        )
    }
}
