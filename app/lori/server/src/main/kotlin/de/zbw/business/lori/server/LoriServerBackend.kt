package de.zbw.business.lori.server

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import de.zbw.api.lori.server.config.LoriConfiguration
import de.zbw.api.lori.server.exception.ResourceStillInUseException
import de.zbw.business.lori.server.type.Bookmark
import de.zbw.business.lori.server.type.BookmarkTemplate
import de.zbw.business.lori.server.type.Group
import de.zbw.business.lori.server.type.Item
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.SearchQueryResult
import de.zbw.business.lori.server.type.Template
import de.zbw.business.lori.server.type.User
import de.zbw.business.lori.server.type.UserRole
import de.zbw.lori.model.UserRest
import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.FacetTransientSet
import de.zbw.persistence.lori.server.TemplateRightIdCreated
import io.ktor.server.auth.jwt.JWTPrincipal
import io.opentelemetry.api.trace.Tracer
import org.apache.logging.log4j.util.Strings
import java.security.MessageDigest
import java.util.Date

/**
 * Backend for the Lori-Server.
 *
 * Created on 07-15-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class LoriServerBackend(
    internal val dbConnector: DatabaseConnector,
    private val config: LoriConfiguration,
) {
    constructor(
        config: LoriConfiguration,
        tracer: Tracer,
    ) : this(
        DatabaseConnector(
            config,
            tracer,
        ),
        config
    )

    fun insertRightForMetadataIds(
        right: ItemRight,
        metadataIds: List<String>,
    ): String {
        val pkRight = dbConnector.rightDB.insertRight(right)
        metadataIds.forEach {
            dbConnector.itemDB.insertItem(it, pkRight)
        }
        return pkRight
    }

    fun insertItemEntry(metadataId: String, rightId: String) = dbConnector.itemDB.insertItem(metadataId, rightId)

    fun insertMetadataElements(metadataElems: List<ItemMetadata>): List<String> =
        metadataElems.map { insertMetadataElement(it) }

    fun insertGroup(group: Group): String = dbConnector.groupDB.insertGroup(group)

    fun insertMetadataElement(metadata: ItemMetadata): String =
        dbConnector.metadataDB.insertMetadata(metadata)

    fun insertRight(right: ItemRight): String {
        val generatedRightId = dbConnector.rightDB.insertRight(right)
        right.groupIds?.forEach { gId ->
            dbConnector.groupDB.insertGroupRightPair(
                rightId = generatedRightId,
                groupId = gId,
            )
        }
        return generatedRightId
    }

    fun updateGroup(group: Group): Int = dbConnector.groupDB.updateGroup(group)

    fun upsertRight(right: ItemRight): Int {
        val rightId = right.rightId!!
        val oldGroupIds = dbConnector.groupDB.getGroupsByRightId(rightId).toSet()
        val newGroupIds = right.groupIds?.toSet() ?: emptySet()

        val toAdd = newGroupIds.subtract(oldGroupIds)
        toAdd.forEach { gId ->
            dbConnector.groupDB.insertGroupRightPair(
                rightId = rightId,
                groupId = gId,
            )
        }

        val toRemove = oldGroupIds.subtract(newGroupIds)
        toRemove.forEach { gId ->
            dbConnector.groupDB.deleteGroupPair(
                rightId = rightId,
                groupId = gId,
            )
        }

        return dbConnector.rightDB.upsertRight(right)
    }

    fun upsertMetadataElements(metadataElems: List<ItemMetadata>): IntArray =
        dbConnector.metadataDB.upsertMetadataBatch(metadataElems.map { it })

    fun upsertMetaData(metadata: List<ItemMetadata>): IntArray = dbConnector.metadataDB.upsertMetadataBatch(metadata)

    fun getMetadataList(limit: Int, offset: Int): List<ItemMetadata> =
        dbConnector.metadataDB.getMetadataRange(limit, offset).takeIf {
            it.isNotEmpty()
        }?.let { metadataList ->
            metadataList.sortedBy { it.metadataId }
        } ?: emptyList()

    fun getMetadataElementsByIds(metadataIds: List<String>): List<ItemMetadata> =
        dbConnector.metadataDB.getMetadata(metadataIds)

    fun metadataContainsId(id: String): Boolean = dbConnector.metadataDB.metadataContainsId(id)

    fun rightContainsId(rightId: String): Boolean = dbConnector.rightDB.rightContainsId(rightId)

    fun getItemByMetadataId(metadataId: String): Item? =
        dbConnector.metadataDB.getMetadata(listOf(metadataId)).takeIf { it.isNotEmpty() }
            ?.first()
            ?.let { meta ->
                val rights = dbConnector.rightDB.getRightIdsByMetadata(metadataId).let {
                    dbConnector.rightDB.getRightsByIds(it)
                }
                Item(
                    meta,
                    rights,
                )
            }

    fun getGroupById(groupId: String): Group? = dbConnector.groupDB.getGroupById(groupId)

    fun getGroupList(
        limit: Int,
        offset: Int,
    ): List<Group> =
        dbConnector.groupDB.getGroupList(limit, offset)

    fun getGroupListIdsOnly(
        limit: Int,
        offset: Int,
    ): List<Group> =
        dbConnector.groupDB.getGroupListIdsOnly(limit, offset)
            .map {
                Group(
                    name = it,
                    entries = emptyList(),
                    description = null,
                )
            }

    fun getRightsByIds(rightIds: List<String>): List<ItemRight> {
        return dbConnector.rightDB.getRightsByIds(rightIds)
    }

    fun getItemList(
        limit: Int,
        offset: Int,
    ): List<Item> {
        val receivedMetadata = dbConnector.metadataDB.getMetadataRange(limit, offset)
        return receivedMetadata
            .takeIf {
                it.isNotEmpty()
            }?.let { metadataList ->
                getRightsForMetadata(metadataList)
            } ?: emptyList()
    }

    private fun getRightsForMetadata(
        metadataList: List<ItemMetadata>,
    ): List<Item> {
        val metadataToRights = metadataList.map { metadata ->
            metadata to dbConnector.rightDB.getRightIdsByMetadata(metadata.metadataId)
        }
        return metadataToRights.map { p ->
            Item(
                p.first,
                dbConnector.rightDB.getRightsByIds(p.second)
            )
        }
    }

    fun itemContainsRight(rightId: String): Boolean = dbConnector.itemDB.itemContainsRight(rightId)

    fun itemContainsMetadata(metadataId: String): Boolean = dbConnector.metadataDB.itemContainsMetadata(metadataId)

    fun itemContainsEntry(metadataId: String, rightId: String): Boolean =
        dbConnector.itemDB.itemContainsEntry(metadataId, rightId)

    fun countMetadataEntries(): Int =
        dbConnector.searchDB.countSearchMetadata(
            emptyMap(),
            emptyList(),
            emptyList(),
            null,
        )

    fun countItemByRightId(rightId: String) = dbConnector.itemDB.countItemByRightId(rightId)

    fun deleteItemEntry(metadataId: String, rightId: String) = dbConnector.itemDB.deleteItem(metadataId, rightId)

    fun deleteItemEntriesByMetadataId(metadataId: String) = dbConnector.itemDB.deleteItemByMetadata(metadataId)

    fun deleteItemEntriesByRightId(rightId: String) = dbConnector.itemDB.deleteItemByRight(rightId)

    fun deleteGroup(groupId: String): Int {
        val receivedRights: List<String> = dbConnector.groupDB.getRightsByGroupId(groupId)
        return if (receivedRights.isEmpty()) {
            dbConnector.groupDB.deleteGroupById(groupId)
        } else {
            throw ResourceStillInUseException(
                "Gruppe wird noch von folgenden Rechte-Ids verwendet: " + receivedRights.joinToString(separator = ",")
            )
        }
    }

    fun deleteMetadata(metadataId: String): Int = dbConnector.metadataDB.deleteMetadata(listOf(metadataId))

    fun deleteRight(rightId: String): Int {
        dbConnector.groupDB.deleteGroupPairsByRightId(rightId)
        return dbConnector.rightDB.deleteRightsByIds(listOf(rightId))
    }

    fun getRightEntriesByMetadataId(metadataId: String): List<ItemRight> =
        dbConnector.rightDB.getRightIdsByMetadata(metadataId).let {
            dbConnector.rightDB.getRightsByIds(it)
        }

    fun userContainsName(name: String): Boolean = dbConnector.userDB.userTableContainsName(name)

    fun insertNewUser(user: UserRest): String =
        dbConnector.userDB.insertUser(
            User(
                name = user.username,
                passwordHash = hashString("SHA-256", user.password),
                role = UserRole.READONLY,
            )
        )

    fun checkCredentials(user: UserRest): Boolean =
        dbConnector.userDB.userExistsByNameAndPassword(
            user.username,
            hashString("SHA-256", user.password),
        )

    fun getCurrentUserRole(username: String): UserRole? =
        dbConnector.userDB.getRoleByUsername(username)

    fun updateUserNonRoleProperties(user: UserRest): Int =
        dbConnector.userDB.updateUserNonRoleProperties(
            User(
                name = user.username,
                passwordHash = hashString("SHA-256", user.password),
                role = null,
            )
        )

    fun updateUserRoleProperty(username: String, role: UserRole): Int =
        dbConnector.userDB.updateUserRoleProperty(username, role)

    fun deleteUser(username: String): Int =
        dbConnector.userDB.deleteUser(username)

    fun generateJWT(username: String): String = JWT.create()
        .withAudience(config.jwtAudience)
        .withIssuer(config.jwtIssuer)
        .withClaim("username", username)
        .withExpiresAt(Date(System.currentTimeMillis() + 60000))
        .sign(Algorithm.HMAC256(config.jwtSecret))

    fun searchQuery(
        searchTerm: String?,
        limit: Int,
        offset: Int,
        metadataSearchFilter: List<MetadataSearchFilter> = emptyList(),
        rightSearchFilter: List<RightSearchFilter> = emptyList(),
        noRightInformationFilter: NoRightInformationFilter? = null,
    ): SearchQueryResult {
        val keys = searchTerm
            ?.let { parseValidSearchKeys(it) }
            ?: emptyMap()

        val invalidSearchKeys = searchTerm
            ?.let { parseInvalidSearchKeys(it) }
            ?: emptyList()

        val hasSearchTokenWithNoKey = searchTerm
            ?.takeIf {
                searchTerm.isNotEmpty()
            }?.let {
                hasSearchTokensWithNoKey(it)
            } ?: false

        // Acquire search results
        val receivedMetadata: List<ItemMetadata> =
            dbConnector.searchDB.searchMetadata(
                keys,
                limit,
                offset,
                metadataSearchFilter,
                rightSearchFilter.takeIf { noRightInformationFilter == null } ?: emptyList(),
                noRightInformationFilter,
            )

        // Combine Metadata entries with their rights
        val items: List<Item> =
            receivedMetadata.takeIf {
                it.isNotEmpty()
            }?.let { metadata ->
                getRightsForMetadata(metadata)
            } ?: (emptyList())

        // Acquire number of results
        val numberOfResults =
            items
                .takeIf { it.isNotEmpty() }
                ?.let {
                    dbConnector.searchDB.countSearchMetadata(
                        keys,
                        metadataSearchFilter,
                        rightSearchFilter.takeIf { noRightInformationFilter == null } ?: emptyList(),
                        noRightInformationFilter,
                    )
                }
                ?: 0

        // Collect all publication types, zdbIds and paketSigels
        val facets: FacetTransientSet = dbConnector.searchDB.searchForFacets(
            keys,
            metadataSearchFilter,
            rightSearchFilter,
            noRightInformationFilter,
        )
        return SearchQueryResult(
            numberOfResults = numberOfResults,
            results = items,
            accessState = facets.accessState,
            hasLicenceContract = facets.hasLicenceContract,
            hasOpenContentLicence = facets.hasOpenContentLicence,
            hasSearchTokenWithNoKey = hasSearchTokenWithNoKey,
            hasZbwUserAgreement = facets.hasZbwUserAgreement,
            invalidSearchKey = invalidSearchKeys,
            paketSigels = facets.paketSigels,
            publicationType = facets.publicationType,
            zdbIds = facets.zdbIds,
        )
    }

    fun insertBookmark(bookmark: Bookmark): Int =
        dbConnector.bookmarkDB.insertBookmark(bookmark)

    fun deleteBookmark(bookmarkId: Int): Int {
        val receivedTemplateIds = dbConnector.templateDB.getTemplateIdsByBookmarkId(bookmarkId)
        return if (receivedTemplateIds.isEmpty()) {
            dbConnector.bookmarkDB.deleteBookmarkById(bookmarkId)
        } else {
            throw ResourceStillInUseException(
                "Bookmark wird noch von folgenden Template-Ids verwendet: " + receivedTemplateIds.joinToString(separator = ",")
            )
        }
    }

    fun updateBookmark(bookmarkId: Int, bookmark: Bookmark): Int =
        dbConnector.bookmarkDB.updateBookmarkById(bookmarkId, bookmark)

    fun getBookmarkById(bookmarkId: Int): Bookmark? =
        dbConnector.bookmarkDB.getBookmarksByIds(listOf(bookmarkId)).firstOrNull()

    fun getBookmarkList(
        limit: Int,
        offset: Int,
    ): List<Bookmark> =
        dbConnector.bookmarkDB.getBookmarkList(limit, offset)

    /**
     * Template list.
     */
    fun insertTemplate(template: Template): TemplateRightIdCreated =
        dbConnector.templateDB.insertTemplate(template)

    fun deleteTemplate(templateId: Int): Int = dbConnector.templateDB.deleteTemplateById(templateId)

    fun updateTemplate(templateId: Int, template: Template): Int =
        dbConnector.templateDB.updateTemplateById(templateId, template)

    fun getTemplateById(templateId: Int): Template? =
        dbConnector.templateDB.getTemplatesByIds(listOf(templateId)).firstOrNull()

    fun getTemplateList(
        limit: Int,
        offset: Int,
    ): List<Template> =
        dbConnector.templateDB.getTemplateList(limit, offset)

    /**
     * Template-Bookmark Pair.
     */
    fun getBookmarksByTemplateId(
        templateId: Int,
    ): List<Bookmark> {
        val bookmarkIds = dbConnector.templateDB.getBookmarkIdsByTemplateId(templateId)
        return dbConnector.bookmarkDB.getBookmarksByIds(bookmarkIds)
    }

    fun deleteBookmarkTemplatePair(
        templateId: Int,
        bookmarkId: Int,
    ): Int = dbConnector.templateDB.deleteTemplateBookmarkPair(
        BookmarkTemplate(
            bookmarkId = bookmarkId,
            templateId = templateId
        )
    )

    fun insertBookmarkTemplatePair(
        bookmarkId: Int,
        templateId: Int,
    ): Int = dbConnector.templateDB.insertTemplateBookmarkPair(
        BookmarkTemplate(
            bookmarkId = bookmarkId,
            templateId = templateId
        )
    )

    fun upsertBookmarkTemplatePairs(bookmarkTemplates: List<BookmarkTemplate>): List<BookmarkTemplate> =
        dbConnector.templateDB.upsertTemplateBookmarkBatch(bookmarkTemplates)

    fun deleteBookmarkTemplatePairs(bookmarkTemplates: List<BookmarkTemplate>): Int =
        bookmarkTemplates.sumOf {
            dbConnector.templateDB.deleteTemplateBookmarkPair(it)
        }

    fun deleteBookmarkTemplatePairsByTemplateId(templateId: Int): Int =
        dbConnector.templateDB.deletePairsByTemplateId(templateId)

    companion object {
        /**
         * Valid patterns: key:value or key:'value1 value2 ...'.
         * Valid special characters: '-:;'
         */
        private val SEARCH_KEY_REGEX = Regex("\\w+:[\\w-:;]+|\\w+:'[\\w\\s-:;]+'")

        fun isJWTExpired(principal: JWTPrincipal): Boolean {
            val expiresAt: Long? = principal
                .expiresAt
                ?.time
                ?.minus(System.currentTimeMillis())
            return expiresAt == null || expiresAt < 0
        }

        fun parseInvalidSearchKeys(s: String): List<String> =
            tokenizeSearchInput(s).mapNotNull {
                val keyname = it.substringBefore(":")
                if (SearchKey.toEnum(keyname) == null) {
                    keyname
                } else {
                    null
                }
            }

        fun parseValidSearchKeys(s: String?): Map<SearchKey, List<String>> =
            s?.let { tokenizeSearchInput(it) }?.mapNotNull {
                val key: SearchKey? = SearchKey.toEnum(it.substringBefore(":"))
                if (key == null) {
                    null
                } else {
                    key to it.substringAfter(":").trim().split("\\s+".toRegex())
                }
            }?.toMap() ?: emptyMap()

        fun searchKeysToString(keys: Map<SearchKey, List<String>>): String =
            keys.entries.joinToString(separator = " ") { e ->
                "${e.key.fromEnum()}:${e.value.joinToString(prefix = "'", postfix = "'", separator = " ")}"
            }

        private fun tokenizeSearchInput(s: String): List<String> {
            val iter = SEARCH_KEY_REGEX.findAll(s).iterator()
            return generateSequence {
                if (iter.hasNext()) {
                    iter.next().value.filter { it != '\'' }
                } else {
                    null
                }
            }.takeWhile { true }.toList()
        }

        fun hasSearchTokensWithNoKey(s: String): Boolean =
            s.takeIf {
                it.isNotEmpty()
            }?.let {
                val tmp = s.trim().replace(SEARCH_KEY_REGEX, Strings.EMPTY).trim().split("\\s+".toRegex())
                if (tmp.size == 1 && tmp[0].isEmpty()) {
                    return false
                } else {
                    true
                }
            } ?: false

        fun hashString(type: String, input: String): String {
            val bytes = MessageDigest
                .getInstance(type)
                .digest(input.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
