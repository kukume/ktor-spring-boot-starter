@file:Suppress("VulnerableLibrariesLocal")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

val springBootVersion = "2.7.11"
val ktorVersion = "2.3.0"

plugins {
    val kotlinVersion = "1.8.20"
    kotlin("jvm") version kotlinVersion
    id("org.jetbrains.kotlin.kapt") version kotlinVersion
    `maven-publish`
    signing
}

group = "me.kuku"
version = "2.3.0.0"

repositories {
    maven("https://nexus.kuku.me/repository/maven-public/")
    mavenCentral()
}

dependencies {
    kapt("org.springframework.boot:spring-boot-configuration-processor:$springBootVersion")
    api("org.springframework.boot:spring-boot-autoconfigure:$springBootVersion")
    compileOnly("org.springframework.data:spring-data-commons:$springBootVersion")
    api("org.springframework.boot:spring-boot-starter-json:$springBootVersion")
    api("io.ktor:ktor-server-core-jvm:$ktorVersion")
    api("io.ktor:ktor-server-thymeleaf:$ktorVersion")
    api("io.ktor:ktor-server-status-pages:$ktorVersion")
    api("io.ktor:ktor-server-double-receive:$ktorVersion")
    api("io.ktor:ktor-server-default-headers-jvm:$ktorVersion")
    api("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    api("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    api("io.ktor:ktor-serialization-jackson-jvm:$ktorVersion")
    api("io.ktor:ktor-server-websockets:$ktorVersion")
//    api("io.ktor:ktor-server-cio-jvm:$ktorVersion")
    api("io.ktor:ktor-server-netty-jvm:$ktorVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xcontext-receivers")
        jvmTarget = "1.8"
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    from(sourceSets["main"].allSource)
    archiveClassifier.set("sources")
}

val docJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

val properties = Properties()
properties.load(File("publish.properties").inputStream())
ext.set("signing.keyId", properties.getProperty("signing.keyId"))
ext.set("signing.password", properties.getProperty("signing.password"))
ext.set("signing.secretKeyRingFile", properties.getProperty("signing.secretKeyRingFile"))

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifact(sourcesJar)
            artifact(docJar)
            artifactId = "ktor-spring-boot-starter"
            pom {
                name.set("ktor-spring-boot-starter")
                description.set("ktor-spring-boot-starter")
                url.set("https://github.com/kukume/ktor-spring-boot-starter")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("kuku")
                        name.set("kuku")
                        email.set("kuku@kuku.me")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/kukume")
                    developerConnection.set("scm:git:ssh://github.com/kukume")
                    url.set("https://github.com/kukume")
                }
            }
            from(components["java"])
        }
    }

    repositories {
        maven {
            url = uri("https://nexus.kuku.me/repository/maven-releases/")
            credentials {
                username = properties.getProperty("kuku.username")
                password = properties.getProperty("kuku.password")
            }
        }

        if (properties.getProperty("sonatype.username") != null && properties.getProperty("sonatype.username").isNotEmpty()) {
            maven {
                url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = properties.getProperty("sonatype.username")
                    password = properties.getProperty("sonatype.password")
                }
            }
        }
    }

    if (properties.getProperty("signing.keyId") != null && properties.getProperty("signing.keyId").isNotEmpty()) {
        signing {
            sign(publishing.publications)
        }
    }
}
