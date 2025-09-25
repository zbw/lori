package de.zbw.api.lori.server.watchdog

import de.zbw.api.lori.server.config.LoriConfiguration
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.mail.MailService
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class JobWatchdog(
    private val mailService: MailService,
    private val config: LoriConfiguration,
    private val backend: LoriServerBackend,
) {
    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    fun start() {
        val now = LocalDateTime.now()
        val firstRun =
            now
                .withHour(7)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
        val initialDelay =
            Duration.between(now, firstRun).toMillis().let {
                if (it < 0) it + Duration.ofDays(1).toMillis() else it
            }

        scheduler.scheduleAtFixedRate(
            { checkJobs() },
            initialDelay,
            TimeUnit.DAYS.toMillis(1),
            TimeUnit.MILLISECONDS,
        )
    }

    private fun checkJobs() {
        try {
            val missingJobs =
                runBlocking {
                    backend.findMissingJobs()
                }
            if (missingJobs.isNotEmpty()) {
                runBlocking {
                    mailService.sendMail(
                        to = config.mailTo,
                        subject = "Lori-Job Error",
                        body =
                            "Folgende Jobs sind wider Erwarten nicht gelaufen: ${missingJobs.joinToString(", ")}\n" +
                                "Bitte kontaktieren Sie den Systembesitzer.",
                    )
                }
            }
        } catch (e: Exception) {
            try {
                runBlocking {
                    mailService.sendMail(
                        to = config.mailTo,
                        subject = "Lori-Job Fehler",
                        body =
                            "Der Watchdog von Lori ist fehlgeschlagen: ${e.message}" +
                                "Bitte kontaktieren Sie den Systembesitzer.",
                    )
                }
            } catch (ignored: Exception) {
                LOG.error("Error when sending a mail", ignored)
            }
        }
    }

    companion object {
        val LOG: Logger = LogManager.getLogger(MailService::class.java)
    }
}
