package de.zbw.api.lori.server.config

import de.gfelbing.konfig.core.source.ChainedKonfiguration
import de.gfelbing.konfig.core.source.SystemPropertiesKonfiguration
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class LoriConfigurationTest {

    @DataProvider(name = DATA_FOR_LORI_CONFIGURATION)
    fun createDataForLoriConfigurationTest() =
        arrayOf(
            arrayOf(
                CONFIG,
            ),
        )

    @Test(dataProvider = DATA_FOR_LORI_CONFIGURATION)
    fun testLoriConfiguration(expectedConfig: LoriConfiguration) {
        System.setProperty("lori.grpc.port", expectedConfig.grpcPort.toString())
        System.setProperty("lori.http.port", expectedConfig.httpPort.toString())
        System.setProperty("lori.sql.url", expectedConfig.sqlUrl)
        System.setProperty("lori.sql.user", expectedConfig.sqlUser)
        System.setProperty("lori.sql.password", expectedConfig.sqlPassword)
        System.setProperty("lori.connection.digitalarchive.address", expectedConfig.digitalArchiveAddress)
        System.setProperty("lori.connection.digitalarchive.basicauth", expectedConfig.digitalArchiveBasicAuth)
        System.setProperty("lori.connection.digitalarchive.credentials.user", expectedConfig.digitalArchiveUsername)
        System.setProperty("lori.connection.digitalarchive.credentials.password", expectedConfig.digitalArchivePassword)
        System.setProperty("lori.jwt.audience", expectedConfig.jwtAudience)
        System.setProperty("lori.jwt.issuer", expectedConfig.jwtIssuer)
        System.setProperty("lori.jwt.realm", expectedConfig.jwtRealm)
        System.setProperty("lori.jwt.secret", expectedConfig.jwtSecret)
        System.setProperty("lori.duo.senderentityid", expectedConfig.duoSenderEntityId)
        System.setProperty("lori.session.sign", expectedConfig.sessionSignKey)
        System.setProperty("lori.session.encrypt", expectedConfig.sessionEncryptKey)
        System.setProperty("lori.stage", expectedConfig.stage)
        System.setProperty("lori.connection.digitalarchive.handleurl", expectedConfig.handleURL)
        val receivedConfig = LoriConfiguration.load(
            "lori",
            ChainedKonfiguration(
                listOf(
                    SystemPropertiesKonfiguration(),
                )
            )
        )
        assertThat(receivedConfig, `is`(expectedConfig))
    }

    companion object {
        const val DATA_FOR_LORI_CONFIGURATION = "DATA_FOR_LORI_CONFIGURATION"
        val CONFIG =
            LoriConfiguration(
                grpcPort = 1234,
                httpPort = 45678,
                sqlUser = "user",
                sqlUrl = "someUrl",
                sqlPassword = "somePW",
                digitalArchiveAddress = "someAddress",
                digitalArchiveBasicAuth = "1234555nase",
                digitalArchiveUsername = "daUser",
                digitalArchivePassword = "daPW",
                jwtSecret = "jwtSecret",
                jwtAudience = "jwtAudience",
                jwtIssuer = "jwtIssuer",
                jwtRealm = "jwtRealm",
                duoSenderEntityId = "someId",
                sessionSignKey = "8BADF00DDEADBEAFDEADBAADDEADBAAD",
                sessionEncryptKey = "CAFEBABEDEADBEAFDEADBAADDEFEC8ED",
                stage = "dev",
                handleURL = "https://testdarch.zbw.eu/econis-archiv/handle/",
            )
    }
}
