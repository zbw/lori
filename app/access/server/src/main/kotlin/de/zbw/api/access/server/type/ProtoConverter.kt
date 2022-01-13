package de.zbw.api.access.server.type

import de.zbw.access.api.AccessStateProto
import de.zbw.access.api.ActionProto
import de.zbw.access.api.ActionTypeProto
import de.zbw.access.api.AttributeProto
import de.zbw.access.api.AttributeTypeProto
import de.zbw.access.api.ItemProto
import de.zbw.access.api.PublicationTypeProto
import de.zbw.access.api.RestrictionProto
import de.zbw.access.api.RestrictionTypeProto
import de.zbw.business.access.server.AccessState
import de.zbw.business.access.server.Action
import de.zbw.business.access.server.ActionType
import de.zbw.business.access.server.Attribute
import de.zbw.business.access.server.AttributeType
import de.zbw.business.access.server.Item
import de.zbw.business.access.server.PublicationType
import de.zbw.business.access.server.Restriction
import de.zbw.business.access.server.RestrictionType
import io.grpc.Status
import io.grpc.StatusRuntimeException

/**
 * Conversion functions between protobuf and business logic.
 *
 * Created on 07-12-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
fun ItemProto.toBusiness(): Item =
    Item(
        metadata = de.zbw.business.access.server.Metadata(
            id = id,
            access_state = this.returnIfFieldIsSet(ItemProto.ACCESS_STATE_FIELD_NUMBER)?.accessState?.toBusiness(),
            band = this.returnIfFieldIsSet(ItemProto.BAND_FIELD_NUMBER)?.band,
            doi = this.returnIfFieldIsSet(ItemProto.DOI_FIELD_NUMBER)?.doi,
            handle = this.handle,
            isbn = this.returnIfFieldIsSet(ItemProto.ISBN_FIELD_NUMBER)?.isbn,
            issn = this.returnIfFieldIsSet(ItemProto.ISSN_FIELD_NUMBER)?.issn,
            paket_sigel = this.returnIfFieldIsSet(ItemProto.PAKET_SIGEL_FIELD_NUMBER)?.paketSigel,
            ppn = this.returnIfFieldIsSet(ItemProto.PPN_FIELD_NUMBER)?.ppn,
            ppn_ebook = this.returnIfFieldIsSet(ItemProto.PPN_EBOOK_FIELD_NUMBER)?.ppnEbook,
            publicationType = this.publicationType.toBusiness(),
            publicationYear = this.publicationYear,
            rights_k10plus = this.returnIfFieldIsSet(ItemProto.RIGHTS_K10PLUS_FIELD_NUMBER)?.rightsK10Plus,
            serialNumber = this.returnIfFieldIsSet(ItemProto.SERIAL_NUMBER_FIELD_NUMBER)?.serialNumber,
            title = title,
            title_journal = this.returnIfFieldIsSet(ItemProto.TITLE_JOURNAL_FIELD_NUMBER)?.titleJournal,
            title_series = this.returnIfFieldIsSet(ItemProto.TITLE_SERIES_FIELD_NUMBER)?.titleSeries,
            zbd_id = this.returnIfFieldIsSet(ItemProto.ZBD_ID_FIELD_NUMBER)?.zbdId,
        ),
        this.actionsList.map { it.toBusiness() },
    )

private fun ItemProto.returnIfFieldIsSet(fieldNumber: Int) = this.takeIf {
    it.hasField(
        ItemProto.getDescriptor().findFieldByNumber(fieldNumber)
    )
}

fun AccessState.toProto(): AccessStateProto =
    when (this) {
        AccessState.CLOSED -> AccessStateProto.ACCESS_STATE_PROTO_CLOSED
        AccessState.OPEN -> AccessStateProto.ACCESS_STATE_PROTO_OPEN
        AccessState.RESTRICTED -> AccessStateProto.ACCESS_STATE_PROTO_RESTRICTED
    }

fun AccessStateProto.toBusiness(): AccessState? =
    when (this) {
        AccessStateProto.ACCESS_STATE_PROTO_CLOSED -> AccessState.CLOSED
        AccessStateProto.ACCESS_STATE_PROTO_OPEN -> AccessState.OPEN
        AccessStateProto.ACCESS_STATE_PROTO_RESTRICTED -> AccessState.RESTRICTED
        else -> null
    }

fun PublicationType.toProto(): PublicationTypeProto =
    when (this) {
        PublicationType.MONO -> PublicationTypeProto.PUBLICATION_TYPE_PROTO_MONO
        PublicationType.PERIODICAL -> PublicationTypeProto.PUBLICATION_TYPE_PROTO_PERIODICAL
    }

fun PublicationTypeProto.toBusiness(): PublicationType =
    when (this) {
        PublicationTypeProto.PUBLICATION_TYPE_PROTO_MONO -> PublicationType.MONO
        PublicationTypeProto.PUBLICATION_TYPE_PROTO_PERIODICAL -> PublicationType.PERIODICAL
        else -> throw IllegalArgumentException("PublicationType has to be set.")
    }

private fun ActionProto.toBusiness(): Action =
    Action(
        type = this.type.toBusiness(),
        permission = this.permission,
        restrictions = this.restrictionsList.map { it.toBusiness() },
    )

private fun RestrictionProto.toBusiness(): Restriction =
    Restriction(
        type = this.type.toBusiness(),
        attribute = this.attribute.toBusiness()
    )

private fun AttributeProto.toBusiness(): Attribute =
    Attribute(
        type = this.type.toBusiness(),
        values = this.valuesList,
    )

internal fun ActionTypeProto.toBusiness(): ActionType =
    when (this) {
        ActionTypeProto.ACTION_TYPE_PROTO_READ -> ActionType.READ
        ActionTypeProto.ACTION_TYPE_PROTO_RUN -> ActionType.RUN
        ActionTypeProto.ACTION_TYPE_PROTO_LEND -> ActionType.LEND
        ActionTypeProto.ACTION_TYPE_PROTO_DOWNLOAD -> ActionType.DOWNLOAD
        ActionTypeProto.ACTION_TYPE_PROTO_PRINT -> ActionType.PRINT
        ActionTypeProto.ACTION_TYPE_PROTO_REPRODUCE -> ActionType.REPRODUCE
        ActionTypeProto.ACTION_TYPE_PROTO_MODIFY -> ActionType.MODIFY
        ActionTypeProto.ACTION_TYPE_PROTO_REUSE -> ActionType.REUSE
        ActionTypeProto.ACTION_TYPE_PROTO_DISTRIBUTE -> ActionType.DISTRIBUTE
        ActionTypeProto.ACTION_TYPE_PROTO_PUBLISH -> ActionType.PUBLISH
        ActionTypeProto.ACTION_TYPE_PROTO_ARCHIVE -> ActionType.ARCHIVE
        ActionTypeProto.ACTION_TYPE_PROTO_INDEX -> ActionType.INDEX
        ActionTypeProto.ACTION_TYPE_PROTO_MOVE -> ActionType.MOVE
        ActionTypeProto.ACTION_TYPE_PROTO_DISPLAY_METADATA -> ActionType.DISPLAY_METADATA
        else -> throw StatusRuntimeException(
            Status.INVALID_ARGUMENT.withDescription("Action type is not specified.")
        )
    }

fun ActionType.toProto(): ActionTypeProto =
    when (this) {
        ActionType.READ -> ActionTypeProto.ACTION_TYPE_PROTO_READ
        ActionType.RUN -> ActionTypeProto.ACTION_TYPE_PROTO_RUN
        ActionType.LEND -> ActionTypeProto.ACTION_TYPE_PROTO_LEND
        ActionType.DOWNLOAD -> ActionTypeProto.ACTION_TYPE_PROTO_DOWNLOAD
        ActionType.PRINT -> ActionTypeProto.ACTION_TYPE_PROTO_PRINT
        ActionType.REPRODUCE -> ActionTypeProto.ACTION_TYPE_PROTO_REPRODUCE
        ActionType.MODIFY -> ActionTypeProto.ACTION_TYPE_PROTO_MODIFY
        ActionType.REUSE -> ActionTypeProto.ACTION_TYPE_PROTO_REUSE
        ActionType.DISTRIBUTE -> ActionTypeProto.ACTION_TYPE_PROTO_DISTRIBUTE
        ActionType.PUBLISH -> ActionTypeProto.ACTION_TYPE_PROTO_PUBLISH
        ActionType.ARCHIVE -> ActionTypeProto.ACTION_TYPE_PROTO_ARCHIVE
        ActionType.INDEX -> ActionTypeProto.ACTION_TYPE_PROTO_INDEX
        ActionType.MOVE -> ActionTypeProto.ACTION_TYPE_PROTO_MOVE
        ActionType.DISPLAY_METADATA -> ActionTypeProto.ACTION_TYPE_PROTO_DISPLAY_METADATA
    }

internal fun RestrictionTypeProto.toBusiness(): RestrictionType =
    when (this) {
        RestrictionTypeProto.RESTRICTION_TYPE_PROTO_GROUP -> RestrictionType.GROUP
        RestrictionTypeProto.RESTRICTION_TYPE_PROTO_AGE -> RestrictionType.AGE
        RestrictionTypeProto.RESTRICTION_TYPE_PROTO_LOCATION -> RestrictionType.LOCATION
        RestrictionTypeProto.RESTRICTION_TYPE_PROTO_DATE -> RestrictionType.DATE
        RestrictionTypeProto.RESTRICTION_TYPE_PROTO_DURATION -> RestrictionType.DURATION
        RestrictionTypeProto.RESTRICTION_TYPE_PROTO_COUNT -> RestrictionType.COUNT
        RestrictionTypeProto.RESTRICTION_TYPE_PROTO_CONCURRENT -> RestrictionType.CONCURRENT
        RestrictionTypeProto.RESTRICTION_TYPE_PROTO_WATERMARK -> RestrictionType.WATERMARK
        RestrictionTypeProto.RESTRICTION_TYPE_PROTO_QUALITY -> RestrictionType.QUALITY
        RestrictionTypeProto.RESTRICTION_TYPE_PROTO_AGREEMENT -> RestrictionType.AGREEMENT
        RestrictionTypeProto.RESTRICTION_TYPE_PROTO_PARTS -> RestrictionType.PARTS
        else -> throw StatusRuntimeException(
            Status.INVALID_ARGUMENT.withDescription("Restriction type is not specified.")
        )
    }

fun RestrictionType.toProto(): RestrictionTypeProto =
    when (this) {
        RestrictionType.GROUP -> RestrictionTypeProto.RESTRICTION_TYPE_PROTO_GROUP
        RestrictionType.AGE -> RestrictionTypeProto.RESTRICTION_TYPE_PROTO_AGE
        RestrictionType.LOCATION -> RestrictionTypeProto.RESTRICTION_TYPE_PROTO_LOCATION
        RestrictionType.DATE -> RestrictionTypeProto.RESTRICTION_TYPE_PROTO_DATE
        RestrictionType.DURATION -> RestrictionTypeProto.RESTRICTION_TYPE_PROTO_DURATION
        RestrictionType.COUNT -> RestrictionTypeProto.RESTRICTION_TYPE_PROTO_COUNT
        RestrictionType.CONCURRENT -> RestrictionTypeProto.RESTRICTION_TYPE_PROTO_CONCURRENT
        RestrictionType.WATERMARK -> RestrictionTypeProto.RESTRICTION_TYPE_PROTO_WATERMARK
        RestrictionType.QUALITY -> RestrictionTypeProto.RESTRICTION_TYPE_PROTO_QUALITY
        RestrictionType.AGREEMENT -> RestrictionTypeProto.RESTRICTION_TYPE_PROTO_AGREEMENT
        RestrictionType.PARTS -> RestrictionTypeProto.RESTRICTION_TYPE_PROTO_PARTS
    }

internal fun AttributeTypeProto.toBusiness(): AttributeType =
    when (this) {
        AttributeTypeProto.ATTRIBUTE_TYPE_PROTO_FROM_DATE -> AttributeType.FROM_DATE
        AttributeTypeProto.ATTRIBUTE_TYPE_PROTO_TO_DATE -> AttributeType.TO_DATE
        AttributeTypeProto.ATTRIBUTE_TYPE_PROTO_MAX_RESOLUTION -> AttributeType.MAX_RESOLUTION
        AttributeTypeProto.ATTRIBUTE_TYPE_PROTO_MAX_BITRATE -> AttributeType.MAX_BITRATE
        AttributeTypeProto.ATTRIBUTE_TYPE_PROTO_COUNT -> AttributeType.COUNT
        AttributeTypeProto.ATTRIBUTE_TYPE_PROTO_INSIDE -> AttributeType.INSIDE
        AttributeTypeProto.ATTRIBUTE_TYPE_PROTO_SUBNET -> AttributeType.SUBNET
        AttributeTypeProto.ATTRIBUTE_TYPE_PROTO_OUTSIDE -> AttributeType.OUTSIDE
        AttributeTypeProto.ATTRIBUTE_TYPE_PROTO_WATERMARK -> AttributeType.WATERMARK
        AttributeTypeProto.ATTRIBUTE_TYPE_PROTO_DURATION -> AttributeType.DURATION
        AttributeTypeProto.ATTRIBUTE_TYPE_PROTO_MIN_AGE -> AttributeType.MIN_AGE
        AttributeTypeProto.ATTRIBUTE_TYPE_PROTO_MAX_AGE -> AttributeType.MAX_AGE
        AttributeTypeProto.ATTRIBUTE_TYPE_PROTO_REQUIRED -> AttributeType.REQUIRED
        AttributeTypeProto.ATTRIBUTE_TYPE_PROTO_GROUPS -> AttributeType.GROUPS
        AttributeTypeProto.ATTRIBUTE_TYPE_PROTO_PARTS -> AttributeType.PARTS
        AttributeTypeProto.ATTRIBUTE_TYPE_PROTO_SESSIONS -> AttributeType.SESSIONS
        else -> throw StatusRuntimeException(
            Status.INVALID_ARGUMENT.withDescription("Attribute type is not specified.")
        )
    }

fun AttributeType.toProto(): AttributeTypeProto =
    when (this) {
        AttributeType.FROM_DATE -> AttributeTypeProto.ATTRIBUTE_TYPE_PROTO_FROM_DATE
        AttributeType.TO_DATE -> AttributeTypeProto.ATTRIBUTE_TYPE_PROTO_TO_DATE
        AttributeType.MAX_RESOLUTION -> AttributeTypeProto.ATTRIBUTE_TYPE_PROTO_MAX_RESOLUTION
        AttributeType.MAX_BITRATE -> AttributeTypeProto.ATTRIBUTE_TYPE_PROTO_MAX_BITRATE
        AttributeType.COUNT -> AttributeTypeProto.ATTRIBUTE_TYPE_PROTO_COUNT
        AttributeType.INSIDE -> AttributeTypeProto.ATTRIBUTE_TYPE_PROTO_INSIDE
        AttributeType.SUBNET -> AttributeTypeProto.ATTRIBUTE_TYPE_PROTO_SUBNET
        AttributeType.OUTSIDE -> AttributeTypeProto.ATTRIBUTE_TYPE_PROTO_OUTSIDE
        AttributeType.WATERMARK -> AttributeTypeProto.ATTRIBUTE_TYPE_PROTO_WATERMARK
        AttributeType.DURATION -> AttributeTypeProto.ATTRIBUTE_TYPE_PROTO_DURATION
        AttributeType.MIN_AGE -> AttributeTypeProto.ATTRIBUTE_TYPE_PROTO_MIN_AGE
        AttributeType.MAX_AGE -> AttributeTypeProto.ATTRIBUTE_TYPE_PROTO_MAX_AGE
        AttributeType.REQUIRED -> AttributeTypeProto.ATTRIBUTE_TYPE_PROTO_REQUIRED
        AttributeType.GROUPS -> AttributeTypeProto.ATTRIBUTE_TYPE_PROTO_GROUPS
        AttributeType.PARTS -> AttributeTypeProto.ATTRIBUTE_TYPE_PROTO_PARTS
        AttributeType.SESSIONS -> AttributeTypeProto.ATTRIBUTE_TYPE_PROTO_SESSIONS
    }
