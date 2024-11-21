package de.zbw.api.lori.server.utils

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class RestConverterUtilTest {
    @DataProvider(name = DATA_FOR_PREPARE_LICENCE_URL)
    fun createDataForLicenceLicenseUrl() =
        arrayOf(
            arrayOf(
                "http cc",
                "http://creativecommons.org/licenses/by/3.0/au",
                "by/3.0/au",
            ),
            arrayOf(
                "https cc",
                "https://creativecommons.org/licenses/by/3.0/au",
                "by/3.0/au",
            ),
            arrayOf(
                "other",
                "http://dx.doi.org/10.17811/ebl.6.2.2017.54-58",
                "other",
            ),
            arrayOf(
                "Empty string",
                "",
                null,
            ),
            arrayOf(
                "Null value",
                null,
                null,
            ),
        )

    @Test(dataProvider = DATA_FOR_PREPARE_LICENCE_URL)
    fun testPrepareLicenceUrlFilter(
        reason: String,
        licenceUrl: String?,
        expected: String?,
    ) {
        assertThat(
            reason,
            RestConverterUtil.prepareLicenceUrlFilter(licenceUrl),
            `is`(expected),
        )
    }

    companion object {
        const val DATA_FOR_PREPARE_LICENCE_URL = "DATA_FOR_PREPARE_LICENCE_URL"
    }
}
