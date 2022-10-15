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

fun List<PartData>.getAsFormItem(name: String) = this.find { it.name == name } as? PartData.FormItem

fun List<PartData>.getAllAsFormItem(name: String) = this.filter { it.name == name }.map { it as? PartData.FormItem? ?: error("${it.name} can not convert PartData.FormItem") }

fun List<PartData>.getAsFormItemOrFail(name: String) = getAsFormItem(name) ?: throw MissingRequestParameterException(name)

fun List<PartData>.getAsFileItem(name: String) = this.find { it.name == name } as? PartData.FileItem

fun List<PartData>.getAllAsFileItem(name: String) = this.filter { it.name == name }.map { it as?PartData.FileItem ?: error("${it.name} can not convert PartData.FileItem") }

fun List<PartData>.getAsFileItemOrFail(name: String) = getAsFileItem(name) ?: throw MissingRequestParameterException(name)

fun List<PartData>.get(name: String) = this.find { it.name == name }

fun List<PartData>.getOrFail(name: String) = this.find { it.name == name } ?: throw MissingRequestParameterException(name)

fun List<PartData>.getAll(name: String) = this.filter { it.name == name }

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
    val b = ip.isNullOrEmpty() || "unknown".equals(ip, true)
    return !b
}