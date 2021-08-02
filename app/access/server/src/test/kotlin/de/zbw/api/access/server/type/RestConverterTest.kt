package de.zbw.api.access.server.type

import de.zbw.access.model.AccessInformation
import de.zbw.business.access.server.AccessRight
import de.zbw.business.access.server.Action
import de.zbw.business.access.server.ActionType
import de.zbw.business.access.server.Attribute
import de.zbw.business.access.server.AttributeType
import de.zbw.business.access.server.Header
import de.zbw.business.access.server.Restriction
import de.zbw.business.access.server.RestrictionType
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test

class RestConverterTest {

    @Test
    fun testAccessRightConversion() {
        // given
        val expected = AccessRight(
            header = Header(
                id = "foo",
                tenant = "bla",
                usageGuide = "guide",
                template = null,
                mention = true,
                shareAlike = false,
                commercialUse = true,
                copyright = true,
            ),
            actions = listOf(
                Action(
                    type = ActionType.READ,
                    permission = true,
                    restrictions = listOf(
                        Restriction(
                            type = RestrictionType.DATE,
                            attribute = Attribute(
                                type = AttributeType.FROM_DATE,
                                values = listOf("2022-01-01")
                            )
                        )
                    )
                )
            ),
        )

        val restObject = AccessInformation(
            id = "foo",
            tenant = "bla",
            usageGuide = "guide",
            template = null,
            mention = true,
            sharealike = false,
            commercialuse = true,
            copyright = true,
            actions = listOf(
                de.zbw.access.model.Action(
                    permission = true,
                    actiontype = de.zbw.access.model.Action.Actiontype.read,
                    restrictions = listOf(
                        de.zbw.access.model.Restriction(
                            restrictiontype = de.zbw.access.model.Restriction.Restrictiontype.date,
                            attributetype = de.zbw.access.model.Restriction.Attributetype.fromdate,
                            attributevalues = listOf("2022-01-01"),
                        )
                    )
                )
            ),
        )

        // when + then
        assertThat(restObject.toBusiness(), `is`(expected))
    }

    @Test
    fun testActionTypeConversionRoundtrip() {
        de.zbw.access.model.Action.Actiontype.values().toList().forEach {
            assertThat(it.toBusiness().toRest(), `is`(it))
        }
    }

    @Test
    fun testAttributeTypeConversionRoundtrip() {
        de.zbw.access.model.Restriction.Attributetype.values().toList().forEach {
            assertThat(
                it.toBusiness().toRest(),
                `is`(it)
            )
        }
    }

    @Test
    fun testRestrictionTypeConversionRoundtrip() {
        de.zbw.access.model.Restriction.Restrictiontype.values().toList().forEach {
            assertThat(
                it.toBusiness().toRest(),
                `is`(it)
            )
        }
    }
}
