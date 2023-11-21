package de.zbw.api.lori.server.type

import net.shibboleth.utilities.java.support.component.ComponentInitializationException
import net.shibboleth.utilities.java.support.xml.BasicParserPool
import net.shibboleth.utilities.java.support.xml.ParserPool
import org.opensaml.core.config.ConfigurationService
import org.opensaml.core.config.InitializationService
import org.opensaml.core.xml.config.XMLObjectProviderRegistry
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport
import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import javax.xml.namespace.QName
import javax.xml.parsers.DocumentBuilderFactory

class SamlUtils(
    registry: XMLObjectProviderRegistry = XMLObjectProviderRegistry()
) {
    init {
        ConfigurationService.register(
            XMLObjectProviderRegistry::class.java, registry
        )
        registry.parserPool = getParserPool()
        InitializationService.initialize()
    }

    private fun getParserPool(): ParserPool {
        val parserPool = BasicParserPool()
        parserPool.maxPoolSize = 100
        parserPool.isCoalescing = true
        parserPool.isIgnoreComments = true
        parserPool.isIgnoreElementContentWhitespace = true
        parserPool.isNamespaceAware = true
        parserPool.isExpandEntityReferences = false
        parserPool.isXincludeAware = false

        val features: MutableMap<String, Boolean> = HashMap()
        features["http://xml.org/sax/features/external-general-entities"] = false
        features["http://xml.org/sax/features/external-parameter-entities"] = false
        features["http://apache.org/xml/features/disallow-doctype-decl"] = true
        features["http://apache.org/xml/features/validation/schema/normalized-value"] = false
        features["http://javax.xml.XMLConstants/feature/secure-processing"] = true
        parserPool.setBuilderFeatures(features.toMap())
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

    inline fun <reified T : Any> buildSAMLObject(clazz: Class<T>): T? =
        try {
            val builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory()
            val defaultElementName: QName = clazz.getDeclaredField("DEFAULT_ELEMENT_NAME")[null] as QName
            val xmlObject = builderFactory.getBuilder(defaultElementName)?.buildObject(defaultElementName)
            listOf(xmlObject).filterIsInstance<T>().firstOrNull()
        } catch (e: IllegalAccessException) {
            throw IllegalArgumentException("Could not create SAML object")
        } catch (e: NoSuchFieldException) {
            throw IllegalArgumentException("Could not create SAML object")
        }
}
