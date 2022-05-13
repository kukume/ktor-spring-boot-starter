package me.kuku.ktor.plugins

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.util.*
import me.kuku.utils.Jackson
import me.kuku.utils.toJsonNode
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

suspend fun ApplicationCall.receiveJsonNode(): JsonNode =
    this.receiveText().toJsonNode()

fun ApplicationCall.propertyOrNull(path: String) =
    this.application.environment.config.propertyOrNull(path)

fun ApplicationCall.property(path: String) =
    this.application.environment.config.property(path)

inline fun <reified T : Any> Parameters.receive(): T {
    val map = mutableMapOf<String, Any>()
    this.names().forEach { map[it] = this.getOrFail(it) }
    return Jackson.parseObject(Jackson.toJsonString(map))
}

fun Parameters.pageable(): Pageable {
    val page = this["page"]?.toIntOrNull() ?: 0
    val size = this["size"]?.toIntOrNull() ?: 20
    var sortObj = Sort.unsorted()
    val sort = this.getAll("sort")
    if (sort != null) {
        val orderList = mutableListOf<Sort.Order>()
        sort.forEach {
            val arr = it.split(",").toMutableList()
            val dire = when (arr.last().lowercase()) {
                "asc" -> {
                    arr.removeLast()
                    Sort.Direction.ASC
                }
                "desc" -> {
                    arr.removeLast()
                    Sort.Direction.DESC
                }
                else -> Sort.Direction.ASC
            }
            for (s in arr) {
                orderList.add(if (dire == Sort.Direction.ASC) Sort.Order.asc(s) else Sort.Order.desc(s))
            }
        }
        sortObj = Sort.by(orderList)
    }
    return PageRequest.of(page, size, sortObj)
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

fun ApplicationRequest.ip(): String {
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
    return ip!!
}

private fun ipOk(ip: String?): Boolean {
    val b = ip == null || ip.isEmpty() || "unknown".equals(ip, true)
    return !b
}