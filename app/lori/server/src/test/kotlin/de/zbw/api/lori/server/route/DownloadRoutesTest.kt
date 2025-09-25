package de.zbw.api.lori.server.route

import de.zbw.api.lori.server.route.RightRoutesKtTest.Companion.getServicePool
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.type.ExportFormat
import de.zbw.business.lori.server.type.ExportJob
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.util.UUID

class DownloadRoutesTest {
    @Test
    fun testGetDownloadCSV() {
        val uuid = UUID.randomUUID()
        val fakeFile =
            mockk<File> {
                every { name } returns "test.csv"
            }

        val exampleCSVInput = "hello,world"
        val expectedExportJob =
            mockk<ExportJob> {
                every { getFile() } returns fakeFile
                every { getInputStream() } returns ByteArrayInputStream(exampleCSVInput.toByteArray())
                every { format } returns ExportFormat.CSV
                every { id } returns uuid
            }
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery {
                    getExportJobById(any())
                } returns expectedExportJob
            }
        val servicePool =
            getServicePool(
                backend,
            )

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response: HttpResponse =
                client.get("/api/v1/download/$uuid") {
                    header(HttpHeaders.Accept, ContentType.Text.CSV)
                    header(HttpHeaders.ContentType, ContentType.Text.CSV.toString())
                }
            assertThat(
                HttpStatusCode.OK,
                `is`(response.status),
            )
            assertThat(
                exampleCSVInput,
                `is`(response.bodyAsText()),
            )
            assertThat(
                "attachment; filename=test.csv",
                `is`(response.headers[HttpHeaders.ContentDisposition]),
            )
            assertThat(
                ContentType.Text.CSV,
                `is`(response.contentType()),
            )
        }
    }

    @Test
    fun testGetDownloadJSON() {
        val uuid = UUID.randomUUID()
        val fakeFile =
            mockk<File> {
                every { name } returns "test.json"
            }

        val exampleJSONInput = "[hello,world]"
        val expectedExportJob =
            mockk<ExportJob> {
                every { getFile() } returns fakeFile
                every { getInputStream() } returns ByteArrayInputStream(exampleJSONInput.toByteArray())
                every { format } returns ExportFormat.JSON
                every { id } returns uuid
            }
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery {
                    getExportJobById(any())
                } returns expectedExportJob
            }
        val servicePool =
            getServicePool(
                backend,
            )

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response: HttpResponse =
                client.get("/api/v1/download/$uuid") {
                    header(HttpHeaders.Accept, ContentType.Application.Json)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
            assertThat(
                HttpStatusCode.OK,
                `is`(response.status),
            )
            assertThat(
                exampleJSONInput,
                `is`(response.bodyAsText()),
            )
            assertThat(
                "attachment; filename=test.json",
                `is`(response.headers[HttpHeaders.ContentDisposition]),
            )
            assertThat(
                ContentType.Application.Json,
                `is`(response.contentType()),
            )
        }
    }

    @Test
    fun testGetDownlowNotFound() {
        val uuid = UUID.randomUUID()
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery {
                    getExportJobById(any())
                } returns null
            }
        val servicePool =
            getServicePool(
                backend,
            )

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response: HttpResponse =
                client.get("/api/v1/download/$uuid") {
                    header(HttpHeaders.Accept, ContentType.Application.Json)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
            assertThat(
                HttpStatusCode.NotFound,
                `is`(response.status),
            )
        }
    }
}
