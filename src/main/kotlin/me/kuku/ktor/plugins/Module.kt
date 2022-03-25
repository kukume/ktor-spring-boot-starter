package me.kuku.ktor.plugins

import com.fasterxml.jackson.databind.SerializationFeature
import freemarker.cache.ClassTemplateLoader
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.freemarker.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.util.pipeline.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.kuku.ktor.service.JacksonConfiguration
import me.kuku.utils.JacksonUtils
import me.kuku.utils.toUrlDecode
import org.springframework.context.ApplicationContext
import kotlin.reflect.jvm.jvmErasure

fun Application.module(applicationContext: ApplicationContext) {
    install(DefaultHeaders)
    install(CallLogging)

    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
    }

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT, SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL)
            kotlin.runCatching {
                val bean = applicationContext.getBean(JacksonConfiguration::class.java)
                bean.configuration(this)
            }
        }
        register(ContentType.Application.FormUrlEncoded, FormUrlEncodedConverter())
    }
}

class FormUrlEncodedConverter: ContentConverter {
    override suspend fun convertForReceive(context: PipelineContext<ApplicationReceiveRequest, ApplicationCall>): Any? {
        val request = context.subject
        val type = request.typeInfo
        val value = request.value as? ByteReadChannel ?: return null
        return withContext(Dispatchers.IO) {
            val reader = value.toInputStream().reader(context.call.request.contentCharset() ?: Charsets.UTF_8)
            val body = reader.readLines().joinToString("")
            val javaObjectType = type.jvmErasure.javaObjectType
            if (body.isEmpty()) {
                val constructor = javaObjectType.getDeclaredConstructor()
                constructor.newInstance()
            } else {
                val objectNode = JacksonUtils.createObjectNode()
                body.split("&").forEach {
                    val arr = it.split("=")
                    val k = arr[0]
                    val v = arr[1].toUrlDecode()
                    objectNode.put(k, v)
                }
                JacksonUtils.parseObject(JacksonUtils.toJsonString(objectNode), javaObjectType)
            }
        }
    }

    override suspend fun convertForSend(
        context: PipelineContext<Any, ApplicationCall>,
        contentType: ContentType,
        value: Any
    ): Any {
        val jsonNode = JacksonUtils.parse(JacksonUtils.toJsonString(value))
        val sb = StringBuilder()
        jsonNode.fieldNames().forEach {
            sb.append(it).append(jsonNode.get(it)).append("&")
        }
        return sb.removeSuffix("&").toString()
    }
}