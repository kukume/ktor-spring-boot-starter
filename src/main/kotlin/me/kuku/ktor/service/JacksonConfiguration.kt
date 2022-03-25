package me.kuku.ktor.service

import com.fasterxml.jackson.databind.ObjectMapper

interface JacksonConfiguration {

    fun configuration(objectMapper: ObjectMapper)

}