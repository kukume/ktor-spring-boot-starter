package me.kuku.ktor.plugins

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.thymeleaf.*
import io.ktor.server.websocket.*
import io.ktor.util.reflect.*
import io.ktor.utils.io.*
import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.kuku.ktor.pojo.ThymeleafConfig
import org.springframework.context.ApplicationContext
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import java.net.URLDecoder
import java.time.Duration

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

    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(PrivateInnerRouting.objectMapper, true))
        register(ContentType.Application.FormUrlEncoded, FormUrlEncodedConverter(PrivateInnerRouting.objectMapper))
    }

    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
        contentConverter = JacksonWebsocketContentConverter(PrivateInnerRouting.objectMapper)
    }
}

class FormUrlEncodedConverter(private val objectMapper: ObjectMapper) : ContentConverter {

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
                    val v = URLDecoder.decode(arr[1], "utf-8")
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
