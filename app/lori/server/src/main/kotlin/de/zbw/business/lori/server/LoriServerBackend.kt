package de.zbw.business.lori.server

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import de.zbw.api.lori.server.config.LoriConfiguration
import de.zbw.lori.model.UserRest
import de.zbw.persistence.lori.server.DatabaseConnector
import io.ktor.server.auth.jwt.JWTPrincipal
import io.opentelemetry.api.trace.Tracer
import java.security.MessageDigest
import java.util.Date

/**
 * Backend for the Access-Server.
 *
 * Created on 07-15-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class LoriServerBackend(
    private val dbConnector: DatabaseConnector,
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
        val pkRight = dbConnector.insertRight(right)
        metadataIds.forEach {
            dbConnector.insertItem(it, pkRight)
        }
        return pkRight
    }

    fun insertItemEntry(metadataId: String, rightId: String) = dbConnector.insertItem(metadataId, rightId)

    fun insertMetadataElements(metadataElems: List<ItemMetadata>): List<String> =
        metadataElems.map { insertMetadataElement(it) }

    fun insertMetadataElement(metadata: ItemMetadata): String =
        dbConnector.insertMetadata(metadata)

    fun insertRight(right: ItemRight): String = dbConnector.insertRight(right)

    fun upsertRight(right: ItemRight): Int = dbConnector.upsertRight(right)

    fun upsertMetadataElements(metadataElems: List<ItemMetadata>): IntArray =
        dbConnector.upsertMetadataBatch(metadataElems.map { it })

    fun upsertMetaData(metadata: List<ItemMetadata>): IntArray = dbConnector.upsertMetadataBatch(metadata)

    fun getMetadataList(limit: Int, offset: Int): List<ItemMetadata> =
        dbConnector.getMetadataRange(limit, offset).takeIf {
            it.isNotEmpty()
        }?.let { metadataList -> metadataList.sortedBy { it.metadataId }
        } ?: emptyList()

    fun getMetadataElementsByIds(metadataIds: List<String>): List<ItemMetadata> = dbConnector.getMetadata(metadataIds)

    fun metadataContainsId(id: String): Boolean = dbConnector.metadataContainsId(id)

    fun rightContainsId(rightId: String): Boolean = dbConnector.rightContainsId(rightId)

    fun getRightsByIds(rightIds: List<String>): List<ItemRight> = dbConnector.getRights(rightIds)

    fun getItemByMetadataId(metadataId: String): Item? =
        dbConnector.getMetadata(listOf(metadataId)).takeIf { it.isNotEmpty() }
            ?.first()
            ?.let { meta ->
                val rights = dbConnector.getRightIdsByMetadata(metadataId).let {
                    dbConnector.getRights(it)
                }
                Item(
                    meta,
                    rights,
                )
            }

    fun getItemList(limit: Int, offset: Int): List<Item> {
        return dbConnector.getMetadataRange(limit, offset).takeIf {
            it.isNotEmpty()
        }?.let { metadataList ->
            val metadataToRights = metadataList.map { metadata ->
                metadata to dbConnector.getRightIdsByMetadata(metadata.metadataId)
            }
            metadataToRights.map { p ->
                Item(
                    p.first,
                    dbConnector.getRights(p.second)
                )
            }
        } ?: emptyList()
    }

    fun itemContainsRight(rightId: String): Boolean = dbConnector.itemContainsRight(rightId)

    fun itemContainsMetadata(metadataId: String): Boolean = dbConnector.itemContainsMetadata(metadataId)

    fun itemContainsEntry(metadataId: String, rightId: String): Boolean =
        dbConnector.itemContainsEntry(metadataId, rightId)

    fun countMetadataEntries(): Int = dbConnector.countMetadataEntries()

    fun countItemByRightId(rightId: String) = dbConnector.countItemByRightId(rightId)

    fun deleteItemEntry(metadataId: String, rightId: String) = dbConnector.deleteItem(metadataId, rightId)

    fun deleteItemEntriesByMetadataId(metadataId: String) = dbConnector.deleteItemByMetadata(metadataId)

    fun deleteItemEntriesByRightId(rightId: String) = dbConnector.deleteItemByRight(rightId)

    fun deleteMetadata(metadataId: String): Int = dbConnector.deleteMetadata(listOf(metadataId))

    fun deleteRight(rightId: String): Int = dbConnector.deleteRights(listOf(rightId))

    fun getRightEntriesByMetadataId(metadataId: String): List<ItemRight> =
        dbConnector.getRightIdsByMetadata(metadataId).let {
            dbConnector.getRights(it)
        }

    fun userContainsName(name: String): Boolean = dbConnector.userTableContainsName(name)

    fun insertNewUser(user: UserRest): String =
        dbConnector.insertUser(
            User(
                name = user.username,
                passwordHash = hashString("SHA-256", user.password),
                role = UserRole.READONLY,
            )
        )

    fun checkCredentials(user: UserRest): Boolean =
        dbConnector.userExistsByNameAndPassword(
            user.username,
            hashString("SHA-256", user.password),
        )

    fun getCurrentUserRole(username: String): UserRole? =
        dbConnector.getRoleByUsername(username)

    fun updateUserNonRoleProperties(user: UserRest): Int =
        dbConnector.updateUserNonRoleProperties(
            User(
                name = user.username,
                passwordHash = hashString("SHA-256", user.password),
                role = null,
            )
        )

    fun updateUserRoleProperty(username: String, role: UserRole): Int =
        dbConnector.updateUserRoleProperty(username, role)

    fun deleteUser(username: String): Int =
        dbConnector.deleteUser(username)

    fun generateJWT(username: String): String = JWT.create()
        .withAudience(config.jwtAudience)
        .withIssuer(config.jwtIssuer)
        .withClaim("username", username)
        .withExpiresAt(Date(System.currentTimeMillis() + 60000))
        .sign(Algorithm.HMAC256(config.jwtSecret))

    fun isExpired(principal: JWTPrincipal): Boolean {
        val expiresAt: Long? = principal
            .expiresAt
            ?.time
            ?.minus(System.currentTimeMillis())
        return expiresAt == null || expiresAt < 0
    }

    internal fun hashString(type: String, input: String): String {
        val bytes = MessageDigest
            .getInstance(type)
            .digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
