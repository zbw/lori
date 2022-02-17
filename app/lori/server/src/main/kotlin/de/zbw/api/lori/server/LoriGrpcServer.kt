package de.zbw.api.lori.server

import de.zbw.api.lori.server.config.LoriConfiguration
import de.zbw.api.lori.server.connector.DAConnector
import de.zbw.api.lori.server.type.DACommunity
import de.zbw.api.lori.server.type.toBusiness
import de.zbw.api.lori.server.type.toProto
import de.zbw.business.lori.server.Item
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.lori.api.ActionProto
import de.zbw.lori.api.AddItemRequest
import de.zbw.lori.api.AddItemResponse
import de.zbw.lori.api.AttributeProto
import de.zbw.lori.api.FullImportRequest
import de.zbw.lori.api.FullImportResponse
import de.zbw.lori.api.GetItemRequest
import de.zbw.lori.api.GetItemResponse
import de.zbw.lori.api.ItemProto
import de.zbw.lori.api.LoriServiceGrpcKt
import de.zbw.lori.api.RestrictionProto
import io.grpc.Status
import io.grpc.StatusRuntimeException
import java.sql.SQLException

/**
 * Access GRPC-server.
 *
 * Created on 07-12-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class LoriGrpcServer(
    config: LoriConfiguration,
    private val backend: LoriServerBackend = LoriServerBackend(config),
    private val daConnector: DAConnector = DAConnector(config, backend),
) : LoriServiceGrpcKt.LoriServiceCoroutineImplBase() {

    override suspend fun fullImport(request: FullImportRequest): FullImportResponse {
        try {
            val token = daConnector.login()
            val community: DACommunity = daConnector.getCommunity(token)
            val imports = daConnector.startFullImport(token, community.collections.map { it.id })
            return FullImportResponse
                .newBuilder()
                .setItemsImported(imports.sum())
                .build()
        } catch (e: Exception) {
            throw StatusRuntimeException(
                Status.INTERNAL.withCause(e.cause)
                    .withDescription("Following error occurred: ${e.message}\nStacktrace: ${e.stackTraceToString()}")
            )
        }
    }

    override suspend fun addItem(request: AddItemRequest): AddItemResponse {
        try {
            request.itemsList.map { it.toBusiness() }.map { backend.insertAccessRightEntry(it) }
        } catch (e: Exception) {
            throw StatusRuntimeException(
                Status.INTERNAL.withCause(e.cause)
                    .withDescription("Error while inserting an access right item: ${e.message}\nStacktrace: ${e.stackTraceToString()}")
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
                    .withDescription("Error while querying header information: ${e.message}\\nStacktrace: ${e.stackTraceToString()}")
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
