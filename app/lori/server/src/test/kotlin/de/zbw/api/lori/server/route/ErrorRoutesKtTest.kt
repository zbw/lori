package de.zbw.api.lori.server.route

import com.google.gson.reflect.TypeToken
import de.zbw.api.lori.server.type.toRest
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.type.ConflictType
import de.zbw.business.lori.server.type.ErrorQueryResult
import de.zbw.business.lori.server.type.RightError
import de.zbw.lori.model.RightErrorInformationRest
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test
import java.lang.reflect.Type
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Testing [ErrorRoutesKtTest].
 *
 * Created on 01-18-2024.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class ErrorRoutesKtTest {
    @Test
    fun testGetBookmarkListOK() {
        // given
        val limit = 50
        val offset = 0
        val expected = TEST_ERROR_RESULT.toRest(limit)
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery {
                    getRightErrorList(
                        limit,
                        offset,
                        any(),
                    )
                } returns TEST_ERROR_RESULT
            }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.get("/api/v1/errors/rights/list?limit=$limit&offset=$offset")
            val content: String = response.bodyAsText()
            val rightErrorResultType: Type = object : TypeToken<RightErrorInformationRest>() {}.type
            val received: RightErrorInformationRest = ItemRoutesKtTest.GSON.fromJson(content, rightErrorResultType)
            assertThat(received, `is`(expected))
        }
    }

    companion object {
        val NOW: OffsetDateTime =
            OffsetDateTime.of(
                2022,
                3,
                1,
                1,
                1,
                0,
                0,
                ZoneOffset.UTC,
            )!!
        val TEST_ERROR =
            RightError(
                errorId = 1,
                message = "Timing conflict",
                conflictingWithRightId = "sourceRightId",
                conflictByRightId = "conflictingRightId",
                handleId = "somehandle",
                createdOn = NOW,
                metadataId = "metadataId",
                conflictType = ConflictType.DATE_OVERLAP,
                conflictByTemplateName = "template name",
            )
        val TEST_ERROR_RESULT =
            ErrorQueryResult(
                totalNumberOfResults = 1,
                templateNames = setOf(TEST_ERROR.conflictByTemplateName!!),
                conflictTypes = setOf(ConflictType.DATE_OVERLAP),
                results = listOf(TEST_ERROR),
            )
    }
}
