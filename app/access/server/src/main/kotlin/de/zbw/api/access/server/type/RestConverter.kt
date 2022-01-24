package de.zbw.api.access.server.type

import de.zbw.access.model.ActionRest
import de.zbw.access.model.ItemRest
import de.zbw.access.model.RestrictionRest
import de.zbw.business.access.server.AccessState
import de.zbw.business.access.server.Action
import de.zbw.business.access.server.ActionType
import de.zbw.business.access.server.Attribute
import de.zbw.business.access.server.AttributeType
import de.zbw.business.access.server.Item
import de.zbw.business.access.server.PublicationType
import de.zbw.business.access.server.Restriction
import de.zbw.business.access.server.RestrictionType

/**
 * Conversion functions between rest interface and business logic.
 *
 * Created on 07-28-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
fun ItemRest.toBusiness() =
    Item(
        itemMetadata = de.zbw.business.access.server.ItemMetadata(
            id = id,
            handle = handle,
            ppn = ppn,
            ppnEbook = ppnEbook,
            title = title,
            titleJournal = titleJournal,
            titleSeries = titleSeries,
            accessState = accessState?.toBusiness(),
            publicationYear = publicationYear,
            band = band,
            publicationType = publicationType.toBusiness(),
            doi = doi,
            serialNumber = serialNumber,
            isbn = isbn,
            rightsK10plus = rightsK10plus,
            paketSigel = paketSigel,
            zbdId = zbdId,
            issn = issn,
            licenseConditions = licenseConditions,
            provenanceLicense = provenanceLicense,
        ),
        actions = this.actions.map { action ->
            Action(
                type = action.actiontype.toBusiness(),
                permission = action.permission,
                restrictions = action.restrictions?.map { restriction -> restriction.toBusiness() } ?: emptyList()
            )
        }
    )

internal fun RestrictionRest.toBusiness(): Restriction =
    Restriction(
        type = this.restrictiontype.toBusiness(),
        attribute = Attribute(
            type = this.attributetype.toBusiness(),
            values = this.attributevalues,
        )

    )

internal fun RestrictionRest.Attributetype.toBusiness(): AttributeType =
    when (this) {
        RestrictionRest.Attributetype.fromdate -> AttributeType.FROM_DATE
        RestrictionRest.Attributetype.todate -> AttributeType.TO_DATE
        RestrictionRest.Attributetype.maxresolution -> AttributeType.MAX_RESOLUTION
        RestrictionRest.Attributetype.maxbitrate -> AttributeType.MAX_BITRATE
        RestrictionRest.Attributetype.count -> AttributeType.COUNT
        RestrictionRest.Attributetype.inside -> AttributeType.INSIDE
        RestrictionRest.Attributetype.subnet -> AttributeType.SUBNET
        RestrictionRest.Attributetype.outside -> AttributeType.OUTSIDE
        RestrictionRest.Attributetype.watermark -> AttributeType.WATERMARK
        RestrictionRest.Attributetype.duration -> AttributeType.DURATION
        RestrictionRest.Attributetype.minage -> AttributeType.MIN_AGE
        RestrictionRest.Attributetype.maxage -> AttributeType.MAX_AGE
        RestrictionRest.Attributetype.required -> AttributeType.REQUIRED
        RestrictionRest.Attributetype.groups -> AttributeType.GROUPS
        RestrictionRest.Attributetype.parts -> AttributeType.PARTS
        RestrictionRest.Attributetype.sessions -> AttributeType.SESSIONS
    }

internal fun RestrictionRest.Restrictiontype.toBusiness(): RestrictionType =
    when (this) {
        RestrictionRest.Restrictiontype.group -> RestrictionType.GROUP
        RestrictionRest.Restrictiontype.age -> RestrictionType.AGE
        RestrictionRest.Restrictiontype.location -> RestrictionType.LOCATION
        RestrictionRest.Restrictiontype.date -> RestrictionType.DATE
        RestrictionRest.Restrictiontype.duration -> RestrictionType.DURATION
        RestrictionRest.Restrictiontype.count -> RestrictionType.COUNT
        RestrictionRest.Restrictiontype.concurrent -> RestrictionType.CONCURRENT
        RestrictionRest.Restrictiontype.watermark -> RestrictionType.WATERMARK
        RestrictionRest.Restrictiontype.quality -> RestrictionType.QUALITY
        RestrictionRest.Restrictiontype.agreement -> RestrictionType.AGREEMENT
        RestrictionRest.Restrictiontype.parts -> RestrictionType.PARTS
    }

internal fun ActionRest.Actiontype.toBusiness(): ActionType =
    when (this) {
        ActionRest.Actiontype.read -> ActionType.READ
        ActionRest.Actiontype.run -> ActionType.RUN
        ActionRest.Actiontype.lend -> ActionType.LEND
        ActionRest.Actiontype.download -> ActionType.DOWNLOAD
        ActionRest.Actiontype.print -> ActionType.PRINT
        ActionRest.Actiontype.reproduce -> ActionType.REPRODUCE
        ActionRest.Actiontype.modify -> ActionType.MODIFY
        ActionRest.Actiontype.reuse -> ActionType.REUSE
        ActionRest.Actiontype.distribute -> ActionType.DISTRIBUTE
        ActionRest.Actiontype.publish -> ActionType.PUBLISH
        ActionRest.Actiontype.archive -> ActionType.ARCHIVE
        ActionRest.Actiontype.index -> ActionType.INDEX
        ActionRest.Actiontype.move -> ActionType.MOVE
        ActionRest.Actiontype.displaymetadata -> ActionType.DISPLAY_METADATA
    }

fun Item.toRest(): ItemRest =
    ItemRest(
        id = this.itemMetadata.id,
        accessState = this.itemMetadata.accessState?.toRest(),
        band = this.itemMetadata.band,
        doi = this.itemMetadata.doi,
        handle = this.itemMetadata.handle,
        isbn = this.itemMetadata.isbn,
        issn = this.itemMetadata.issn,
        paketSigel = this.itemMetadata.paketSigel,
        ppn = this.itemMetadata.ppn,
        ppnEbook = this.itemMetadata.ppnEbook,
        publicationType = this.itemMetadata.publicationType.toRest(),
        publicationYear = this.itemMetadata.publicationYear,
        rightsK10plus = this.itemMetadata.rightsK10plus,
        serialNumber = this.itemMetadata.serialNumber,
        title = this.itemMetadata.title,
        titleJournal = this.itemMetadata.titleJournal,
        titleSeries = this.itemMetadata.titleSeries,
        zbdId = this.itemMetadata.zbdId,
        actions = this.actions.map { a ->
            ActionRest(
                actiontype = a.type.toRest(),
                permission = a.permission,
                restrictions = a.restrictions.map { r ->
                    RestrictionRest(
                        restrictiontype = r.type.toRest(),
                        attributetype = r.attribute.type.toRest(),
                        attributevalues = r.attribute.values,
                    )
                }
            )
        }
    )

internal fun ItemRest.AccessState.toBusiness(): AccessState =
    when (this) {
        ItemRest.AccessState.closed -> AccessState.CLOSED
        ItemRest.AccessState.open -> AccessState.OPEN
        ItemRest.AccessState.restricted -> AccessState.RESTRICTED
    }

internal fun AccessState.toRest(): ItemRest.AccessState =
    when (this) {
        AccessState.CLOSED -> ItemRest.AccessState.closed
        AccessState.OPEN -> ItemRest.AccessState.open
        AccessState.RESTRICTED -> ItemRest.AccessState.restricted
    }

internal fun ItemRest.PublicationType.toBusiness(): PublicationType =
    when (this) {
        ItemRest.PublicationType.mono -> PublicationType.MONO
        ItemRest.PublicationType.periodical -> PublicationType.PERIODICAL
    }

internal fun PublicationType.toRest(): ItemRest.PublicationType =
    when (this) {
        PublicationType.MONO -> ItemRest.PublicationType.mono
        PublicationType.PERIODICAL -> ItemRest.PublicationType.periodical
    }

internal fun AttributeType.toRest(): RestrictionRest.Attributetype =
    when (this) {
        AttributeType.FROM_DATE -> RestrictionRest.Attributetype.fromdate
        AttributeType.TO_DATE -> RestrictionRest.Attributetype.todate
        AttributeType.MAX_RESOLUTION -> RestrictionRest.Attributetype.maxresolution
        AttributeType.MAX_BITRATE -> RestrictionRest.Attributetype.maxbitrate
        AttributeType.COUNT -> RestrictionRest.Attributetype.count
        AttributeType.INSIDE -> RestrictionRest.Attributetype.inside
        AttributeType.SUBNET -> RestrictionRest.Attributetype.subnet
        AttributeType.OUTSIDE -> RestrictionRest.Attributetype.outside
        AttributeType.WATERMARK -> RestrictionRest.Attributetype.watermark
        AttributeType.DURATION -> RestrictionRest.Attributetype.duration
        AttributeType.MIN_AGE -> RestrictionRest.Attributetype.minage
        AttributeType.MAX_AGE -> RestrictionRest.Attributetype.maxage
        AttributeType.REQUIRED -> RestrictionRest.Attributetype.required
        AttributeType.GROUPS -> RestrictionRest.Attributetype.groups
        AttributeType.PARTS -> RestrictionRest.Attributetype.parts
        AttributeType.SESSIONS -> RestrictionRest.Attributetype.sessions
    }

internal fun RestrictionType.toRest(): RestrictionRest.Restrictiontype =
    when (this) {
        RestrictionType.GROUP -> RestrictionRest.Restrictiontype.group
        RestrictionType.AGE -> RestrictionRest.Restrictiontype.age
        RestrictionType.LOCATION -> RestrictionRest.Restrictiontype.location
        RestrictionType.DATE -> RestrictionRest.Restrictiontype.date
        RestrictionType.DURATION -> RestrictionRest.Restrictiontype.duration
        RestrictionType.COUNT -> RestrictionRest.Restrictiontype.count
        RestrictionType.CONCURRENT -> RestrictionRest.Restrictiontype.concurrent
        RestrictionType.WATERMARK -> RestrictionRest.Restrictiontype.watermark
        RestrictionType.QUALITY -> RestrictionRest.Restrictiontype.quality
        RestrictionType.AGREEMENT -> RestrictionRest.Restrictiontype.agreement
        RestrictionType.PARTS -> RestrictionRest.Restrictiontype.parts
    }

fun ActionType.toRest() =
    when (this) {
        ActionType.READ -> ActionRest.Actiontype.read
        ActionType.RUN -> ActionRest.Actiontype.run
        ActionType.LEND -> ActionRest.Actiontype.lend
        ActionType.DOWNLOAD -> ActionRest.Actiontype.download
        ActionType.PRINT -> ActionRest.Actiontype.print
        ActionType.REPRODUCE -> ActionRest.Actiontype.reproduce
        ActionType.MODIFY -> ActionRest.Actiontype.modify
        ActionType.REUSE -> ActionRest.Actiontype.reuse
        ActionType.DISTRIBUTE -> ActionRest.Actiontype.distribute
        ActionType.PUBLISH -> ActionRest.Actiontype.publish
        ActionType.ARCHIVE -> ActionRest.Actiontype.archive
        ActionType.INDEX -> ActionRest.Actiontype.index
        ActionType.MOVE -> ActionRest.Actiontype.move
        ActionType.DISPLAY_METADATA -> ActionRest.Actiontype.displaymetadata
    }
