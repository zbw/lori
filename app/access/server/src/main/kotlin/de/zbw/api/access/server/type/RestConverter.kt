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

/**
 * Conversion functions between rest interface and business logic.
 *
 * Created on 07-28-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
fun AccessInformation.toBusiness() =
    AccessRight(
        header = Header(
            id = this.id,
            tenant = this.tenant,
            usageGuide = this.usageGuide,
            template = this.template,
            mention = this.mention ?: false,
            shareAlike = this.sharealike ?: false,
            commercialUse = this.commercialuse ?: false,
            copyright = this.copyright ?: false,
        ),
        actions = this.actions.map { action ->
            Action(
                type = action.actiontype.toBusiness(),
                permission = action.permission,
                restrictions = action.restrictions?.map { restriction -> restriction.toBusiness() } ?: emptyList()
            )
        }
    )

internal fun de.zbw.access.model.Restriction.toBusiness(): Restriction =
    Restriction(
        type = this.restrictiontype.toBusiness(),
        attribute = Attribute(
            type = this.attributetype.toBusiness(),
            values = this.attributevalues,
        )

    )

internal fun de.zbw.access.model.Restriction.Attributetype.toBusiness(): AttributeType =
    when (this) {
        de.zbw.access.model.Restriction.Attributetype.fromdate -> AttributeType.FROM_DATE
        de.zbw.access.model.Restriction.Attributetype.todate -> AttributeType.TO_DATE
        de.zbw.access.model.Restriction.Attributetype.maxresolution -> AttributeType.MAX_RESOLUTION
        de.zbw.access.model.Restriction.Attributetype.maxbitrate -> AttributeType.MAX_BITRATE
        de.zbw.access.model.Restriction.Attributetype.count -> AttributeType.COUNT
        de.zbw.access.model.Restriction.Attributetype.inside -> AttributeType.INSIDE
        de.zbw.access.model.Restriction.Attributetype.subnet -> AttributeType.SUBNET
        de.zbw.access.model.Restriction.Attributetype.outside -> AttributeType.OUTSIDE
        de.zbw.access.model.Restriction.Attributetype.watermark -> AttributeType.WATERMARK
        de.zbw.access.model.Restriction.Attributetype.duration -> AttributeType.DURATION
        de.zbw.access.model.Restriction.Attributetype.minage -> AttributeType.MIN_AGE
        de.zbw.access.model.Restriction.Attributetype.maxage -> AttributeType.MAX_AGE
        de.zbw.access.model.Restriction.Attributetype.required -> AttributeType.REQUIRED
        de.zbw.access.model.Restriction.Attributetype.groups -> AttributeType.GROUPS
        de.zbw.access.model.Restriction.Attributetype.parts -> AttributeType.PARTS
        de.zbw.access.model.Restriction.Attributetype.sessions -> AttributeType.SESSIONS
    }

internal fun de.zbw.access.model.Restriction.Restrictiontype.toBusiness(): RestrictionType =
    when (this) {
        de.zbw.access.model.Restriction.Restrictiontype.group -> RestrictionType.GROUP
        de.zbw.access.model.Restriction.Restrictiontype.age -> RestrictionType.AGE
        de.zbw.access.model.Restriction.Restrictiontype.location -> RestrictionType.LOCATION
        de.zbw.access.model.Restriction.Restrictiontype.date -> RestrictionType.DATE
        de.zbw.access.model.Restriction.Restrictiontype.duration -> RestrictionType.DURATION
        de.zbw.access.model.Restriction.Restrictiontype.count -> RestrictionType.COUNT
        de.zbw.access.model.Restriction.Restrictiontype.concurrent -> RestrictionType.CONCURRENT
        de.zbw.access.model.Restriction.Restrictiontype.watermark -> RestrictionType.WATERMARK
        de.zbw.access.model.Restriction.Restrictiontype.quality -> RestrictionType.QUALITY
        de.zbw.access.model.Restriction.Restrictiontype.agreement -> RestrictionType.AGREEMENT
        de.zbw.access.model.Restriction.Restrictiontype.parts -> RestrictionType.PARTS
    }

internal fun de.zbw.access.model.Action.Actiontype.toBusiness(): ActionType =
    when (this) {
        de.zbw.access.model.Action.Actiontype.read -> ActionType.READ
        de.zbw.access.model.Action.Actiontype.run -> ActionType.RUN
        de.zbw.access.model.Action.Actiontype.lend -> ActionType.LEND
        de.zbw.access.model.Action.Actiontype.download -> ActionType.DOWNLOAD
        de.zbw.access.model.Action.Actiontype.print -> ActionType.PRINT
        de.zbw.access.model.Action.Actiontype.reproduce -> ActionType.REPRODUCE
        de.zbw.access.model.Action.Actiontype.modify -> ActionType.MODIFY
        de.zbw.access.model.Action.Actiontype.reuse -> ActionType.REUSE
        de.zbw.access.model.Action.Actiontype.distribute -> ActionType.DISTRIBUTE
        de.zbw.access.model.Action.Actiontype.publish -> ActionType.PUBLISH
        de.zbw.access.model.Action.Actiontype.archive -> ActionType.ARCHIVE
        de.zbw.access.model.Action.Actiontype.index -> ActionType.INDEX
        de.zbw.access.model.Action.Actiontype.move -> ActionType.MOVE
        de.zbw.access.model.Action.Actiontype.displaymetadata -> ActionType.DISPLAY_METADATA
    }

fun AccessRight.toRest(): AccessInformation =
    AccessInformation(
        id = this.header.id,
        tenant = this.header.tenant,
        usageGuide = this.header.usageGuide,
        template = this.header.template,
        mention = this.header.mention,
        sharealike = this.header.shareAlike,
        commercialuse = this.header.commercialUse,
        copyright = this.header.copyright,
        actions = this.actions.map { a ->
            de.zbw.access.model.Action(
                actiontype = a.type.toRest(),
                permission = a.permission,
                restrictions = a.restrictions.map { r ->
                    de.zbw.access.model.Restriction(
                        restrictiontype = r.type.toRest(),
                        attributetype = r.attribute.type.toRest(),
                        attributevalues = r.attribute.values,
                    )
                }
            )
        }
    )

internal fun AttributeType.toRest(): de.zbw.access.model.Restriction.Attributetype =
    when (this) {
        AttributeType.FROM_DATE -> de.zbw.access.model.Restriction.Attributetype.fromdate
        AttributeType.TO_DATE -> de.zbw.access.model.Restriction.Attributetype.todate
        AttributeType.MAX_RESOLUTION -> de.zbw.access.model.Restriction.Attributetype.maxresolution
        AttributeType.MAX_BITRATE -> de.zbw.access.model.Restriction.Attributetype.maxbitrate
        AttributeType.COUNT -> de.zbw.access.model.Restriction.Attributetype.count
        AttributeType.INSIDE -> de.zbw.access.model.Restriction.Attributetype.inside
        AttributeType.SUBNET -> de.zbw.access.model.Restriction.Attributetype.subnet
        AttributeType.OUTSIDE -> de.zbw.access.model.Restriction.Attributetype.outside
        AttributeType.WATERMARK -> de.zbw.access.model.Restriction.Attributetype.watermark
        AttributeType.DURATION -> de.zbw.access.model.Restriction.Attributetype.duration
        AttributeType.MIN_AGE -> de.zbw.access.model.Restriction.Attributetype.minage
        AttributeType.MAX_AGE -> de.zbw.access.model.Restriction.Attributetype.maxage
        AttributeType.REQUIRED -> de.zbw.access.model.Restriction.Attributetype.required
        AttributeType.GROUPS -> de.zbw.access.model.Restriction.Attributetype.groups
        AttributeType.PARTS -> de.zbw.access.model.Restriction.Attributetype.parts
        AttributeType.SESSIONS -> de.zbw.access.model.Restriction.Attributetype.sessions
    }

internal fun RestrictionType.toRest(): de.zbw.access.model.Restriction.Restrictiontype =
    when (this) {
        RestrictionType.GROUP -> de.zbw.access.model.Restriction.Restrictiontype.group
        RestrictionType.AGE -> de.zbw.access.model.Restriction.Restrictiontype.age
        RestrictionType.LOCATION -> de.zbw.access.model.Restriction.Restrictiontype.location
        RestrictionType.DATE -> de.zbw.access.model.Restriction.Restrictiontype.date
        RestrictionType.DURATION -> de.zbw.access.model.Restriction.Restrictiontype.duration
        RestrictionType.COUNT -> de.zbw.access.model.Restriction.Restrictiontype.count
        RestrictionType.CONCURRENT -> de.zbw.access.model.Restriction.Restrictiontype.concurrent
        RestrictionType.WATERMARK -> de.zbw.access.model.Restriction.Restrictiontype.watermark
        RestrictionType.QUALITY -> de.zbw.access.model.Restriction.Restrictiontype.quality
        RestrictionType.AGREEMENT -> de.zbw.access.model.Restriction.Restrictiontype.agreement
        RestrictionType.PARTS -> de.zbw.access.model.Restriction.Restrictiontype.parts
    }

fun ActionType.toRest() =
    when (this) {
        ActionType.READ -> de.zbw.access.model.Action.Actiontype.read
        ActionType.RUN -> de.zbw.access.model.Action.Actiontype.run
        ActionType.LEND -> de.zbw.access.model.Action.Actiontype.lend
        ActionType.DOWNLOAD -> de.zbw.access.model.Action.Actiontype.download
        ActionType.PRINT -> de.zbw.access.model.Action.Actiontype.print
        ActionType.REPRODUCE -> de.zbw.access.model.Action.Actiontype.reproduce
        ActionType.MODIFY -> de.zbw.access.model.Action.Actiontype.modify
        ActionType.REUSE -> de.zbw.access.model.Action.Actiontype.reuse
        ActionType.DISTRIBUTE -> de.zbw.access.model.Action.Actiontype.distribute
        ActionType.PUBLISH -> de.zbw.access.model.Action.Actiontype.publish
        ActionType.ARCHIVE -> de.zbw.access.model.Action.Actiontype.archive
        ActionType.INDEX -> de.zbw.access.model.Action.Actiontype.index
        ActionType.MOVE -> de.zbw.access.model.Action.Actiontype.move
        ActionType.DISPLAY_METADATA -> de.zbw.access.model.Action.Actiontype.displaymetadata
    }
