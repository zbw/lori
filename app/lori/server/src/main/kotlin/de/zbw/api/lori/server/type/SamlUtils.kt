package de.zbw.api.lori.server.type

import net.shibboleth.utilities.java.support.component.ComponentInitializationException
import net.shibboleth.utilities.java.support.resolver.CriteriaSet
import net.shibboleth.utilities.java.support.xml.BasicParserPool
import net.shibboleth.utilities.java.support.xml.ParserPool
import org.opensaml.core.config.ConfigurationService
import org.opensaml.core.config.InitializationService
import org.opensaml.core.criterion.EntityIdCriterion
import org.opensaml.core.xml.XMLObject
import org.opensaml.core.xml.config.XMLObjectProviderRegistry
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport
import org.opensaml.saml.common.xml.SAMLConstants
import org.opensaml.saml.criterion.EntityRoleCriterion
import org.opensaml.saml.criterion.ProtocolCriterion
import org.opensaml.saml.metadata.resolver.impl.AbstractReloadingMetadataResolver
import org.opensaml.saml.metadata.resolver.impl.PredicateRoleDescriptorResolver
import org.opensaml.saml.saml2.core.Assertion
import org.opensaml.saml.saml2.core.Response
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
    private val metadataPath: String,
    registry: XMLObjectProviderRegistry = XMLObjectProviderRegistry(),
) {
    init {
        ConfigurationService.register(
            XMLObjectProviderRegistry::class.java,
            registry,
        )
        registry.parserPool = getParserPool()
        InitializationService.initialize()
    }

    private var metadataCredentialResolver: MetadataCredentialResolver? = null

    // Get resolver to extract public key from metadata
    fun initMetadataResolver(metadataResolver: AbstractReloadingMetadataResolver) {
        metadataCredentialResolver = getMetadataCredentialResolver(metadataResolver)
    }

    private fun getParserPool(): ParserPool {
        val parserPool =
            BasicParserPool().apply {
                maxPoolSize = 100
                isCoalescing = true
                isIgnoreComments = true
                isIgnoreElementContentWhitespace = true
                isNamespaceAware = true
                isExpandEntityReferences = false
                isXincludeAware = false
            }

        val features: Map<String, Boolean> =
            mapOf(
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

    fun verifySignatureUsingSignatureValidator(signature: Signature) {
        // Set criterion to get relevant certificate
        val criteriaSet =
            CriteriaSet().apply {
                add(UsageCriterion(UsageType.SIGNING))
                add(EntityRoleCriterion(IDPSSODescriptor.DEFAULT_ELEMENT_NAME))
                add(ProtocolCriterion(SAMLConstants.SAML20P_NS))
                add(EntityIdCriterion(metadataPath))
            }

        // Resolve credential
        val credential =
            metadataCredentialResolver!!.resolveSingle(criteriaSet)
                ?: throw SecurityException("Criterion does not match entry in metadata file")

        // Verify signature format
        val profileValidator = SAMLSignatureProfileValidator()
        profileValidator.validate(signature)

        // Verify signature
        SignatureValidator.validate(signature, credential)
    }

    /**
     * This function does not work in a static context, although it seems like it.
     * Reason for this is that a registry needs to be instantiated.
     */
    fun unmarshallSAMLObject(responseMessage: String): Response? {
        try {
            val documentBuilderFactory = DocumentBuilderFactory.newInstance()
            documentBuilderFactory.isNamespaceAware = true
            val docBuilder = documentBuilderFactory.newDocumentBuilder()
            val document: Document = docBuilder.parse(ByteArrayInputStream(responseMessage.toByteArray()))
            val unmarshallerFactory = XMLObjectProviderRegistrySupport.getUnmarshallerFactory()
            val defaultElementName: QName = Response::class.java.getDeclaredField("DEFAULT_ELEMENT_NAME")[null] as QName
            val xmlObject =
                unmarshallerFactory.getUnmarshaller(defaultElementName)?.unmarshall(document.documentElement)
            return listOf(xmlObject).filterIsInstance<Response>().firstOrNull()
        } catch (e: Exception) {
            throw e
        }
    }

    private fun getMetadataCredentialResolver(metadataResolver: AbstractReloadingMetadataResolver): MetadataCredentialResolver {
        metadataResolver.setId(metadataResolver.javaClass.canonicalName)
        metadataResolver.parserPool = getParserPool()
        metadataResolver.initialize()
        val roleResolver = PredicateRoleDescriptorResolver(metadataResolver)
        val keyResolver =
            DefaultSecurityConfigurationBootstrap
                .buildBasicInlineKeyInfoCredentialResolver()
        val metadataCredentialResolver = MetadataCredentialResolver()
        metadataCredentialResolver.keyInfoCredentialResolver = keyResolver
        metadataCredentialResolver.roleDescriptorResolver = roleResolver
        metadataCredentialResolver.initialize()
        roleResolver.initialize()
        return metadataCredentialResolver
    }

    companion object {
        fun decodeSAMLResponse(message: String): String =
            message
                .substringAfter("SAMLResponse=")
                .let { URLDecoder.decode(it, "UTF-8") }
                .let { String(Base64.getDecoder().decode(it)) }

        fun getAttributeValuesByName(
            assertion: Assertion,
            attributeName: String,
        ): List<XMLObject> =
            assertion.attributeStatements
                .flatMap { attrStmt ->
                    val attributes = attrStmt.attributes.filter { attr -> attr.name == attributeName }
                    attributes.map { it.attributeValues }
                }.flatten()
    }
}
