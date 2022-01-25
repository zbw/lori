package de.zbw.api.access.server

import de.zbw.access.api.ActionProto
import de.zbw.access.api.AddItemRequest
import de.zbw.access.api.AddItemResponse
import de.zbw.access.api.AttributeProto
import de.zbw.access.api.GetItemRequest
import de.zbw.access.api.GetItemResponse
import de.zbw.access.api.ItemProto
import de.zbw.access.api.LoriServiceGrpcKt
import de.zbw.access.api.RestrictionProto
import de.zbw.api.access.server.config.AccessConfiguration
import de.zbw.api.access.server.type.toBusiness
import de.zbw.api.access.server.type.toProto
import de.zbw.business.access.server.AccessServerBackend
import de.zbw.business.access.server.Item
import io.grpc.Status
import io.grpc.StatusRuntimeException
import java.sql.SQLException

/**
 * Access GRPC-server.
 *
 * Created on 07-12-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class AccessGrpcServer(
    config: AccessConfiguration,
    private val backend: AccessServerBackend = AccessServerBackend(config),
) : LoriServiceGrpcKt.LoriServiceCoroutineImplBase() {

    override suspend fun addItem(request: AddItemRequest): AddItemResponse {
        try {
            request.itemsList.map { it.toBusiness() }.map { backend.insertAccessRightEntry(it) }
        } catch (e: Exception) {
            throw StatusRuntimeException(
                Status.INTERNAL.withCause(e.cause)
                    .withDescription("Error while inserting an access right item: ${e.message}")
            )
        }

        return AddItemResponse
            .newBuilder()
            .build()
    }

    override suspend fun getItem(request: GetItemRequest): GetItemResponse {
        try {
            val accessRights: List<Item> = backend.getAccessRightEntries(request.idsList)

            return GetItemResponse.newBuilder()
                .addAllAccessRights(
                    accessRights.map {
                        ItemProto.newBuilder()
                            .setIfNotNull(it.itemMetadata.id) { b, value -> b.setId(value) }
                            .setIfNotNull(it.itemMetadata.accessState) { b, value -> b.setAccessState(value.toProto()) }
                            .setIfNotNull(it.itemMetadata.band) { b, value -> b.setBand(value) }
                            .setIfNotNull(it.itemMetadata.doi) { b, value -> b.setDoi(value) }
                            .setHandle(it.itemMetadata.handle)
                            .setIfNotNull(it.itemMetadata.isbn) { b, value -> b.setIsbn(value) }
                            .setIfNotNull(it.itemMetadata.issn) { b, value -> b.setIssn(value) }
                            .setIfNotNull(it.itemMetadata.paketSigel) { b, value -> b.setPaketSigel(value) }
                            .setIfNotNull(it.itemMetadata.ppn) { b, value -> b.setPpn(value) }
                            .setIfNotNull(it.itemMetadata.ppnEbook) { b, value -> b.setPpnEbook(value) }
                            .setPublicationType(it.itemMetadata.publicationType.toProto())
                            .setPublicationYear(it.itemMetadata.publicationYear)
                            .setIfNotNull(it.itemMetadata.rightsK10plus) { b, value -> b.setRightsK10Plus(value) }
                            .setIfNotNull(it.itemMetadata.serialNumber) { b, value -> b.setSerialNumber(value) }
                            .setTitle(it.itemMetadata.title)
                            .setIfNotNull(it.itemMetadata.titleJournal) { b, value -> b.setTitleJournal(value) }
                            .setIfNotNull(it.itemMetadata.titleSeries) { b, value -> b.setTitleSeries(value) }
                            .setIfNotNull(it.itemMetadata.zbdId) { b, value -> b.setZbdId(value) }
                            .addAllActions(
                                it.actions.map { action ->
                                    ActionProto
                                        .newBuilder()
                                        .setPermission(action.permission)
                                        .setType(action.type.toProto())
                                        .addAllRestrictions(
                                            action.restrictions.map { restriction ->
                                                RestrictionProto.newBuilder()
                                                    .setType(restriction.type.toProto())
                                                    .setAttribute(
                                                        AttributeProto.newBuilder()
                                                            .setType(restriction.attribute.type.toProto())
                                                            .addAllValues(restriction.attribute.values)
                                                            .build()
                                                    )
                                                    .build()
                                            }
                                        )
                                        .build()
                                }

                            )
                            .build()
                    }
                )
                .build()
        } catch (e: SQLException) {
            throw StatusRuntimeException(
                Status.INTERNAL.withCause(e.cause)
                    .withDescription("Error while querying header information: ${e.message}")
            )
        }
    }
}

private fun <T> ItemProto.Builder.setIfNotNull(
    value: T?,
    setter: (ItemProto.Builder, T) -> ItemProto.Builder,
): ItemProto.Builder {
    return if (value != null) {
        setter(this, value)
    } else this
}
