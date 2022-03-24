package me.kuku.ktor.config

import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import me.kuku.ktor.plugins.module
import me.kuku.ktor.plugins.routes
import me.kuku.ktor.pojo.KtorConfig
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Service
import kotlin.reflect.full.declaredMemberExtensionFunctions
import kotlin.reflect.full.extensionReceiverParameter

@Configuration
@EnableConfigurationProperties(KtorConfig::class)
open class KtorAutoConfiguration(
    private val ktorConfig: KtorConfig,
    private val applicationContext: ApplicationContext
) {

    @Bean
    open fun applicationEngine(): ApplicationEngine {
        return embeddedServer(Netty, port = ktorConfig.port, host = ktorConfig.host) {
            module()
            routes()
            val names = applicationContext.beanDefinitionNames
            val clazzList = mutableListOf<Class<*>>()
            for (name in names) {
                val clazz = applicationContext.getType(name)
                if (clazz?.isAnnotationPresent(Service::class.java) == true || clazz?.isAnnotationPresent(Component::class.java) == true ||
                        clazz?.isAnnotationPresent(Controller::class.java) == true) {
                    clazzList.add(clazz)
                }
            }
            routing {
                for (clazz in clazzList) {
                    kotlin.runCatching {
                        val functions = clazz.kotlin.declaredMemberExtensionFunctions
                        for (function in functions) {
                            if (function.extensionReceiverParameter?.type?.toString() == "io.ktor.routing.Routing") {
                                function.call(applicationContext.getBean(clazz), this)
                            }
                        }
                    }
                }
            }
            for (clazz in clazzList) {
                kotlin.runCatching {
                    val functions = clazz.kotlin.declaredMemberExtensionFunctions
                    for (function in functions) {
                        if (function.extensionReceiverParameter?.type?.toString() == "io.ktor.application.Application") {
                            function.call(applicationContext.getBean(clazz), this)
                        }
                    }
                }
            }
        }.start()
    }
}