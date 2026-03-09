package de.zbw.business.lori.server.type

import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_AUTHOR
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_BAND
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_COLLECTION_HANDLE
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_COLLECTION_NAME
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_COMMUNITY_HANDLE
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_COMMUNITY_NAME
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_CREATED_BY
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_CREATED_ON
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_DELETED
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_DOI
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_HANDLE
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_ISBN
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_ISSN
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_IS_PART_OF_SERIES
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_LAST_UPDATED_BY
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_LAST_UPDATED_ON
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_LICENCE_URL
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_LICENCE_URL_FILTER
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_PAKET_SIGEL
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_PPN
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_PUBLICATION_TYPE
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_PUBLICATION_YEAR
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_STORAGE_DATE
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_SUBCOMMUNITY_HANDLE
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_SUBCOMMUNITY_NAME
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_TITLE
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_TITLE_JOURNAL
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_TITLE_SERIES
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_ZDB_IDS
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.OffsetDateTime

data class Item(
    val metadata: ItemMetadata,
    val rights: List<ItemRight>,
)

data class ItemId(
    val handle: String,
    val rightId: String,
)

@Serializable
data class ItemMetadata(
    val author: String?,
    val band: String?,
    val collectionHandle: String?,
    val collectionName: String?,
    val communityHandle: String?,
    val communityName: String?,
    val createdBy: String?,
    @Serializable(with = OffsetDateTimeSerializer::class)
    val createdOn: OffsetDateTime?,
    val deleted: Boolean,
    val doi: List<String>?,
    val econbizId: String?,
    val handle: String,
    val isbn: List<String>?,
    val issn: String?,
    val isPartOfSeries: List<String>?,
    val lastUpdatedBy: String?,
    @Serializable(with = OffsetDateTimeSerializer::class)
    val lastUpdatedOn: OffsetDateTime?,
    val licenceUrl: String?,
    val licenceUrlFilter: String?,
    val paketSigel: List<String>?,
    val ppn: String?,
    val publicationType: PublicationType,
    val publicationYear: Int?,
    val subCommunityHandle: String?,
    val subCommunityName: String?,
    @Serializable(with = OffsetDateTimeSerializer::class)
    val storageDate: OffsetDateTime?,
    val title: String,
    val titleJournal: String?,
    val titleSeries: String?,
    val zdbIds: List<String>?,
) {
    fun toCSV(): String {
        fun listToString(list: List<String>?): String? = list?.joinToString(";")

        fun dateToString(date: OffsetDateTime?): String? = date?.toString()

        val fields =
            listOf(
                author,
                band,
                collectionHandle,
                collectionName,
                communityHandle,
                communityName,
                createdBy,
                dateToString(createdOn),
                deleted.toString(),
                listToString(doi),
                handle,
                listToString(isbn),
                issn,
                listToString(isPartOfSeries),
                lastUpdatedBy,
                dateToString(lastUpdatedOn),
                licenceUrl,
                licenceUrlFilter,
                listToString(paketSigel),
                ppn,
                publicationType.name,
                publicationYear?.toString(),
                subCommunityHandle,
                subCommunityName,
                dateToString(storageDate),
                title,
                titleJournal,
                titleSeries,
                listToString(zdbIds),
            )

        return fields.joinToString(",") { csvEscape(it) }
    }

    private fun csvEscape(value: String?): String {
        if (value == null) return ""
        val needsQuoting =
            value.contains(',') ||
                value.contains('"') ||
                value.contains('\n') ||
                value.contains('\r')
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuoting) "\"$escaped\"" else escaped
    }

    fun toJson(): String = JSON_ENCODER.encodeToString(this)

    companion object {
        private val JSON_ENCODER =
            Json {
                prettyPrint = false
                encodeDefaults = true
            }

        fun csvFileHeader(): String =
            "$COLUMN_METADATA_AUTHOR, " +
                "$COLUMN_METADATA_BAND," +
                "$COLUMN_METADATA_COLLECTION_HANDLE," +
                "$COLUMN_METADATA_COLLECTION_NAME," +
                "$COLUMN_METADATA_COMMUNITY_HANDLE," +
                "$COLUMN_METADATA_COMMUNITY_NAME," +
                "$COLUMN_METADATA_CREATED_BY," +
                "$COLUMN_METADATA_CREATED_ON," +
                "$COLUMN_METADATA_DELETED," +
                "$COLUMN_METADATA_DOI," +
                "$COLUMN_METADATA_HANDLE," +
                "$COLUMN_METADATA_ISBN," +
                "$COLUMN_METADATA_ISSN," +
                "$COLUMN_METADATA_IS_PART_OF_SERIES," +
                "$COLUMN_METADATA_LAST_UPDATED_BY," +
                "$COLUMN_METADATA_LAST_UPDATED_ON," +
                "$COLUMN_METADATA_LICENCE_URL," +
                "$COLUMN_METADATA_LICENCE_URL_FILTER," +
                "$COLUMN_METADATA_PAKET_SIGEL," +
                "$COLUMN_METADATA_PPN," +
                "$COLUMN_METADATA_PUBLICATION_TYPE," +
                "$COLUMN_METADATA_PUBLICATION_YEAR," +
                "$COLUMN_METADATA_SUBCOMMUNITY_HANDLE," +
                "$COLUMN_METADATA_SUBCOMMUNITY_NAME," +
                "$COLUMN_METADATA_STORAGE_DATE," +
                "$COLUMN_METADATA_TITLE," +
                "$COLUMN_METADATA_TITLE_JOURNAL," +
                "$COLUMN_METADATA_TITLE_SERIES," +
                COLUMN_METADATA_ZDB_IDS
    }
}

enum class AccessState(
    val priority: Int,
) {
    OPEN(1),
    RESTRICTED(2),
    CLOSED(3),
}

enum class PublicationType(
    val priority: Int,
) {
    ARTICLE(1),
    BOOK(2),
    BOOK_PART(3),
    CONFERENCE_PAPER(4),
    OTHER(5),
    PERIODICAL_PART(6),
    PROCEEDING(7),
    RESEARCH_REPORT(8),
    THESIS(9),
    WORKING_PAPER(10),
}

enum class BasisStorage {
    AUTHOR_RIGHT_EXCEPTION,
    LICENCE_CONTRACT,
    OPEN_CONTENT_LICENCE,
    USER_AGREEMENT,
    ZBW_POLICY_RESTRICTED,
    ZBW_POLICY_UNANSWERED,
}

enum class BasisAccessState {
    AUTHOR_RIGHT_EXCEPTION,
    LICENCE_CONTRACT,
    OPEN_CONTENT_LICENCE,
    USER_AGREEMENT,
    ZBW_POLICY,
}

data class ItemRight(
    val rightId: String? = null,
    val accessState: AccessState? = null,
    val basisAccessState: BasisAccessState? = null,
    val basisStorage: BasisStorage? = null,
    val createdBy: String? = null,
    val createdOn: OffsetDateTime? = null,
    val endDate: LocalDate? = null,
    val exceptionOfId: String? = null,
    val hasExceptionId: String? = null,
    val firstAppliedOn: OffsetDateTime? = null,
    val groupIds: List<Int>? = null,
    val groups: List<Group>? = null,
    val isTemplate: Boolean,
    val lastAppliedOn: OffsetDateTime? = null,
    val lastUpdatedBy: String? = null,
    val lastUpdatedOn: OffsetDateTime? = null,
    val licenceContract: String? = null,
    val hasLegalRisk: Boolean? = null,
    val notesGeneral: String? = null,
    val notesFormalRules: String? = null,
    val notesProcessDocumentation: String? = null,
    val notesManagementRelated: String? = null,
    val predecessorId: String? = null,
    val restrictedOpenContentLicence: Boolean? = null,
    val startDate: LocalDate,
    val successorId: String? = null,
    val templateDescription: String? = null,
    val templateName: String? = null,
    val zbwUserAgreement: Boolean? = null,
) {
    fun validate(): List<String> =
        mutableListOf<String>().also { errors ->
            if (groupIds?.isNotEmpty() ?: false && accessState != AccessState.RESTRICTED) {
                errors.add("AccessState must be RESTRICTED if groups are defined")
            }

            if (endDate != null && startDate.isAfter(endDate)) {
                errors.add("Start date must be before end date")
            }
            return errors
        }
}

data class ItemRow(
    val rightId: String,
    val handle: String,
    val createdBy: String?,
    val createdOn: OffsetDateTime?,
    val lastUpdatedBy: String?,
    val lastUpdatedOn: OffsetDateTime?,
)

enum class FormalRule {
    CC_LICENCE_NO_RESTRICTION,
    COPYRIGHT_EXCEPTION_RISKFREE,
    LICENCE_CONTRACT,
    ZBW_USER_AGREEMENT,
}
