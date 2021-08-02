package de.zbw.api.access.server

import de.zbw.access.api.AccessRightProto
import de.zbw.access.api.AccessServiceGrpcKt
import de.zbw.access.api.ActionProto
import de.zbw.access.api.AddAccessInformationRequest
import de.zbw.access.api.AddAccessInformationResponse
import de.zbw.access.api.AttributeProto
import de.zbw.access.api.GetAccessInformationRequest
import de.zbw.access.api.GetAccessInformationResponse
import de.zbw.access.api.RestrictionProto
import de.zbw.api.access.server.config.AccessConfiguration
import de.zbw.api.access.server.type.toBusiness
import de.zbw.api.access.server.type.toProto
import de.zbw.business.access.server.AccessServerBackend
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
) : AccessServiceGrpcKt.AccessServiceCoroutineImplBase() {

    override suspend fun addAccessInformation(request: AddAccessInformationRequest): AddAccessInformationResponse {
        try {
            request.itemsList.map { it.toBusiness() }.map { backend.insertAccessRightEntry(it) }
        } catch (e: Exception) {
            throw StatusRuntimeException(
                Status.INTERNAL.withCause(e.cause)
                    .withDescription("Error while inserting an access right item: ${e.message}")
            )
        }

        return AddAccessInformationResponse
            .newBuilder()
            .build()
    }

    override suspend fun getAccessInformation(request: GetAccessInformationRequest): GetAccessInformationResponse {
        try {
            val accessRights = backend.getAccessRightEntries(request.idsList)

            return GetAccessInformationResponse.newBuilder()
                .addAllAccessRights(
                    accessRights.map {
                        AccessRightProto.newBuilder()
                            .setIfNotNull(it.header.id) { b, value -> b.setId(value) }
                            .setIfNotNull(it.header.tenant) { b, value -> b.setTenant(value) }
                            .setIfNotNull(it.header.usageGuide) { b, value -> b.setUsageGuide(value) }
                            .setIfNotNull(it.header.template) { b, value -> b.setTemplate(value) }
                            .setIfNotNull(it.header.mention) { b, value -> b.setMention(value) }
                            .setIfNotNull(it.header.shareAlike) { b, value -> b.setSharealike(value) }
                            .setIfNotNull(it.header.commercialUse) { b, value -> b.setCommercialuse(value) }
                            .setIfNotNull(it.header.copyright) { b, value -> b.setCopyright(value) }
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

private fun <T> AccessRightProto.Builder.setIfNotNull(
    value: T?,
    setter: (AccessRightProto.Builder, T) -> AccessRightProto.Builder,
): AccessRightProto.Builder {
    return if (value != null) {
        setter(this, value)
    } else this
}
