package de.zbw.business.lori.server.mail

import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.Date
import java.util.Properties

/**
 * Service for sending mails.
 */
class MailService(
    val port: Int,
    val host: String,
    val fromEmail: String,
    private val useStartTls: Boolean = false, // STARTTLS (e.g. port 587)
    private val useSsl: Boolean = false, // Implicit SSL/SMTPS (e.g. port 465)
    private val auth: Boolean = false,
    private val username: String? = null,
    private val password: String? = null,
    private val connectionTimeoutMs: Int = 10_000,
    private val timeoutMs: Int = 10_000,
) {
    private val props: Properties =
        Properties().apply {
            put("mail.smtp.host", host)
            put("mail.smtp.port", port.toString())
            put("mail.smtp.auth", auth.toString())
            put("mail.smtp.starttls.enable", useStartTls.toString())
            put("mail.smtp.ssl.enable", useSsl.toString())
            put("mail.smtp.connectiontimeout", connectionTimeoutMs.toString())
            put("mail.smtp.timeout", timeoutMs.toString())
            put("mail.smtp.writetimeout", timeoutMs.toString())
        }

    private val transportProtocol = if (useSsl) "smtps" else "smtp"

    private val session: Session =
        if (auth && !username.isNullOrEmpty() && password != null) {
            Session.getInstance(
                props,
                object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication = PasswordAuthentication(username, password)
                },
            )
        } else {
            Session.getInstance(props)
        }

    @Throws(Exception::class)
    suspend fun sendMail(
        to: String,
        subject: String,
        body: String,
    ): Unit =
        withContext(Dispatchers.IO) {
            val msg =
                MimeMessage(session).apply {
                    setFrom(InternetAddress(fromEmail))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
                    this.subject = subject
                    setText(body)
                    sentDate = Date()
                }

            var transport: Transport? = null
            try {
                transport = session.getTransport(transportProtocol)
                if (auth && !username.isNullOrEmpty() && password != null) {
                    // explicit connect with credentials
                    transport.connect(host, port, username, password)
                } else {
                    // connect with no auth (server must accept it)
                    transport.connect()
                }
                transport.sendMessage(msg, msg.allRecipients)
            } finally {
                // always attempt to close the transport to release sockets
                try {
                    transport?.close()
                } catch (ignored: Exception) {
                    // ignore or log
                    LOG.error("Error closing transport when sending mail", ignored)
                }
            }
        }

    companion object {
        val LOG: Logger = LogManager.getLogger(MailService::class.java)
    }
}
