package me.kuku.ktor.config

import io.ktor.server.application.Application
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import me.kuku.ktor.plugins.module
import me.kuku.ktor.pojo.KtorConfig
import me.kuku.ktor.pojo.ThymeleafConfig
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredMemberExtensionFunctions
import kotlin.reflect.full.extensionReceiverParameter

//@Configuration
@AutoConfiguration
@EnableConfigurationProperties(KtorConfig::class, ThymeleafConfig::class)
@org.springframework.context.annotation.Lazy(false)
open class KtorAutoConfiguration(
    private val ktorConfig: KtorConfig,
    private val applicationContext: ApplicationContext
) {

    @Bean
    open fun applicationEngine(): ApplicationEngine {
        return embeddedServer(Netty, port = ktorConfig.port, host = ktorConfig.host, module = { init(applicationContext) }).start()
    }
}

internal data class KtorExec(val any: Any, val function: KFunction<*>)

fun Application.init(applicationContext: ApplicationContext) {
    val applicationKtor = mutableListOf<KtorExec>()
    val routingKtor = mutableListOf<KtorExec>()
    val names = applicationContext.beanDefinitionNames
    val clazzList = mutableListOf<Class<*>>()
    for (name in names) {
        applicationContext.getType(name)?.let {
            clazzList.add(it)
        }
    }
    for (clazz in clazzList) {
        val functions = kotlin.runCatching {
            clazz.kotlin.declaredMemberExtensionFunctions
        }.getOrNull() ?: continue
        for (function in functions) {
            if (function.extensionReceiverParameter?.type?.toString() == "io.ktor.server.application.Application") {
                applicationKtor.add(KtorExec(applicationContext.getBean(clazz), function))
            }
            if (function.extensionReceiverParameter?.type?.toString() == "io.ktor.server.routing.Routing") {
                routingKtor.add(KtorExec(applicationContext.getBean(clazz), function))
            }
        }
    }
    module(applicationContext)
    for (ktorExec in applicationKtor) {
        ktorExec.function.call(ktorExec.any, this)
    }

    routing {
        for (ktorExec in routingKtor) {
            ktorExec.function.call(ktorExec.any, this)
        }
    }
}
