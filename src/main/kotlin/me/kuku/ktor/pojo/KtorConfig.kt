package me.kuku.ktor.pojo

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "spring.ktor")
open class KtorConfig {

    open var host: String = "0.0.0.0"

    open var port: Int = 8080



}