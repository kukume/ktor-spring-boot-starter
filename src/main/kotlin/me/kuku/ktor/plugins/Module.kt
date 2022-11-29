package me.kuku.ktor.plugins

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.thymeleaf.*
import io.ktor.util.reflect.*
import io.ktor.utils.io.*
import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.kuku.ktor.pojo.KtorConfig
import me.kuku.ktor.pojo.ThymeleafConfig
import me.kuku.ktor.service.JacksonConfiguration
import me.kuku.utils.Jackson
import me.kuku.utils.toUrlDecode
import org.springframework.context.ApplicationContext
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver

fun Application.module(applicationContext: ApplicationContext) {

    val thymeleafConfig = applicationContext.getBean(ThymeleafConfig::class.java)

    install(DefaultHeaders)
    install(CallLogging)

    install(Thymeleaf) {
        setTemplateResolver(ClassLoaderTemplateResolver().apply {
            prefix = "templates/"
            suffix = ".html"
            characterEncoding = "utf-8"
            isCacheable = thymeleafConfig.cache
        })
    }

    val jacksonConfiguration = kotlin.runCatching {
        applicationContext.getBean(JacksonConfiguration::class.java)
    }.getOrNull()

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL)
            jacksonConfiguration?.configuration(this)
        }
        register(ContentType.Application.FormUrlEncoded, FormUrlEncodedConverter())
    }
}

class FormUrlEncodedConverter(private val objectMapper: ObjectMapper = Jackson.objectMapper): ContentConverter {

    override suspend fun serializeNullable(
        contentType: ContentType,
        charset: Charset,
        typeInfo: TypeInfo,
        value: Any?
    ): OutgoingContent {
        return OutputStreamContent(
            {
                val jsonNode = objectMapper.readTree(objectMapper.writeValueAsString(value))
                val sb = StringBuilder()
                jsonNode.fieldNames().forEach {
                    sb.append(it).append(jsonNode.get(it)).append("&")
                }
                objectMapper.writeValue(this, sb.removeSuffix("&").toString())
            },
            contentType.withCharsetIfNeeded(charset)
        )
    }

    override suspend fun deserialize(charset: Charset, typeInfo: TypeInfo, content: ByteReadChannel): Any? {
        try {
            return withContext(Dispatchers.IO) {
                val reader = content.toInputStream().reader(charset)
                val body = reader.readText()
                val objectNode = objectMapper.createObjectNode()
                body.split("&").forEach {
                    val arr = it.split("=")
                    val k = arr[0]
                    val v = arr[1].toUrlDecode()
                    objectNode.put(k, v)
                }
                objectMapper.treeToValue(objectNode, objectMapper.constructType(typeInfo.reifiedType))
            }
        } catch (deserializeFailure: Exception) {
            val convertException = JsonConvertException("Illegal json parameter found", deserializeFailure)

            when (deserializeFailure) {
                is JsonParseException -> throw convertException
                is JsonMappingException -> throw convertException
                else -> throw deserializeFailure
            }
        }
    }
}