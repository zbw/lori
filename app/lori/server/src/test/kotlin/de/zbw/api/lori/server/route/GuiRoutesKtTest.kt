package de.zbw.api.lori.server.route

import com.google.gson.reflect.TypeToken
import de.zbw.api.lori.server.type.UserSession
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.type.Session
import de.zbw.business.lori.server.type.UserRole
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.Assert.assertTrue
import org.testng.annotations.AfterClass
import org.testng.annotations.Test
import java.lang.reflect.Type
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.assertNotNull

class GuiRoutesKtTest {
    @AfterClass
    fun afterTestActions() {
        unmockkAll()
    }

    @Test
    fun testPostCallbackFound() {
        mockkStatic(Instant::class)
        every { Instant.now() } returns VALID_TIME.toInstant()
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { insertSession(any()) } returns TEST_SESSION_ID
            every { getSessionById(any()) } returns Session(
                sessionID = "foobar",
                authenticated = true,
                firstName = "c@foobar.eu",
                lastName = null,
                role = UserRole.READONLY,
                validUntil = VALID_TIME.plusDays(1).toInstant()
            )
        }
        val servicePool = RightRoutesKtTest.getServicePool(backend)
        testApplication {
            application(
                servicePool.application()
            )
            val response: HttpResponse = client.post("/ui/callback-sso") {
                header(HttpHeaders.Accept, ContentType.Application.Any)
                header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded)
                setBody(TEST_MESSAGE)
            }
            val hasSetCookieHeader = response.headers["Set-Cookie"]?.contains("JSESSIONID") ?: false
            assertTrue(hasSetCookieHeader)
            assertThat(
                response.headers["Location"],
                `is`("/ui?login=success"),
            )
            assertThat("Should return 302", response.status, `is`(HttpStatusCode.Found))

            val responseGet = client.get("/api/v1/users/sessions") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.Cookie, "JSESSIONID=email%3D%2523sc%2540foobar.eu%26role%3D%2523sREADONLY%26sessionId%3D%2523s$TEST_SESSION_ID")
            }

            val content: String = responseGet.bodyAsText()
            val templateListType: Type = object : TypeToken<UserSession>() {}.type
            val received: UserSession = ItemRoutesKtTest.GSON.fromJson(content, templateListType)
            assertNotNull(received)
        }
    }

    @Test
    fun testPostCallbackUnauthorized() {
        mockkStatic(Instant::class)
        every { Instant.now() } returns INVALID_TIME.toInstant()
        val backend = mockk<LoriServerBackend>(relaxed = true)
        val servicePool = RightRoutesKtTest.getServicePool(backend)
        testApplication {
            application(
                servicePool.application()
            )
            val response: HttpResponse = client.post("/ui/callback-sso") {
                header(HttpHeaders.Accept, ContentType.Application.Any)
                header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded)
                setBody(TEST_MESSAGE)
            }
            assertThat("Should return 401", response.status, `is`(HttpStatusCode.Unauthorized))
        }
    }

    companion object {

        const val TEST_MESSAGE =
            "SAMLResponse=PHNhbWxwOlJlc3BvbnNlIHhtbG5zOnNhbWw9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDphc3NlcnRpb24iIHhtbG5zOnNhbWxwPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6cHJvdG9jb2wiIHhtbG5zOnhzPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxL1hNTFNjaGVtYSIgeG1sbnM6eHNpPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxL1hNTFNjaGVtYS1pbnN0YW5jZSIgeG1sbnM6ZHM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvMDkveG1sZHNpZyMiIHhtbG5zOm1kPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6bWV0YWRhdGEiIHhtbG5zOnhlbmM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvMDQveG1sZW5jIyIgeG1sbnM6eGVuYzExPSJodHRwOi8vd3d3LnczLm9yZy8yMDA5L3htbGVuYzExIyIgVmVyc2lvbj0iMi4wIiBJRD0iRFVPXzg2M2MwYjNjYWQ5Y2Q4Mzc5YzAzY2ViODhmZjAxOWNhNzY0MmIwNmMiIElzc3VlSW5zdGFudD0iMjAyMy0xMS0yMFQxMzoyMDoyOFoiIERlc3RpbmF0aW9uPSJodHRwOi8vZ3VpLmxvcmktZGV2Lnpidy1uZXR0Lnpidy1raWVsLmRlL3VpL2NhbGxiYWNrLXNzbyI%2BPHNhbWw6SXNzdWVyPmh0dHBzOi8vc3NvLTE3OTNiMTdjLnNzby5kdW9zZWN1cml0eS5jb20vc2FtbDIvc3AvRElNT0pUUFBMNkUwOTBYNEJVTDEvbWV0YWRhdGE8L3NhbWw6SXNzdWVyPjxkczpTaWduYXR1cmU%2BPGRzOlNpZ25lZEluZm8%2BPGRzOkNhbm9uaWNhbGl6YXRpb25NZXRob2QgQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxLzEwL3htbC1leGMtYzE0biMiLz48ZHM6U2lnbmF0dXJlTWV0aG9kIEFsZ29yaXRobT0iaHR0cDovL3d3dy53My5vcmcvMjAwMS8wNC94bWxkc2lnLW1vcmUjcnNhLXNoYTI1NiIvPjxkczpSZWZlcmVuY2UgVVJJPSIjRFVPXzg2M2MwYjNjYWQ5Y2Q4Mzc5YzAzY2ViODhmZjAxOWNhNzY0MmIwNmMiPjxkczpUcmFuc2Zvcm1zPjxkczpUcmFuc2Zvcm0gQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRzaWcjZW52ZWxvcGVkLXNpZ25hdHVyZSIvPjxkczpUcmFuc2Zvcm0gQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxLzEwL3htbC1leGMtYzE0biMiLz48L2RzOlRyYW5zZm9ybXM%2BPGRzOkRpZ2VzdE1ldGhvZCBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvMDQveG1sZW5jI3NoYTI1NiIvPjxkczpEaWdlc3RWYWx1ZT5VUFdITVhPZmJQQldsMDc3WEphQXpicy9IZmpQc0d5WVdSODBLaW96UkFvPTwvZHM6RGlnZXN0VmFsdWU%2BPC9kczpSZWZlcmVuY2U%2BPC9kczpTaWduZWRJbmZvPjxkczpTaWduYXR1cmVWYWx1ZT5TeE5BZHptMUM3QkJKSkJjcE9YbzZ1enl6YW1XRkxMT09FaHVkQU5mN0pObHdBUWdNc2QvTzM1eGIyc3JxbkEzMGJvZHFhMTQ5aXlzU283SFExSTdOREdOUk5ZRXI3cEwzSmRHOFVEdnE1dm5qekJyNWljR1RlY0N6UUJnS0VrMldyMzhaRmFncFNVeWdpVktyYkswcVNrUnhwSWdTeU9KTk9NS3hNRGlNZDFCaVc3bVJlQW0zaWp2Z0NkbWRIM01sWlA5ekFnc1BtQ3I0b3ZOMlVnMTB0Mjkyd3daU296TmNwZ3pkK0I0amdHMUtxME1PZ3h1RDdlNHBJQUVjbFliUklsR3NQbWU2ZWx6QkRCSkE3eUFWcnNRQ0FXR2FhTmlXQjhCM1lRVStrVExNQ290SjVFSWJuM0VXT2NzajY3eW1lNDd6ZFA2UGNlaHNHc1E1cjJMRXc9PTwvZHM6U2lnbmF0dXJlVmFsdWU%2BPGRzOktleUluZm8%2BPGRzOlg1MDlEYXRhPjxkczpYNTA5Q2VydGlmaWNhdGU%2BTUlJRERUQ0NBZldnQXdJQkFnSVVCbHZhQ3hURkxpQW03ZDc1UVlPZGg0QW4zTW93RFFZSktvWklodmNOQVFFTEJRQXdOakVWTUJNR0ExVUVDZ3dNUkhWdklGTmxZM1Z5YVhSNU1SMHdHd1lEVlFRRERCUkVTVTFQU2xSUVVFdzJSVEE1TUZnMFFsVk1NVEFlRncweU16RXhNRGN4TkRFNU16aGFGdzB6T0RBeE1Ua3dNekUwTURkYU1EWXhGVEFUQmdOVkJBb01ERVIxYnlCVFpXTjFjbWwwZVRFZE1Cc0dBMVVFQXd3VVJFbE5UMHBVVUZCTU5rVXdPVEJZTkVKVlRERXdnZ0VpTUEwR0NTcUdTSWIzRFFFQkFRVUFBNElCRHdBd2dnRUtBb0lCQVFDVndDa2YrS1RFWGRwQlo1dGNwZzRuM1VSbUFGM3FWV3ZicVhtRXQ2LytKNHFRZ3VBY1l5M2hGcHVhTlpzMjR3Z09hZ2d4c1dpWGNhM1MzVnE5cDUyV0hDL0s2eGFyUXl2OWpEemNTRVp2alJ2TVVMeTFlL3R3L1VkaWgxMlVSWnM2VER5Z3RFZmp0OFNUZ1VFbU5IY05McmorV3FkcFVvT0VSQlVaUnZWYkFNK0lGOXlUUUx2YXd5VXdNRzJhMjEvUXFtOTc2dXlHR3hNTnhDeGFtVlJnL2d1NlFvMzdwQjYrc0QrTlU4WjM0SU9HWlNlUTNFYmFXbVY3aVBVdGdSWVlkbXhCTGtFblpKRnVOUUI2ZFFhRno3Z0kvTXdEcUxkQlRLdEFJQmZ6S1RrYXJlUW5XNXlTVVFoZzZqenRXUXNCUXpRb2k3MWEzM2s5MUF6WjR5a3hBZ01CQUFHakV6QVJNQThHQTFVZEV3RUIvd1FGTUFNQkFmOHdEUVlKS29aSWh2Y05BUUVMQlFBRGdnRUJBSDhySXp0UlNyd2F5Ni9xMktFcDdSYjJJL1l2NlZXb3preEN1TU1PWDdRbkNaanJ3OURzaW81SHFTdkdLZ1NEZUZNYlE1L0pwY3pVZU9Pb0xWZ1BmbWppMTZ5d1k1ODJSZ2RxZ0ZabjdNcVhkMVUzRWF1TmtrSFdvTjlkTVV3ZGVYOTV6bjcyMWNYQ2g2R1NEaWl2eWtKSkVIVW9HSHd1YndEcFhZVW84Vlg0dVlRZzZlMUM0WTI1R2JTQ3E1ZXhPN2xXQzRhaGxtUWpyTW1pTWlpK0VHMmE1VmtmK1hNMXdDcE5vZnowd1UzUXZYNUltNFBubEhqQzVXbVdldHF3QTlUMWE3WUZIVDRnbnZOdG14Zm95UXBJRUpsanJsOWc5cm9mMnVBQXB3T3Z4SXNqdGlRMnlZb3FpWnY5Z2UrbmlkVHRMdjBmbG1senk2ZFRYdmJIczJjPTwvZHM6WDUwOUNlcnRpZmljYXRlPjwvZHM6WDUwOURhdGE%2BPC9kczpLZXlJbmZvPjwvZHM6U2lnbmF0dXJlPjxzYW1scDpTdGF0dXM%2BPHNhbWxwOlN0YXR1c0NvZGUgVmFsdWU9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDpzdGF0dXM6U3VjY2VzcyIvPjwvc2FtbHA6U3RhdHVzPjxzYW1sOkFzc2VydGlvbiBJRD0iRFVPXzkzYzQ5NjRiMTFhZDZmNmU4MTM3M2Y5NGQ2ODhhY2U2M2JhY2Y3ZGUiIElzc3VlSW5zdGFudD0iMjAyMy0xMS0yMFQxMzoyMDoyOFoiIFZlcnNpb249IjIuMCI%2BPHNhbWw6SXNzdWVyPmh0dHBzOi8vc3NvLTE3OTNiMTdjLnNzby5kdW9zZWN1cml0eS5jb20vc2FtbDIvc3AvRElNT0pUUFBMNkUwOTBYNEJVTDEvbWV0YWRhdGE8L3NhbWw6SXNzdWVyPjxkczpTaWduYXR1cmU%2BPGRzOlNpZ25lZEluZm8%2BPGRzOkNhbm9uaWNhbGl6YXRpb25NZXRob2QgQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxLzEwL3htbC1leGMtYzE0biMiLz48ZHM6U2lnbmF0dXJlTWV0aG9kIEFsZ29yaXRobT0iaHR0cDovL3d3dy53My5vcmcvMjAwMS8wNC94bWxkc2lnLW1vcmUjcnNhLXNoYTI1NiIvPjxkczpSZWZlcmVuY2UgVVJJPSIjRFVPXzkzYzQ5NjRiMTFhZDZmNmU4MTM3M2Y5NGQ2ODhhY2U2M2JhY2Y3ZGUiPjxkczpUcmFuc2Zvcm1zPjxkczpUcmFuc2Zvcm0gQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRzaWcjZW52ZWxvcGVkLXNpZ25hdHVyZSIvPjxkczpUcmFuc2Zvcm0gQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxLzEwL3htbC1leGMtYzE0biMiLz48L2RzOlRyYW5zZm9ybXM%2BPGRzOkRpZ2VzdE1ldGhvZCBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvMDQveG1sZW5jI3NoYTI1NiIvPjxkczpEaWdlc3RWYWx1ZT5RcTBGSHBFWVZTeXNaekJ0QnlpMzZZQjZHejA4MjNxUHhuZ0Mwd0JHYzMwPTwvZHM6RGlnZXN0VmFsdWU%2BPC9kczpSZWZlcmVuY2U%2BPC9kczpTaWduZWRJbmZvPjxkczpTaWduYXR1cmVWYWx1ZT5pUDJBUHpoTm9RTlJWM3ZoQjFjOVRxdVRVd1dsZEtpVTFrdm9NRkJEM2tkT1VjNXN0b2E2ZndrV3hOZzFDNmc4czRDeitUTllneWxyQ3hmSXVqWlIyL2o5QVF2bmlXQXhMOVpkd3E0dzFUekxRemZHOVZ6Y0RkdFVMMFFKTWpxSWcybEtxbGxLdXlTSFNMNnA2emdzdEI2ZnBTWmd2cWJJWkFibXdlUXRYZXY3U1BsaFhsK1VLOU5NVXg0dGFrbEdYMG9TdE12QTM2YUtpbW9TcXArb2ROYXQ3ZkRWRGVkT01lMmVZN01ObUxrVGU2cVV5SGpxd0FWMnhQWElzVUVBbmQ0VjcwTjhWTGQ0ZlJNWVh2M1FrZUEyWVhTWU9HZlhYZ2hTTGZDdHFHV0tkOWxEUHFCL04zVmFiQnZZV1lTZDJyUFYzL2I3MmRoVmFGQ2JzbjJXT3c9PTwvZHM6U2lnbmF0dXJlVmFsdWU%2BPGRzOktleUluZm8%2BPGRzOlg1MDlEYXRhPjxkczpYNTA5Q2VydGlmaWNhdGU%2BTUlJRERUQ0NBZldnQXdJQkFnSVVCbHZhQ3hURkxpQW03ZDc1UVlPZGg0QW4zTW93RFFZSktvWklodmNOQVFFTEJRQXdOakVWTUJNR0ExVUVDZ3dNUkhWdklGTmxZM1Z5YVhSNU1SMHdHd1lEVlFRRERCUkVTVTFQU2xSUVVFdzJSVEE1TUZnMFFsVk1NVEFlRncweU16RXhNRGN4TkRFNU16aGFGdzB6T0RBeE1Ua3dNekUwTURkYU1EWXhGVEFUQmdOVkJBb01ERVIxYnlCVFpXTjFjbWwwZVRFZE1Cc0dBMVVFQXd3VVJFbE5UMHBVVUZCTU5rVXdPVEJZTkVKVlRERXdnZ0VpTUEwR0NTcUdTSWIzRFFFQkFRVUFBNElCRHdBd2dnRUtBb0lCQVFDVndDa2YrS1RFWGRwQlo1dGNwZzRuM1VSbUFGM3FWV3ZicVhtRXQ2LytKNHFRZ3VBY1l5M2hGcHVhTlpzMjR3Z09hZ2d4c1dpWGNhM1MzVnE5cDUyV0hDL0s2eGFyUXl2OWpEemNTRVp2alJ2TVVMeTFlL3R3L1VkaWgxMlVSWnM2VER5Z3RFZmp0OFNUZ1VFbU5IY05McmorV3FkcFVvT0VSQlVaUnZWYkFNK0lGOXlUUUx2YXd5VXdNRzJhMjEvUXFtOTc2dXlHR3hNTnhDeGFtVlJnL2d1NlFvMzdwQjYrc0QrTlU4WjM0SU9HWlNlUTNFYmFXbVY3aVBVdGdSWVlkbXhCTGtFblpKRnVOUUI2ZFFhRno3Z0kvTXdEcUxkQlRLdEFJQmZ6S1RrYXJlUW5XNXlTVVFoZzZqenRXUXNCUXpRb2k3MWEzM2s5MUF6WjR5a3hBZ01CQUFHakV6QVJNQThHQTFVZEV3RUIvd1FGTUFNQkFmOHdEUVlKS29aSWh2Y05BUUVMQlFBRGdnRUJBSDhySXp0UlNyd2F5Ni9xMktFcDdSYjJJL1l2NlZXb3preEN1TU1PWDdRbkNaanJ3OURzaW81SHFTdkdLZ1NEZUZNYlE1L0pwY3pVZU9Pb0xWZ1BmbWppMTZ5d1k1ODJSZ2RxZ0ZabjdNcVhkMVUzRWF1TmtrSFdvTjlkTVV3ZGVYOTV6bjcyMWNYQ2g2R1NEaWl2eWtKSkVIVW9HSHd1YndEcFhZVW84Vlg0dVlRZzZlMUM0WTI1R2JTQ3E1ZXhPN2xXQzRhaGxtUWpyTW1pTWlpK0VHMmE1VmtmK1hNMXdDcE5vZnowd1UzUXZYNUltNFBubEhqQzVXbVdldHF3QTlUMWE3WUZIVDRnbnZOdG14Zm95UXBJRUpsanJsOWc5cm9mMnVBQXB3T3Z4SXNqdGlRMnlZb3FpWnY5Z2UrbmlkVHRMdjBmbG1senk2ZFRYdmJIczJjPTwvZHM6WDUwOUNlcnRpZmljYXRlPjwvZHM6WDUwOURhdGE%2BPC9kczpLZXlJbmZvPjwvZHM6U2lnbmF0dXJlPjxzYW1sOlN1YmplY3Q%2BPHNhbWw6TmFtZUlEIEZvcm1hdD0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6MS4xOm5hbWVpZC1mb3JtYXQ6ZW1haWxBZGRyZXNzIj5jLmJheUB6Ynctb25saW5lLmV1PC9zYW1sOk5hbWVJRD48c2FtbDpTdWJqZWN0Q29uZmlybWF0aW9uIE1ldGhvZD0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOmNtOmJlYXJlciI%2BPHNhbWw6U3ViamVjdENvbmZpcm1hdGlvbkRhdGEgTm90T25PckFmdGVyPSIyMDIzLTExLTIwVDEzOjI1OjI4WiIgUmVjaXBpZW50PSJodHRwOi8vZ3VpLmxvcmktZGV2Lnpidy1uZXR0Lnpidy1raWVsLmRlL3VpL2NhbGxiYWNrLXNzbyIvPjwvc2FtbDpTdWJqZWN0Q29uZmlybWF0aW9uPjwvc2FtbDpTdWJqZWN0PjxzYW1sOkNvbmRpdGlvbnMgTm90QmVmb3JlPSIyMDIzLTExLTIwVDEzOjE5OjU4WiIgTm90T25PckFmdGVyPSIyMDIzLTExLTIwVDEzOjI1OjI4WiI%2BPHNhbWw6QXVkaWVuY2VSZXN0cmljdGlvbj48c2FtbDpBdWRpZW5jZT5MT1JJLWRldiBAIFRlbGVrb20tQ2xvdWQ8L3NhbWw6QXVkaWVuY2U%2BPC9zYW1sOkF1ZGllbmNlUmVzdHJpY3Rpb24%2BPC9zYW1sOkNvbmRpdGlvbnM%2BPHNhbWw6QXV0aG5TdGF0ZW1lbnQgQXV0aG5JbnN0YW50PSIyMDIzLTExLTIwVDEzOjIwOjI4WiIgU2Vzc2lvbkluZGV4PSJEVU9fOTNjNDk2NGIxMWFkNmY2ZTgxMzczZjk0ZDY4OGFjZTYzYmFjZjdkZSI%2BPHNhbWw6QXV0aG5Db250ZXh0PjxzYW1sOkF1dGhuQ29udGV4dENsYXNzUmVmPnVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDphYzpjbGFzc2VzOlBhc3N3b3JkUHJvdGVjdGVkVHJhbnNwb3J0PC9zYW1sOkF1dGhuQ29udGV4dENsYXNzUmVmPjwvc2FtbDpBdXRobkNvbnRleHQ%2BPC9zYW1sOkF1dGhuU3RhdGVtZW50Pjwvc2FtbDpBc3NlcnRpb24%2BPC9zYW1scDpSZXNwb25zZT4%3D"
        const val TEST_SESSION_ID = "foobar"
        val VALID_TIME = OffsetDateTime.of(
            2023,
            11,
            20,
            13,
            20,
            0,
            0,
            ZoneOffset.UTC
        )!!

        val INVALID_TIME = OffsetDateTime.of(
            2023,
            11,
            20,
            13,
            18,
            0,
            0,
            ZoneOffset.UTC
        )!!
    }
}
