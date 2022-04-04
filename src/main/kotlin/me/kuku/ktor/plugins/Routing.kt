package me.kuku.ktor.plugins

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import com.alibaba.fastjson.TypeReference
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.util.*

val ApplicationCall.queryParameters: Parameters
    get() = this.request.queryParameters

suspend fun ApplicationCall.receiveJSONObject(): JSONObject =
    JSON.parseObject(this.receiveText())

suspend fun ApplicationCall.receiveJSONArray(): JSONArray =
    JSON.parseArray(this.receiveText())

fun ApplicationCall.propertyOrNull(path: String) =
    this.application.environment.config.propertyOrNull(path)

fun ApplicationCall.property(path: String) =
    this.application.environment.config.property(path)

val ApplicationCall.environment: ApplicationEnvironment
    get() = this.application.environment

inline fun <reified T : Any> Parameters.receive(): T {
    val map = mutableMapOf<String, Any>()
    this.names().forEach { map[it] = this.getOrFail(it) }
    val jsonStr = JSON.toJSONString(map)
    return JSON.parseObject(jsonStr, object: TypeReference<T>() {})
}


class MultiPart {
    private val map = mutableMapOf<String, PartData>()

    fun put(name: String, partData: PartData) {
        map[name] = partData
    }

    operator fun set(name: String, partData: PartData) {
        map[name] = partData
    }

    fun get(name: String) = map[name]

    fun getOrFail(name: String) = map[name] ?: throw MissingRequestParameterException(name)

    fun getValue(name: String) = map[name]?.value

    fun getValueOrFail(name: String) = map[name]?.value ?: throw MissingRequestParameterException(name)

    fun getValueOrDefault(name: String, defaultValue: String) = map[name]?.value ?: defaultValue

    fun getFile(name: String) = map[name]?.fileItem

    fun getFileOrFail(name: String) = map[name]?.fileItem ?: throw MissingRequestParameterException(name)

    private val PartData.value: String
        get() {
            val formItem = this as PartData.FormItem
            return formItem.value
        }

    private val PartData.fileItem: PartData.FileItem
        get() = this as PartData.FileItem


}

suspend fun ApplicationCall.multipart(): MultiPart = MultiPart().apply {
    this@multipart.receiveMultipart().forEachPart { p -> this[p.name!!] = p }
}

fun ApplicationRequest.ip(): String? {
    val headers = this.headers
    var ip = headers["x-forwarded-for"]?.split(",")?.get(0)
    if (!ipOk(ip)) {
        ip = headers["Proxy-Client-IP"]?.split(",")?.get(0)
        if (!ipOk(ip)) {
            ip = headers["WL-Proxy-Client-IP"]?.split(",")?.get(0)
            if (!ipOk(ip)) {
                ip = headers["HTTP_CLIENT_IP"]?.split(",")?.get(0)
                if (!ipOk(ip)) {
                    ip = headers["HTTP_X_FORWARDED_FOR"]?.split(",")?.get(0)
                    if (!ipOk(ip)) {
                        ip = headers["X-Real-IP"]?.split(",")?.get(0)
                        if (!ipOk(ip)) {
                            ip = this.local.remoteHost
                        }
                    }
                }
            }
        }
    }
    return ip
}

private fun ipOk(ip: String?): Boolean {
    val b = ip == null || ip.isEmpty() || "unknown".equals(ip, true)
    return !b
}