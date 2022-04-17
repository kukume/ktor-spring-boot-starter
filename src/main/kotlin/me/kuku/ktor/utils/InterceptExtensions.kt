package me.kuku.ktor.utils

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.util.pipeline.*

fun PipelineContext<*, ApplicationCall>.except(list: List<String>): Boolean {
    val request = call.request
    val path = request.path()
    val method = request.httpMethod
    for (s in list) {
        val sss = s.split("|")
        val pathParam = sss[0]
        val methodParam = if (sss.size == 1) "get" else sss[1]
        if ((pathParam == path) && method.value == methodParam.uppercase()) return false
    }
    return true
}

fun PipelineContext<*, ApplicationCall>.only(list: List<String>): Boolean {
    val request = call.request
    val path = request.path()
    val method = request.httpMethod
    for (s in list) {
        val sss = s.split("|")
        val pathParam = sss[0]
        val methodParam = if (sss.size == 1) "get" else sss[1]
        if ((pathParam == path) && method.value == methodParam.uppercase()) return true
    }
    return false
}

suspend fun PipelineContext<*, ApplicationCall>.except(block: suspend InterceptQList.() -> Unit) {
    val list = InterceptQList()
    block.invoke(list)
    for (interceptQ in list.interceptQs) {
        except(interceptQ)
    }
}

suspend fun PipelineContext<*, ApplicationCall>.only(block: suspend InterceptQList.() -> Unit) {
    val list = InterceptQList()
    block.invoke(list)
    for (interceptQ in list.interceptQs) {
        only(interceptQ)
    }
}

class InterceptQList {

    val interceptQs = mutableListOf<InterceptQ>()

    fun buildIntercept(body: InterceptQ.() -> Unit) =
        interceptQs.add(InterceptQ().apply(body))

    fun buildIntercept(interceptPath: InterceptPath, block: suspend InterceptQ.() -> Unit) =
        interceptQs.add(InterceptQ().add(interceptPath).exec(block))

    fun buildIntercept(path: String, method: String, block: suspend InterceptQ.() -> Unit) =
        interceptQs.add(InterceptQ().add(path, method).exec(block))

    fun buildIntercept(path: String, method: HttpMethod, block: suspend InterceptQ.() -> Unit) =
        interceptQs.add(InterceptQ().add(path, method).exec(block))

}

data class InterceptPath(
    val path: String, val method: HttpMethod
)

class InterceptQ {

    val list = mutableListOf<InterceptPath>()
    val execSuspendList = mutableListOf<suspend InterceptQ.() -> Unit>()

    fun add(path: String, method: HttpMethod): InterceptQ {
        list.add(InterceptPath(path, method))
        return this
    }

    fun add(path: String, method: String): InterceptQ {
        val httpMethod = HttpMethod.parse(method.uppercase())
        add(path, httpMethod)
        return this
    }

    fun add(interceptPath: InterceptPath): InterceptQ {
        list.add(interceptPath)
        return this
    }

    fun exec(block: suspend InterceptQ.() -> Unit): InterceptQ {
        execSuspendList.add(block)
        return this
    }

    suspend fun only(context: PipelineContext<*, ApplicationCall>) {
        context.only(this)
    }

    suspend fun except(context: PipelineContext<*, ApplicationCall>) {
        context.except(this)
    }
}

fun buildIntercept(body: InterceptQ.() -> Unit) = InterceptQ().apply(body)

fun buildIntercept(interceptPath: InterceptPath, block: suspend InterceptQ.() -> Unit) = InterceptQ().add(interceptPath).exec(block)

fun buildIntercept(path: String, method: String, block: suspend InterceptQ.() -> Unit) = InterceptQ().add(path, method).exec(block)

fun buildIntercept(path: String, method: HttpMethod, block: suspend InterceptQ.() -> Unit) = InterceptQ().add(path, method).exec(block)

suspend fun PipelineContext<*, ApplicationCall>.only(interceptQ: InterceptQ) {
    val list = interceptQ.list
    val request = call.request
    val path = request.path()
    val method = request.httpMethod
    var b = false
    for (InterceptPath in list) {
        if (InterceptPath.path == path && method == InterceptPath.method) b = true
    }
    if (b) {
        interceptQ.execSuspendList.forEach { it.invoke(interceptQ) }
    }
}

suspend fun PipelineContext<*, ApplicationCall>.except(interceptQ: InterceptQ) {
    val list = interceptQ.list
    val request = call.request
    val path = request.path()
    val method = request.httpMethod
    var b = true
    for (InterceptPath in list) {
        if (InterceptPath.path == path && method == InterceptPath.method) b = false
    }
    if (b) {
        interceptQ.execSuspendList.forEach { it.invoke(interceptQ) }
    }
}