package de.zbw.business.lori.server

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.testng.annotations.Test

class SearchKeyTest {
    @Test
    fun testBijectivity() {
        SearchKey.values().map { key ->
            assertThat(
                SearchKey.toEnum(key.fromEnum()),
                `is`(key),
            )
        }
    }
}
