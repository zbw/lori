package de.zbw.api.lori.server.route

import com.google.gson.reflect.TypeToken
import de.zbw.api.lori.server.route.ItemRoutesKtTest.Companion.GSON
import de.zbw.api.lori.server.route.ItemRoutesKtTest.Companion.TEST_RIGHT
import de.zbw.api.lori.server.route.ItemRoutesKtTest.Companion.getServicePool
import de.zbw.api.lori.server.route.ItemRoutesKtTest.Companion.jsonAsString
import de.zbw.api.lori.server.type.toRest
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.type.Group
import de.zbw.business.lori.server.type.GroupEntry
import de.zbw.lori.model.GroupRest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.postgresql.util.PSQLException
import org.testng.annotations.Test
import java.lang.reflect.Type
import java.sql.SQLException

/**
 * Testing [GroupRoutes].
 *
 * Created on 11-14-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class GroupRoutesKtTest {
    @Test
    fun testGetGroupByIdOK() {
        // given
        val groupId = "someId"
        val expected = TEST_GROUP.toRest()
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { getGroupById(groupId) } returns TEST_GROUP
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.get("/api/v1/group/$groupId")
            val content: String = response.bodyAsText()
            val groupListType: Type = object : TypeToken<GroupRest>() {}.type
            val received: GroupRest = GSON.fromJson(content, groupListType)
            assertThat(received, `is`(expected))
        }
    }

    @Test
    fun testGetGroupByIdNotFound() {
        // given
        val groupId = "someId"
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { getGroupById(groupId) } returns null
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.get("/api/v1/group/$groupId")
            assertThat(response.status, `is`(HttpStatusCode.NotFound))
        }
    }

    @Test
    fun testGetGroupByIdInternalError() {
        // given
        val groupId = "someId"
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { getGroupById(groupId) } throws SQLException()
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.get("/api/v1/group/$groupId")
            assertThat(response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    @Test
    fun testDeleteByGroupIdOK() {
        // given
        val groupId = "someId"
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { deleteGroup(groupId) } returns 1
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.delete("/api/v1/group/$groupId")
            assertThat(response.status, `is`(HttpStatusCode.OK))
        }
    }

    @Test
    fun testDeleteByGroupIdNotFound() {
        // given
        val groupId = "someId"
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { deleteGroup(groupId) } returns 0
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.delete("/api/v1/group/$groupId")
            assertThat(response.status, `is`(HttpStatusCode.NotFound))
        }
    }

    @Test
    fun testDeleteByGroupIdInternalError() {
        // given
        val groupId = "someId"
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { deleteGroup(groupId) } throws SQLException()
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.delete("/api/v1/group/$groupId")
            assertThat(response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    @Test
    fun testPostGroupCreated() {
        // given
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { insertGroup(any()) } returns "foobar"
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.post("/api/v1/group") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_GROUP.toRest()))
            }
            assertThat("Should return 201", response.status, `is`(HttpStatusCode.Created))
        }
    }

    @Test
    fun testPostGroupBadRequest() {
        // given
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { insertGroup(any()) } returns "foobar"
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.post("/api/v1/group") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_RIGHT))
            }
            assertThat("Should return 400", response.status, `is`(HttpStatusCode.BadRequest))
        }
    }

    @Test
    fun testPostGroupConflict() {
        // given
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { insertGroup(any()) } throws mockk<PSQLException> {
                every { sqlState } returns "23505"
                every { message } returns "error"
            }
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.post("/api/v1/group") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_GROUP.toRest()))
            }
            assertThat("Should return 409", response.status, `is`(HttpStatusCode.Conflict))
        }
    }

    @Test
    fun testPostGroupIAE() {
        // given
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { insertGroup(any()) } throws IllegalArgumentException()
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.post("/api/v1/group") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_GROUP.toRest()))
            }
            assertThat("Should return 400", response.status, `is`(HttpStatusCode.BadRequest))
        }
    }

    @Test
    fun testPostGroupInternalError() {
        // given
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { insertGroup(any()) } throws SQLException()
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.post("/api/v1/group") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_GROUP.toRest()))
            }
            assertThat("Should return 500", response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    @Test
    fun testPutGroupNoContent() {
        // given
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { updateGroup(any()) } returns 1
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.put("/api/v1/group") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_GROUP.toRest()))
            }
            assertThat("Should return 204", response.status, `is`(HttpStatusCode.NoContent))
        }
    }

    @Test
    fun testPutGroupBadRequest() {
        // given
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { updateGroup(any()) } returns 1
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.put("/api/v1/group") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_RIGHT))
            }
            assertThat("Should return 400", response.status, `is`(HttpStatusCode.BadRequest))
        }
    }

    @Test
    fun testPutGroupNotFound() {
        // given
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { updateGroup(any()) } returns 0
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.put("/api/v1/group") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_GROUP.toRest()))
            }
            assertThat("Should return 404", response.status, `is`(HttpStatusCode.NotFound))
        }
    }

    @Test
    fun testPutGroupInternalError() {
        // given
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { updateGroup(any()) } throws SQLException()
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.put("/api/v1/group") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_GROUP.toRest()))
            }
            assertThat("Should return 500", response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    @Test
    fun testGetGroupListOK() {
        // given
        val limit = 50
        val offset = 0
        val expected = listOf(TEST_GROUP.toRest())
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { getGroupList(limit, offset) } returns listOf(TEST_GROUP)
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.get("/api/v1/group/list?limit=$limit&offset=$offset")
            val content: String = response.bodyAsText()
            val groupListType: Type = object : TypeToken<ArrayList<GroupRest>>() {}.type
            val received: ArrayList<GroupRest> = GSON.fromJson(content, groupListType)
            assertThat(received, `is`(expected))
        }
    }

    @Test
    fun testGetGroupListBadRequest() {
        // given
        val limit = 0
        val offset = 0
        listOf(TEST_GROUP.toRest())
        val backend = mockk<LoriServerBackend>(relaxed = true) { }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.get("/api/v1/group/list?limit=$limit&offset=$offset")
            assertThat(response.status, `is`(HttpStatusCode.BadRequest))
        }
    }

    @Test
    fun testGetGroupListInternalError() {
        // given
        val limit = 5
        val offset = 0
        listOf(TEST_GROUP.toRest())
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { getGroupList(limit, offset) } throws SQLException()
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.get("/api/v1/group/list?limit=$limit&offset=$offset")
            assertThat(response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    companion object {
        val TEST_GROUP = Group(
            name = "name",
            description = "some description",
            entries = listOf(
                GroupEntry(
                    organisationName = "some organisation",
                    ipAddresses = "192.168.1.127",
                )
            )
        )
    }
}
