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
                            .setIfNotNull(it.metadata.id) { b, value -> b.setId(value) }
                            .setIfNotNull(it.metadata.access_state) { b, value -> b.setAccessState(value.toProto()) }
                            .setIfNotNull(it.metadata.band) { b, value -> b.setBand(value) }
                            .setIfNotNull(it.metadata.doi) { b, value -> b.setDoi(value) }
                            .setHandle(it.metadata.handle)
                            .setIfNotNull(it.metadata.isbn) { b, value -> b.setIsbn(value) }
                            .setIfNotNull(it.metadata.issn) { b, value -> b.setIssn(value) }
                            .setIfNotNull(it.metadata.paket_sigel) { b, value -> b.setPaketSigel(value) }
                            .setIfNotNull(it.metadata.ppn) { b, value -> b.setPpn(value) }
                            .setIfNotNull(it.metadata.ppn_ebook) { b, value -> b.setPpnEbook(value) }
                            .setPublicationType(it.metadata.publicationType.toProto())
                            .setPublicationYear(it.metadata.publicationYear)
                            .setIfNotNull(it.metadata.rights_k10plus) { b, value -> b.setRightsK10Plus(value) }
                            .setIfNotNull(it.metadata.serialNumber) { b, value -> b.setSerialNumber(value) }
                            .setTitle(it.metadata.title)
                            .setIfNotNull(it.metadata.title_journal) { b, value -> b.setTitleJournal(value) }
                            .setIfNotNull(it.metadata.title_series) { b, value -> b.setTitleSeries(value) }
                            .setIfNotNull(it.metadata.zbd_id) { b, value -> b.setZbdId(value) }
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
