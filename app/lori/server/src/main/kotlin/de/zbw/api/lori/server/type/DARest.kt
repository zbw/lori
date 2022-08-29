package de.zbw.api.lori.server.type

/**
 * Types that represent JSON objects of DSpace v.5.x REST API.
 *
 * Created on 02-10-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
@kotlinx.serialization.Serializable
data class DAObject(
    val id: Int,
    val name: String,
    val handle: String?,
    val type: String?,
    val link: String,
    val expand: List<String>,
)

@kotlinx.serialization.Serializable
data class DAItem(
    val id: Int,
    val name: String?,
    val handle: String?,
    val type: String?,
    val link: String,
    val expand: List<String>,
    val lastModified: String?,
    val parentCollection: DACollection?,
    val parentCollectionList: List<DACollection>,
    val parentCommunityList: List<DACommunity>,
    val metadata: List<DAMetadata>,
    val bitstreams: List<DABitstream>,
    val archived: String?,
    val withdrawn: String?,
)

@kotlinx.serialization.Serializable
data class DACollection(
    val id: Int,
    val name: String,
    val handle: String?,
    val type: String?,
    val link: String,
    val expand: List<String>,
    val logo: DABitstream?,
    val parentCommunity: String?,
    val parentCommunityList: List<String>,
    val items: List<DAItem>,
    val license: String?,
    val copyrightText: String?,
    val introductoryText: String?,
    val shortDescription: String?,
    val sidebarText: String?,
    val numberItems: Int?,
)

@kotlinx.serialization.Serializable
data class DACommunity(
    val id: Int,
    val name: String,
    val handle: String?,
    val type: String?,
    val countItems: Int?,
    val link: String,
    val expand: List<String>,
    val logo: DABitstream?,
    val parentCommunity: DACommunity?,
    val copyrightText: String?,
    val introductoryText: String?,
    val shortDescription: String?,
    val sidebarText: String?,
    val subcommunities: List<DACollection>,
    val collections: List<DACollection>,
)

@kotlinx.serialization.Serializable
data class DAMetadata(
    val key: String,
    val value: String,
    val language: String?,
)

@kotlinx.serialization.Serializable
data class DABitstream(
    val id: Int?,
    val name: String?,
    val handle: String?,
    val type: String?,
    val link: String,
    val expand: List<String>,
    val bundleName: String?,
    val description: String?,
    val format: String?,
    val mimeType: String?,
    val sizeBytes: Int?,
    val parentObject: DAObject?,
    val retrieveLink: String?,
    val checkSum: DAChecksum?,
    val sequenceId: Int?,
    val policies: DAResourcePolicy?,
)

@kotlinx.serialization.Serializable
data class DAResourcePolicy(
    val action: String?,
    val endDate: String?,
    val epersonId: Int?,
    val groupid: Int?,
    val id: Int,
    val resourceId: Int?,
    val resourceTypeId: String?,
    val rpDescription: String?,
    val rpName: String?,
    val rpType: String?,
    val startDate: String?,
)

@kotlinx.serialization.Serializable
data class DAChecksum(
    val value: String,
    val checkSumAlgorithm: String,
)

@kotlinx.serialization.Serializable
data class DACredentials(
    val email: String,
    val password: String,
)
