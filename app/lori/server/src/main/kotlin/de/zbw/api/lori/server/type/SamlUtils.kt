package de.zbw.api.lori.server.type

import net.shibboleth.utilities.java.support.component.ComponentInitializationException
import net.shibboleth.utilities.java.support.resolver.CriteriaSet
import net.shibboleth.utilities.java.support.xml.BasicParserPool
import net.shibboleth.utilities.java.support.xml.ParserPool
import org.opensaml.core.config.ConfigurationService
import org.opensaml.core.config.InitializationService
import org.opensaml.core.criterion.EntityIdCriterion
import org.opensaml.core.xml.config.XMLObjectProviderRegistry
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport
import org.opensaml.saml.common.xml.SAMLConstants
import org.opensaml.saml.criterion.EntityRoleCriterion
import org.opensaml.saml.criterion.ProtocolCriterion
import org.opensaml.saml.metadata.resolver.impl.FilesystemMetadataResolver
import org.opensaml.saml.metadata.resolver.impl.PredicateRoleDescriptorResolver
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor
import org.opensaml.saml.security.impl.MetadataCredentialResolver
import org.opensaml.saml.security.impl.SAMLSignatureProfileValidator
import org.opensaml.security.credential.UsageType
import org.opensaml.security.criteria.UsageCriterion
import org.opensaml.xmlsec.config.impl.DefaultSecurityConfigurationBootstrap
import org.opensaml.xmlsec.signature.Signature
import org.opensaml.xmlsec.signature.support.SignatureValidator
import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileNotFoundException
import java.net.URLDecoder
import java.util.Base64
import javax.xml.namespace.QName
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Utils for handling messages using the SAML2.0 protocol.
 *
 * Created on 11-21-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class SamlUtils(
    private val senderEntityId: String,
    registry: XMLObjectProviderRegistry = XMLObjectProviderRegistry(),
) {
    init {
        ConfigurationService.register(
            XMLObjectProviderRegistry::class.java, registry
        )
        registry.parserPool = getParserPool()
        InitializationService.initialize()
    }

    // Get resolver to extract public key from metadata
    private val metadataCredentialResolver: MetadataCredentialResolver = getMetadataCredentialResolver()

    private fun getParserPool(): ParserPool {
        val parserPool = BasicParserPool().apply {
            maxPoolSize = 100
            isCoalescing = true
            isIgnoreComments = true
            isIgnoreElementContentWhitespace = true
            isNamespaceAware = true
            isExpandEntityReferences = false
            isXincludeAware = false
        }

        val features: Map<String, Boolean> = mapOf(
            "http://xml.org/sax/features/external-general-entities" to false,
            "http://xml.org/sax/features/external-parameter-entities" to false,
            "http://apache.org/xml/features/disallow-doctype-decl" to true,
            "http://apache.org/xml/features/validation/schema/normalized-value" to false,
            "http://javax.xml.XMLConstants/feature/secure-processing" to true,
        )
        parserPool.setBuilderFeatures(features)
        parserPool.setBuilderAttributes(HashMap())
        try {
            parserPool.initialize()
        } catch (e: ComponentInitializationException) {
            throw e
        }
        return parserPool
    }

    inline fun <reified T : Any> unmarshallSAMLObject(clazz: Class<T>, responseMessage: String): T? {
        try {
            val documentBuilderFactory = DocumentBuilderFactory.newInstance()
            documentBuilderFactory.isNamespaceAware = true
            val docBuilder = documentBuilderFactory.newDocumentBuilder()
            val document: Document = docBuilder.parse(ByteArrayInputStream(responseMessage.toByteArray()))
            val unmarshallerFactory = XMLObjectProviderRegistrySupport.getUnmarshallerFactory()
            val defaultElementName: QName = clazz.getDeclaredField("DEFAULT_ELEMENT_NAME")[null] as QName
            val xmlObject =
                unmarshallerFactory.getUnmarshaller(defaultElementName)?.unmarshall(document.documentElement)
            return listOf(xmlObject).filterIsInstance<T>().firstOrNull()
        } catch (e: Exception) {
            throw e
        }
    }

    fun verifySignatureUsingSignatureValidator(signature: Signature) {
        // Set criterion to get relevant certificate
        val criteriaSet = CriteriaSet().apply {
            add(UsageCriterion(UsageType.SIGNING))
            add(EntityRoleCriterion(IDPSSODescriptor.DEFAULT_ELEMENT_NAME))
            add(ProtocolCriterion(SAMLConstants.SAML20P_NS))
            add(EntityIdCriterion(senderEntityId))
        }

        // Resolve credential
        val credential = metadataCredentialResolver.resolveSingle(criteriaSet)
            ?: throw SecurityException("Criterion does not match entry in metadata file")

        // Verify signature format
        val profileValidator = SAMLSignatureProfileValidator()
        profileValidator.validate(signature)

        // Verify signature
        SignatureValidator.validate(signature, credential)
    }

    private fun getMetadataCredentialResolver(): MetadataCredentialResolver {
        val metadataCredentialResolver = MetadataCredentialResolver()
        val metadataFile = javaClass.classLoader.getResource(SENDER_METADATA_PATH)
            ?.toURI()
            ?.let { File(it) }
            ?: throw FileNotFoundException("Metadata file was not found under path $SENDER_METADATA_PATH")
        val metadataResolver = FilesystemMetadataResolver(metadataFile)
        metadataResolver.setId(metadataResolver.javaClass.canonicalName)
        metadataResolver.parserPool = getParserPool()
        metadataResolver.initialize()
        val roleResolver = PredicateRoleDescriptorResolver(metadataResolver)
        val keyResolver = DefaultSecurityConfigurationBootstrap
            .buildBasicInlineKeyInfoCredentialResolver()
        metadataCredentialResolver.keyInfoCredentialResolver = keyResolver
        metadataCredentialResolver.roleDescriptorResolver = roleResolver
        metadataCredentialResolver.initialize()
        roleResolver.initialize()
        return metadataCredentialResolver
    }

    companion object {
        const val SENDER_METADATA_PATH = "saml/metadata.xml"

        fun decodeSAMLResponse(message: String): String =
            message
                .substringAfter("SAMLResponse=")
                .let { URLDecoder.decode(it, "UTF-8") }
                .let { String(Base64.getDecoder().decode(it)) }
    }
}
