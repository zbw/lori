package de.zbw.api.lori.server.type

import io.ktor.server.plugins.BadRequestException
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.opensaml.saml.metadata.resolver.impl.FilesystemMetadataResolver
import org.opensaml.saml.saml2.core.Response
import org.opensaml.xmlsec.signature.support.SignatureException
import org.testng.annotations.Test
import java.io.File
import java.io.FileNotFoundException

class SamlUtilsTest {
    @Test
    fun testVerifySignatureUsingSignatureValidator() {
        val samlUtils =
            SamlUtils(
                metadataPath = VALID_SENDER_ENTITY_ID,
            )

        val metadataFile =
            javaClass.classLoader
                .getResource(SENDER_METADATA_PATH)
                ?.toURI()
                ?.let { File(it) }
                ?: throw FileNotFoundException("Metadata file was not found under path $SENDER_METADATA_PATH")
        val metadataResolver = FilesystemMetadataResolver(metadataFile)
        samlUtils.initMetadataResolver(metadataResolver)

        val response: Response =
            samlUtils.unmarshallSAMLObject(SAML_RESPONSE_VALID)
                ?: throw BadRequestException("Input is invalid. Expected SAML2.0 response in XML format.")
        samlUtils.verifySignatureUsingSignatureValidator(
            response.signature ?: throw SecurityException("No signature in response."),
        )
    }

    @Test(expectedExceptions = [SecurityException::class])
    fun testVerifySignatureUsingSignatureValidatorInvalidCriterion() {
        val samlUtils =
            SamlUtils(
                INVALID_SENDER_ENTITY_ID,
            )
        val metadataFile =
            javaClass.classLoader
                .getResource(SENDER_METADATA_PATH)
                ?.toURI()
                ?.let { File(it) }
                ?: throw FileNotFoundException("Metadata file was not found under path $SENDER_METADATA_PATH")
        val metadataResolver = FilesystemMetadataResolver(metadataFile)
        samlUtils.initMetadataResolver(metadataResolver)

        val response: Response =
            samlUtils.unmarshallSAMLObject(SAML_RESPONSE_VALID)
                ?: throw BadRequestException("Input is invalid. Expected SAML2.0 response in XML format.")
        samlUtils.verifySignatureUsingSignatureValidator(
            response.signature ?: throw SecurityException("No signature in response."),
        )
    }

    @Test(expectedExceptions = [SignatureException::class])
    fun testVerifySignatureUsingSignatureValidatorInvalidSignature() {
        val samlUtils =
            SamlUtils(
                VALID_SENDER_ENTITY_ID,
            )
        val metadataFile =
            javaClass.classLoader
                .getResource(SENDER_METADATA_PATH)
                ?.toURI()
                ?.let { File(it) }
                ?: throw FileNotFoundException("Metadata file was not found under path $SENDER_METADATA_PATH")
        val metadataResolver = FilesystemMetadataResolver(metadataFile)
        samlUtils.initMetadataResolver(metadataResolver)
        val response: Response =
            samlUtils.unmarshallSAMLObject(SAML_RESPONSE_INVALID_SIGNATURE)
                ?: throw BadRequestException("Input is invalid. Expected SAML2.0 response in XML format.")
        samlUtils.verifySignatureUsingSignatureValidator(
            response.signature ?: throw SecurityException("No signature in response."),
        )
    }

    @Test
    fun testDecodeSamlResponse() {
        assertThat(
            SamlUtils.decodeSAMLResponse(VALID_SAML_RESPONSE_ENCODED),
            `is`(SAML_RESPONSE_VALID),
        )
    }

    companion object {
        const val SENDER_METADATA_PATH = "saml/metadata.xml"

        @Suppress("ktlint:standard:max-line-length")
        const val VALID_SAML_RESPONSE_ENCODED =
            "SAMLResponse=PHNhbWxwOlJlc3BvbnNlIHhtbG5zOnNhbWw9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDphc3NlcnRpb24iIHhtbG5zOnNhbWxwPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6cHJvdG9jb2wiIHhtbG5zOnhzPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxL1hNTFNjaGVtYSIgeG1sbnM6eHNpPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxL1hNTFNjaGVtYS1pbnN0YW5jZSIgeG1sbnM6ZHM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvMDkveG1sZHNpZyMiIHhtbG5zOm1kPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6bWV0YWRhdGEiIHhtbG5zOnhlbmM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvMDQveG1sZW5jIyIgeG1sbnM6eGVuYzExPSJodHRwOi8vd3d3LnczLm9yZy8yMDA5L3htbGVuYzExIyIgVmVyc2lvbj0iMi4wIiBJRD0iRFVPXzFiN2NlMDk1OGUyZTUzNjc2MWZmMTllZDE3NDI4ODRmNmVlN2M2OTQiIElzc3VlSW5zdGFudD0iMjAyNC0wMS0xMVQxNDoxNTowNFoiIERlc3RpbmF0aW9uPSJodHRwOi8vZ3VpLmxvcmktZGV2Lnpidy1uZXR0Lnpidy1raWVsLmRlL3VpL2NhbGxiYWNrLXNzbyI%2BPHNhbWw6SXNzdWVyPmh0dHBzOi8vc3NvLTE3OTNiMTdjLnNzby5kdW9zZWN1cml0eS5jb20vc2FtbDIvc3AvRElNT0pUUFBMNkUwOTBYNEJVTDEvbWV0YWRhdGE8L3NhbWw6SXNzdWVyPjxkczpTaWduYXR1cmU%2BPGRzOlNpZ25lZEluZm8%2BPGRzOkNhbm9uaWNhbGl6YXRpb25NZXRob2QgQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxLzEwL3htbC1leGMtYzE0biMiLz48ZHM6U2lnbmF0dXJlTWV0aG9kIEFsZ29yaXRobT0iaHR0cDovL3d3dy53My5vcmcvMjAwMS8wNC94bWxkc2lnLW1vcmUjcnNhLXNoYTI1NiIvPjxkczpSZWZlcmVuY2UgVVJJPSIjRFVPXzFiN2NlMDk1OGUyZTUzNjc2MWZmMTllZDE3NDI4ODRmNmVlN2M2OTQiPjxkczpUcmFuc2Zvcm1zPjxkczpUcmFuc2Zvcm0gQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRzaWcjZW52ZWxvcGVkLXNpZ25hdHVyZSIvPjxkczpUcmFuc2Zvcm0gQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxLzEwL3htbC1leGMtYzE0biMiLz48L2RzOlRyYW5zZm9ybXM%2BPGRzOkRpZ2VzdE1ldGhvZCBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvMDQveG1sZW5jI3NoYTI1NiIvPjxkczpEaWdlc3RWYWx1ZT5Fcm1ESnpVbHRwSU85VkQvQmU2TUJDYnkxdnZ0MkJsZ0Izd0JGTWNKc1BzPTwvZHM6RGlnZXN0VmFsdWU%2BPC9kczpSZWZlcmVuY2U%2BPC9kczpTaWduZWRJbmZvPjxkczpTaWduYXR1cmVWYWx1ZT5Yd3RFeWlHRW9EZ3lwU3JGeW5DM21oSTZFMG5NSEx4OE11MTN2R1piOVplNjNyL2k5MUdUajNhM1lNUHJMNDZUNWcyUWpyOC8wQkJaOUlXSFNhcjBxdG16NHpxTm94VktJSmx0dW5JV1FXYTZOdlhOeXBkQ0F0VGFCYWNCZFhnd20ya3VmdHRRbndJQk9BSEREZGJlQkJUMlpndHdMaXIxdExuTjVISDIwTmpoNnhxckd2RUtGSjRsenphUmF5WHp5MXlWMEE3VVZuQitnTnN1QS8yTm4rMVRKYVE5Wlk2a2lDNEwzbjZMNUtxSjRVWUY3ZzlSbTVqdlR5bG9HY1V6S1daNzI0ZmFOd3pDbUZtZmVTWUxtY2dYc0xJRkRoamFVMzRBdThFYks1SDdRczFTWlhZM0llQkYwUXBWQ2JUVlpBN1Q3Q0NveFEwSUE0c0w4U3h1aXc9PTwvZHM6U2lnbmF0dXJlVmFsdWU%2BPGRzOktleUluZm8%2BPGRzOlg1MDlEYXRhPjxkczpYNTA5Q2VydGlmaWNhdGU%2BTUlJRERUQ0NBZldnQXdJQkFnSVVCbHZhQ3hURkxpQW03ZDc1UVlPZGg0QW4zTW93RFFZSktvWklodmNOQVFFTEJRQXdOakVWTUJNR0ExVUVDZ3dNUkhWdklGTmxZM1Z5YVhSNU1SMHdHd1lEVlFRRERCUkVTVTFQU2xSUVVFdzJSVEE1TUZnMFFsVk1NVEFlRncweU16RXhNRGN4TkRFNU16aGFGdzB6T0RBeE1Ua3dNekUwTURkYU1EWXhGVEFUQmdOVkJBb01ERVIxYnlCVFpXTjFjbWwwZVRFZE1Cc0dBMVVFQXd3VVJFbE5UMHBVVUZCTU5rVXdPVEJZTkVKVlRERXdnZ0VpTUEwR0NTcUdTSWIzRFFFQkFRVUFBNElCRHdBd2dnRUtBb0lCQVFDVndDa2YrS1RFWGRwQlo1dGNwZzRuM1VSbUFGM3FWV3ZicVhtRXQ2LytKNHFRZ3VBY1l5M2hGcHVhTlpzMjR3Z09hZ2d4c1dpWGNhM1MzVnE5cDUyV0hDL0s2eGFyUXl2OWpEemNTRVp2alJ2TVVMeTFlL3R3L1VkaWgxMlVSWnM2VER5Z3RFZmp0OFNUZ1VFbU5IY05McmorV3FkcFVvT0VSQlVaUnZWYkFNK0lGOXlUUUx2YXd5VXdNRzJhMjEvUXFtOTc2dXlHR3hNTnhDeGFtVlJnL2d1NlFvMzdwQjYrc0QrTlU4WjM0SU9HWlNlUTNFYmFXbVY3aVBVdGdSWVlkbXhCTGtFblpKRnVOUUI2ZFFhRno3Z0kvTXdEcUxkQlRLdEFJQmZ6S1RrYXJlUW5XNXlTVVFoZzZqenRXUXNCUXpRb2k3MWEzM2s5MUF6WjR5a3hBZ01CQUFHakV6QVJNQThHQTFVZEV3RUIvd1FGTUFNQkFmOHdEUVlKS29aSWh2Y05BUUVMQlFBRGdnRUJBSDhySXp0UlNyd2F5Ni9xMktFcDdSYjJJL1l2NlZXb3preEN1TU1PWDdRbkNaanJ3OURzaW81SHFTdkdLZ1NEZUZNYlE1L0pwY3pVZU9Pb0xWZ1BmbWppMTZ5d1k1ODJSZ2RxZ0ZabjdNcVhkMVUzRWF1TmtrSFdvTjlkTVV3ZGVYOTV6bjcyMWNYQ2g2R1NEaWl2eWtKSkVIVW9HSHd1YndEcFhZVW84Vlg0dVlRZzZlMUM0WTI1R2JTQ3E1ZXhPN2xXQzRhaGxtUWpyTW1pTWlpK0VHMmE1VmtmK1hNMXdDcE5vZnowd1UzUXZYNUltNFBubEhqQzVXbVdldHF3QTlUMWE3WUZIVDRnbnZOdG14Zm95UXBJRUpsanJsOWc5cm9mMnVBQXB3T3Z4SXNqdGlRMnlZb3FpWnY5Z2UrbmlkVHRMdjBmbG1senk2ZFRYdmJIczJjPTwvZHM6WDUwOUNlcnRpZmljYXRlPjwvZHM6WDUwOURhdGE%2BPC9kczpLZXlJbmZvPjwvZHM6U2lnbmF0dXJlPjxzYW1scDpTdGF0dXM%2BPHNhbWxwOlN0YXR1c0NvZGUgVmFsdWU9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDpzdGF0dXM6U3VjY2VzcyIvPjwvc2FtbHA6U3RhdHVzPjxzYW1sOkFzc2VydGlvbiBJRD0iRFVPXzNiOGZlMjgzY2ZlYTY3ZDkyOTUwNjBiNzhlZjZhNDVhNjY1ZmQ2MTgiIElzc3VlSW5zdGFudD0iMjAyNC0wMS0xMVQxNDoxNTowNFoiIFZlcnNpb249IjIuMCI%2BPHNhbWw6SXNzdWVyPmh0dHBzOi8vc3NvLTE3OTNiMTdjLnNzby5kdW9zZWN1cml0eS5jb20vc2FtbDIvc3AvRElNT0pUUFBMNkUwOTBYNEJVTDEvbWV0YWRhdGE8L3NhbWw6SXNzdWVyPjxkczpTaWduYXR1cmU%2BPGRzOlNpZ25lZEluZm8%2BPGRzOkNhbm9uaWNhbGl6YXRpb25NZXRob2QgQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxLzEwL3htbC1leGMtYzE0biMiLz48ZHM6U2lnbmF0dXJlTWV0aG9kIEFsZ29yaXRobT0iaHR0cDovL3d3dy53My5vcmcvMjAwMS8wNC94bWxkc2lnLW1vcmUjcnNhLXNoYTI1NiIvPjxkczpSZWZlcmVuY2UgVVJJPSIjRFVPXzNiOGZlMjgzY2ZlYTY3ZDkyOTUwNjBiNzhlZjZhNDVhNjY1ZmQ2MTgiPjxkczpUcmFuc2Zvcm1zPjxkczpUcmFuc2Zvcm0gQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRzaWcjZW52ZWxvcGVkLXNpZ25hdHVyZSIvPjxkczpUcmFuc2Zvcm0gQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxLzEwL3htbC1leGMtYzE0biMiLz48L2RzOlRyYW5zZm9ybXM%2BPGRzOkRpZ2VzdE1ldGhvZCBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvMDQveG1sZW5jI3NoYTI1NiIvPjxkczpEaWdlc3RWYWx1ZT5OMHE5V2FhSkJGa3NsdzlTNm1EUVg0eFFqM2dsdHFrc0Zvc2o3UGpuWExFPTwvZHM6RGlnZXN0VmFsdWU%2BPC9kczpSZWZlcmVuY2U%2BPC9kczpTaWduZWRJbmZvPjxkczpTaWduYXR1cmVWYWx1ZT5TVk9Lb0lNVWhRRUI1b2VGbzkxNzhMeDNKd1ZVcEkyVzJFbTMyRTIyeDRJbXc5TXcwQVNGMC9uQTJSMXJzZExzYWJVcFZpSnE1K2JCWVJwaWFkeXV3YVVDMWJ1WERjM2lrRkg4Y1VpMktNSDFXNy94blNwMXQyYUpRZDc2c0R6Z3BxSDc5cmFFS0t6ODA3S2IxVkYyRWh2dVYwVzFsem5lYUs4QmxxMWNkTkluWjVWYnUxeWRMMEd3RXZ1THZBeWQvWDUxdW1CWk4ydmVIS2FFb21BeFRjcDBzZHMvSCsxSHlNcGJWOVdybGo3UitWTnVzZGFVVkF6T3dkTWlLMzdMUnFWdVg1NzRRSlV3Si9SUGRuMXYzSHcvVERDZUg3YU5ibS83N3FhWmRBd2hwVzZQTklpNDhTdDAwbWdRZHlkajNKV0hiQjF2ZHBSS04wd0ZaWVVSZXc9PTwvZHM6U2lnbmF0dXJlVmFsdWU%2BPGRzOktleUluZm8%2BPGRzOlg1MDlEYXRhPjxkczpYNTA5Q2VydGlmaWNhdGU%2BTUlJRERUQ0NBZldnQXdJQkFnSVVCbHZhQ3hURkxpQW03ZDc1UVlPZGg0QW4zTW93RFFZSktvWklodmNOQVFFTEJRQXdOakVWTUJNR0ExVUVDZ3dNUkhWdklGTmxZM1Z5YVhSNU1SMHdHd1lEVlFRRERCUkVTVTFQU2xSUVVFdzJSVEE1TUZnMFFsVk1NVEFlRncweU16RXhNRGN4TkRFNU16aGFGdzB6T0RBeE1Ua3dNekUwTURkYU1EWXhGVEFUQmdOVkJBb01ERVIxYnlCVFpXTjFjbWwwZVRFZE1Cc0dBMVVFQXd3VVJFbE5UMHBVVUZCTU5rVXdPVEJZTkVKVlRERXdnZ0VpTUEwR0NTcUdTSWIzRFFFQkFRVUFBNElCRHdBd2dnRUtBb0lCQVFDVndDa2YrS1RFWGRwQlo1dGNwZzRuM1VSbUFGM3FWV3ZicVhtRXQ2LytKNHFRZ3VBY1l5M2hGcHVhTlpzMjR3Z09hZ2d4c1dpWGNhM1MzVnE5cDUyV0hDL0s2eGFyUXl2OWpEemNTRVp2alJ2TVVMeTFlL3R3L1VkaWgxMlVSWnM2VER5Z3RFZmp0OFNUZ1VFbU5IY05McmorV3FkcFVvT0VSQlVaUnZWYkFNK0lGOXlUUUx2YXd5VXdNRzJhMjEvUXFtOTc2dXlHR3hNTnhDeGFtVlJnL2d1NlFvMzdwQjYrc0QrTlU4WjM0SU9HWlNlUTNFYmFXbVY3aVBVdGdSWVlkbXhCTGtFblpKRnVOUUI2ZFFhRno3Z0kvTXdEcUxkQlRLdEFJQmZ6S1RrYXJlUW5XNXlTVVFoZzZqenRXUXNCUXpRb2k3MWEzM2s5MUF6WjR5a3hBZ01CQUFHakV6QVJNQThHQTFVZEV3RUIvd1FGTUFNQkFmOHdEUVlKS29aSWh2Y05BUUVMQlFBRGdnRUJBSDhySXp0UlNyd2F5Ni9xMktFcDdSYjJJL1l2NlZXb3preEN1TU1PWDdRbkNaanJ3OURzaW81SHFTdkdLZ1NEZUZNYlE1L0pwY3pVZU9Pb0xWZ1BmbWppMTZ5d1k1ODJSZ2RxZ0ZabjdNcVhkMVUzRWF1TmtrSFdvTjlkTVV3ZGVYOTV6bjcyMWNYQ2g2R1NEaWl2eWtKSkVIVW9HSHd1YndEcFhZVW84Vlg0dVlRZzZlMUM0WTI1R2JTQ3E1ZXhPN2xXQzRhaGxtUWpyTW1pTWlpK0VHMmE1VmtmK1hNMXdDcE5vZnowd1UzUXZYNUltNFBubEhqQzVXbVdldHF3QTlUMWE3WUZIVDRnbnZOdG14Zm95UXBJRUpsanJsOWc5cm9mMnVBQXB3T3Z4SXNqdGlRMnlZb3FpWnY5Z2UrbmlkVHRMdjBmbG1senk2ZFRYdmJIczJjPTwvZHM6WDUwOUNlcnRpZmljYXRlPjwvZHM6WDUwOURhdGE%2BPC9kczpLZXlJbmZvPjwvZHM6U2lnbmF0dXJlPjxzYW1sOlN1YmplY3Q%2BPHNhbWw6TmFtZUlEIEZvcm1hdD0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6MS4xOm5hbWVpZC1mb3JtYXQ6ZW1haWxBZGRyZXNzIj5jLmJheUB6Ynctb25saW5lLmV1PC9zYW1sOk5hbWVJRD48c2FtbDpTdWJqZWN0Q29uZmlybWF0aW9uIE1ldGhvZD0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOmNtOmJlYXJlciI%2BPHNhbWw6U3ViamVjdENvbmZpcm1hdGlvbkRhdGEgTm90T25PckFmdGVyPSIyMDI0LTAxLTExVDE0OjIwOjA0WiIgUmVjaXBpZW50PSJodHRwOi8vZ3VpLmxvcmktZGV2Lnpidy1uZXR0Lnpidy1raWVsLmRlL3VpL2NhbGxiYWNrLXNzbyIvPjwvc2FtbDpTdWJqZWN0Q29uZmlybWF0aW9uPjwvc2FtbDpTdWJqZWN0PjxzYW1sOkNvbmRpdGlvbnMgTm90QmVmb3JlPSIyMDI0LTAxLTExVDE0OjE0OjM0WiIgTm90T25PckFmdGVyPSIyMDI0LTAxLTExVDE0OjIwOjA0WiI%2BPHNhbWw6QXVkaWVuY2VSZXN0cmljdGlvbj48c2FtbDpBdWRpZW5jZT5MT1JJLWRldiBAIFRlbGVrb20tQ2xvdWQ8L3NhbWw6QXVkaWVuY2U%2BPC9zYW1sOkF1ZGllbmNlUmVzdHJpY3Rpb24%2BPC9zYW1sOkNvbmRpdGlvbnM%2BPHNhbWw6QXV0aG5TdGF0ZW1lbnQgQXV0aG5JbnN0YW50PSIyMDI0LTAxLTExVDE0OjE1OjA0WiIgU2Vzc2lvbkluZGV4PSJEVU9fM2I4ZmUyODNjZmVhNjdkOTI5NTA2MGI3OGVmNmE0NWE2NjVmZDYxOCI%2BPHNhbWw6QXV0aG5Db250ZXh0PjxzYW1sOkF1dGhuQ29udGV4dENsYXNzUmVmPnVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDphYzpjbGFzc2VzOlBhc3N3b3JkUHJvdGVjdGVkVHJhbnNwb3J0PC9zYW1sOkF1dGhuQ29udGV4dENsYXNzUmVmPjwvc2FtbDpBdXRobkNvbnRleHQ%2BPC9zYW1sOkF1dGhuU3RhdGVtZW50Pjwvc2FtbDpBc3NlcnRpb24%2BPC9zYW1scDpSZXNwb25zZT4%3D"

        @Suppress("ktlint:standard:max-line-length")
        const val SAML_RESPONSE_VALID =
            "<samlp:Response xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\" xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\" xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\" xmlns:xenc11=\"http://www.w3.org/2009/xmlenc11#\" Version=\"2.0\" ID=\"DUO_1b7ce0958e2e536761ff19ed1742884f6ee7c694\" IssueInstant=\"2024-01-11T14:15:04Z\" Destination=\"http://gui.lori-dev.zbw-nett.zbw-kiel.de/ui/callback-sso\"><saml:Issuer>https://sso-1793b17c.sso.duosecurity.com/saml2/sp/DIMOJTPPL6E090X4BUL1/metadata</saml:Issuer><ds:Signature><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/><ds:SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\"/><ds:Reference URI=\"#DUO_1b7ce0958e2e536761ff19ed1742884f6ee7c694\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\"/><ds:DigestValue>ErmDJzUltpIO9VD/Be6MBCby1vvt2BlgB3wBFMcJsPs=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>XwtEyiGEoDgypSrFynC3mhI6E0nMHLx8Mu13vGZb9Ze63r/i91GTj3a3YMPrL46T5g2Qjr8/0BBZ9IWHSar0qtmz4zqNoxVKIJltunIWQWa6NvXNypdCAtTaBacBdXgwm2kufttQnwIBOAHDDdbeBBT2ZgtwLir1tLnN5HH20Njh6xqrGvEKFJ4lzzaRayXzy1yV0A7UVnB+gNsuA/2Nn+1TJaQ9ZY6kiC4L3n6L5KqJ4UYF7g9Rm5jvTyloGcUzKWZ724faNwzCmFmfeSYLmcgXsLIFDhjaU34Au8EbK5H7Qs1SZXY3IeBF0QpVCbTVZA7T7CCoxQ0IA4sL8Sxuiw==</ds:SignatureValue><ds:KeyInfo><ds:X509Data><ds:X509Certificate>MIIDDTCCAfWgAwIBAgIUBlvaCxTFLiAm7d75QYOdh4An3MowDQYJKoZIhvcNAQELBQAwNjEVMBMGA1UECgwMRHVvIFNlY3VyaXR5MR0wGwYDVQQDDBRESU1PSlRQUEw2RTA5MFg0QlVMMTAeFw0yMzExMDcxNDE5MzhaFw0zODAxMTkwMzE0MDdaMDYxFTATBgNVBAoMDER1byBTZWN1cml0eTEdMBsGA1UEAwwURElNT0pUUFBMNkUwOTBYNEJVTDEwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCVwCkf+KTEXdpBZ5tcpg4n3URmAF3qVWvbqXmEt6/+J4qQguAcYy3hFpuaNZs24wgOaggxsWiXca3S3Vq9p52WHC/K6xarQyv9jDzcSEZvjRvMULy1e/tw/Udih12URZs6TDygtEfjt8STgUEmNHcNLrj+WqdpUoOERBUZRvVbAM+IF9yTQLvawyUwMG2a21/Qqm976uyGGxMNxCxamVRg/gu6Qo37pB6+sD+NU8Z34IOGZSeQ3EbaWmV7iPUtgRYYdmxBLkEnZJFuNQB6dQaFz7gI/MwDqLdBTKtAIBfzKTkareQnW5ySUQhg6jztWQsBQzQoi71a33k91AzZ4ykxAgMBAAGjEzARMA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQELBQADggEBAH8rIztRSrway6/q2KEp7Rb2I/Yv6VWozkxCuMMOX7QnCZjrw9Dsio5HqSvGKgSDeFMbQ5/JpczUeOOoLVgPfmji16ywY582RgdqgFZn7MqXd1U3EauNkkHWoN9dMUwdeX95zn721cXCh6GSDiivykJJEHUoGHwubwDpXYUo8VX4uYQg6e1C4Y25GbSCq5exO7lWC4ahlmQjrMmiMii+EG2a5Vkf+XM1wCpNofz0wU3QvX5Im4PnlHjC5WmWetqwA9T1a7YFHT4gnvNtmxfoyQpIEJljrl9g9rof2uAApwOvxIsjtiQ2yYoqiZv9ge+nidTtLv0flmlzy6dTXvbHs2c=</ds:X509Certificate></ds:X509Data></ds:KeyInfo></ds:Signature><samlp:Status><samlp:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\"/></samlp:Status><saml:Assertion ID=\"DUO_3b8fe283cfea67d9295060b78ef6a45a665fd618\" IssueInstant=\"2024-01-11T14:15:04Z\" Version=\"2.0\"><saml:Issuer>https://sso-1793b17c.sso.duosecurity.com/saml2/sp/DIMOJTPPL6E090X4BUL1/metadata</saml:Issuer><ds:Signature><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/><ds:SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\"/><ds:Reference URI=\"#DUO_3b8fe283cfea67d9295060b78ef6a45a665fd618\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\"/><ds:DigestValue>N0q9WaaJBFkslw9S6mDQX4xQj3gltqksFosj7PjnXLE=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>SVOKoIMUhQEB5oeFo9178Lx3JwVUpI2W2Em32E22x4Imw9Mw0ASF0/nA2R1rsdLsabUpViJq5+bBYRpiadyuwaUC1buXDc3ikFH8cUi2KMH1W7/xnSp1t2aJQd76sDzgpqH79raEKKz807Kb1VF2EhvuV0W1lzneaK8Blq1cdNInZ5Vbu1ydL0GwEvuLvAyd/X51umBZN2veHKaEomAxTcp0sds/H+1HyMpbV9Wrlj7R+VNusdaUVAzOwdMiK37LRqVuX574QJUwJ/RPdn1v3Hw/TDCeH7aNbm/77qaZdAwhpW6PNIi48St00mgQdydj3JWHbB1vdpRKN0wFZYURew==</ds:SignatureValue><ds:KeyInfo><ds:X509Data><ds:X509Certificate>MIIDDTCCAfWgAwIBAgIUBlvaCxTFLiAm7d75QYOdh4An3MowDQYJKoZIhvcNAQELBQAwNjEVMBMGA1UECgwMRHVvIFNlY3VyaXR5MR0wGwYDVQQDDBRESU1PSlRQUEw2RTA5MFg0QlVMMTAeFw0yMzExMDcxNDE5MzhaFw0zODAxMTkwMzE0MDdaMDYxFTATBgNVBAoMDER1byBTZWN1cml0eTEdMBsGA1UEAwwURElNT0pUUFBMNkUwOTBYNEJVTDEwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCVwCkf+KTEXdpBZ5tcpg4n3URmAF3qVWvbqXmEt6/+J4qQguAcYy3hFpuaNZs24wgOaggxsWiXca3S3Vq9p52WHC/K6xarQyv9jDzcSEZvjRvMULy1e/tw/Udih12URZs6TDygtEfjt8STgUEmNHcNLrj+WqdpUoOERBUZRvVbAM+IF9yTQLvawyUwMG2a21/Qqm976uyGGxMNxCxamVRg/gu6Qo37pB6+sD+NU8Z34IOGZSeQ3EbaWmV7iPUtgRYYdmxBLkEnZJFuNQB6dQaFz7gI/MwDqLdBTKtAIBfzKTkareQnW5ySUQhg6jztWQsBQzQoi71a33k91AzZ4ykxAgMBAAGjEzARMA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQELBQADggEBAH8rIztRSrway6/q2KEp7Rb2I/Yv6VWozkxCuMMOX7QnCZjrw9Dsio5HqSvGKgSDeFMbQ5/JpczUeOOoLVgPfmji16ywY582RgdqgFZn7MqXd1U3EauNkkHWoN9dMUwdeX95zn721cXCh6GSDiivykJJEHUoGHwubwDpXYUo8VX4uYQg6e1C4Y25GbSCq5exO7lWC4ahlmQjrMmiMii+EG2a5Vkf+XM1wCpNofz0wU3QvX5Im4PnlHjC5WmWetqwA9T1a7YFHT4gnvNtmxfoyQpIEJljrl9g9rof2uAApwOvxIsjtiQ2yYoqiZv9ge+nidTtLv0flmlzy6dTXvbHs2c=</ds:X509Certificate></ds:X509Data></ds:KeyInfo></ds:Signature><saml:Subject><saml:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress\">c.bay@zbw-online.eu</saml:NameID><saml:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\"><saml:SubjectConfirmationData NotOnOrAfter=\"2024-01-11T14:20:04Z\" Recipient=\"http://gui.lori-dev.zbw-nett.zbw-kiel.de/ui/callback-sso\"/></saml:SubjectConfirmation></saml:Subject><saml:Conditions NotBefore=\"2024-01-11T14:14:34Z\" NotOnOrAfter=\"2024-01-11T14:20:04Z\"><saml:AudienceRestriction><saml:Audience>LORI-dev @ Telekom-Cloud</saml:Audience></saml:AudienceRestriction></saml:Conditions><saml:AuthnStatement AuthnInstant=\"2024-01-11T14:15:04Z\" SessionIndex=\"DUO_3b8fe283cfea67d9295060b78ef6a45a665fd618\"><saml:AuthnContext><saml:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport</saml:AuthnContextClassRef></saml:AuthnContext></saml:AuthnStatement></saml:Assertion></samlp:Response>"

        @Suppress("ktlint:standard:max-line-length")
        const val SAML_RESPONSE_INVALID_SIGNATURE =
            "<samlp:Response xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\" xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\" xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\" xmlns:xenc11=\"http://www.w3.org/2009/xmlenc11#\" Version=\"2.0\" ID=\"DUO_1b7ce0958e2e536761ff19ed1742884f6ee7c694\" IssueInstant=\"2024-01-11T14:15:04Z\" Destination=\"http://gui.lori-dev.zbw-nett.zbw-kiel.de/ui/callback-sso\"><saml:Issuer>https://sso-1793b17c.sso.duosecurity.com/saml2/sp/DIMOJTPPL6E090X4BUL1/metadata</saml:Issuer><ds:Signature><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/><ds:SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\"/><ds:Reference URI=\"#DUO_1b7ce0958e2e536761ff19ed1742884f6ee7c694\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\"/><ds:DigestValue>ErmDJzUltpIO9VD/Be6MBCby1vvt2BlgB3wBFMcJsPs=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>XwtEyiGEoDgypSrFHLHFLFHLF0nMHLx8Mu13vGZb9Ze63r/i91GTj3a3YMPrL46T5g2Qjr8/0BBZ9IWHSar0qtmz4zqNoxVKIJltunIWQWa6NvXNypdCAtTaBacBdXgwm2kufttQnwIBOAHDDdbeBBT2ZgtwLir1tLnN5HH20Njh6xqrGvEKFJ4lzzaRayXzy1yV0A7UVnB+gNsuA/2Nn+1TJaQ9ZY6kiC4L3n6L5KqJ4UYF7g9Rm5jvTyloGcUzKWZ724faNwzCmFmfeSYLmcgXsLIFDhjaU34Au8EbK5H7Qs1SZXY3IeBF0QpVCbTVZA7T7CCoxQ0IA4sL8Sxuiw==</ds:SignatureValue><ds:KeyInfo><ds:X509Data><ds:X509Certificate>MIIDDTCCAfWgAwIBAgIUBlvaCxTFLiAm7d75QYOdh4An3MowDQYJKoZIhvcNAQELBQAwNjEVMBMGA1UECgwMRHVvIFNlY3VyaXR5MR0wGwYDVQQDDBRESU1PSlRQUEw2RTA5MFg0QlVMMTAeFw0yMzExMDcxNDE5MzhaFw0zODAxMTkwMzE0MDdaMDYxFTATBgNVBAoMDER1byBTZWN1cml0eTEdMBsGA1UEAwwURElNT0pUUFBMNkUwOTBYNEJVTDEwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCVwCkf+KTEXdpBZ5tcpg4n3URmAF3qVWvbqXmEt6/+J4qQguAcYy3hFpuaNZs24wgOaggxsWiXca3S3Vq9p52WHC/K6xarQyv9jDzcSEZvjRvMULy1e/tw/Udih12URZs6TDygtEfjt8STgUEmNHcNLrj+WqdpUoOERBUZRvVbAM+IF9yTQLvawyUwMG2a21/Qqm976uyGGxMNxCxamVRg/gu6Qo37pB6+sD+NU8Z34IOGZSeQ3EbaWmV7iPUtgRYYdmxBLkEnZJFuNQB6dQaFz7gI/MwDqLdBTKtAIBfzKTkareQnW5ySUQhg6jztWQsBQzQoi71a33k91AzZ4ykxAgMBAAGjEzARMA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQELBQADggEBAH8rIztRSrway6/q2KEp7Rb2I/Yv6VWozkxCuMMOX7QnCZjrw9Dsio5HqSvGKgSDeFMbQ5/JpczUeOOoLVgPfmji16ywY582RgdqgFZn7MqXd1U3EauNkkHWoN9dMUwdeX95zn721cXCh6GSDiivykJJEHUoGHwubwDpXYUo8VX4uYQg6e1C4Y25GbSCq5exO7lWC4ahlmQjrMmiMii+EG2a5Vkf+XM1wCpNofz0wU3QvX5Im4PnlHjC5WmWetqwA9T1a7YFHT4gnvNtmxfoyQpIEJljrl9g9rof2uAApwOvxIsjtiQ2yYoqiZv9ge+nidTtLv0flmlzy6dTXvbHs2c=</ds:X509Certificate></ds:X509Data></ds:KeyInfo></ds:Signature><samlp:Status><samlp:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\"/></samlp:Status><saml:Assertion ID=\"DUO_3b8fe283cfea67d9295060b78ef6a45a665fd618\" IssueInstant=\"2024-01-11T14:15:04Z\" Version=\"2.0\"><saml:Issuer>https://sso-1793b17c.sso.duosecurity.com/saml2/sp/DIMOJTPPL6E090X4BUL1/metadata</saml:Issuer><ds:Signature><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/><ds:SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\"/><ds:Reference URI=\"#DUO_3b8fe283cfea67d9295060b78ef6a45a665fd618\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\"/><ds:DigestValue>N0q9WaaJBFkslw9S6mDQX4xQj3gltqksFosj7PjnXLE=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>SVOKoIMUhQEB5oeFo9178Lx3JwVUpI2W2Em32E22x4Imw9Mw0ASF0/nA2R1rsdLsabUpViJq5+bBYRpiadyuwaUC1buXDc3ikFH8cUi2KMH1W7/xnSp1t2aJQd76sDzgpqH79raEKKz807Kb1VF2EhvuV0W1lzneaK8Blq1cdNInZ5Vbu1ydL0GwEvuLvAyd/X51umBZN2veHKaEomAxTcp0sds/H+1HyMpbV9Wrlj7R+VNusdaUVAzOwdMiK37LRqVuX574QJUwJ/RPdn1v3Hw/TDCeH7aNbm/77qaZdAwhpW6PNIi48St00mgQdydj3JWHbB1vdpRKN0wFZYURew==</ds:SignatureValue><ds:KeyInfo><ds:X509Data><ds:X509Certificate>MIIDDTCCAfWgAwIBAgIUBlvaCxTFLiAm7d75QYOdh4An3MowDQYJKoZIhvcNAQELBQAwNjEVMBMGA1UECgwMRHVvIFNlY3VyaXR5MR0wGwYDVQQDDBRESU1PSlRQUEw2RTA5MFg0QlVMMTAeFw0yMzExMDcxNDE5MzhaFw0zODAxMTkwMzE0MDdaMDYxFTATBgNVBAoMDER1byBTZWN1cml0eTEdMBsGA1UEAwwURElNT0pUUFBMNkUwOTBYNEJVTDEwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCVwCkf+KTEXdpBZ5tcpg4n3URmAF3qVWvbqXmEt6/+J4qQguAcYy3hFpuaNZs24wgOaggxsWiXca3S3Vq9p52WHC/K6xarQyv9jDzcSEZvjRvMULy1e/tw/Udih12URZs6TDygtEfjt8STgUEmNHcNLrj+WqdpUoOERBUZRvVbAM+IF9yTQLvawyUwMG2a21/Qqm976uyGGxMNxCxamVRg/gu6Qo37pB6+sD+NU8Z34IOGZSeQ3EbaWmV7iPUtgRYYdmxBLkEnZJFuNQB6dQaFz7gI/MwDqLdBTKtAIBfzKTkareQnW5ySUQhg6jztWQsBQzQoi71a33k91AzZ4ykxAgMBAAGjEzARMA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQELBQADggEBAH8rIztRSrway6/q2KEp7Rb2I/Yv6VWozkxCuMMOX7QnCZjrw9Dsio5HqSvGKgSDeFMbQ5/JpczUeOOoLVgPfmji16ywY582RgdqgFZn7MqXd1U3EauNkkHWoN9dMUwdeX95zn721cXCh6GSDiivykJJEHUoGHwubwDpXYUo8VX4uYQg6e1C4Y25GbSCq5exO7lWC4ahlmQjrMmiMii+EG2a5Vkf+XM1wCpNofz0wU3QvX5Im4PnlHjC5WmWetqwA9T1a7YFHT4gnvNtmxfoyQpIEJljrl9g9rof2uAApwOvxIsjtiQ2yYoqiZv9ge+nidTtLv0flmlzy6dTXvbHs2c=</ds:X509Certificate></ds:X509Data></ds:KeyInfo></ds:Signature><saml:Subject><saml:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress\">c.bay@zbw-online.eu</saml:NameID><saml:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\"><saml:SubjectConfirmationData NotOnOrAfter=\"2024-01-11T14:20:04Z\" Recipient=\"http://gui.lori-dev.zbw-nett.zbw-kiel.de/ui/callback-sso\"/></saml:SubjectConfirmation></saml:Subject><saml:Conditions NotBefore=\"2024-01-11T14:14:34Z\" NotOnOrAfter=\"2024-01-11T14:20:04Z\"><saml:AudienceRestriction><saml:Audience>LORI-dev @ Telekom-Cloud</saml:Audience></saml:AudienceRestriction></saml:Conditions><saml:AuthnStatement AuthnInstant=\"2024-01-11T14:15:04Z\" SessionIndex=\"DUO_3b8fe283cfea67d9295060b78ef6a45a665fd618\"><saml:AuthnContext><saml:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport</saml:AuthnContextClassRef></saml:AuthnContext></saml:AuthnStatement></saml:Assertion></samlp:Response>"

        @Suppress("ktlint:standard:max-line-length")
        const val VALID_SENDER_ENTITY_ID =
            "https://sso-1793b17c.sso.duosecurity.com/saml2/sp/DIMOJTPPL6E090X4BUL1/metadata"
        const val INVALID_SENDER_ENTITY_ID = "https://mycooldomain.net"
    }
}
