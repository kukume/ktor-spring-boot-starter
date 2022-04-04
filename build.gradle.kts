import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

val springBootVersion = "2.6.4"
val ktorVersion = "1.6.8"
val utilsVersion = "0.3.18"

plugins {
    val kotlinVersion = "1.6.20"
    kotlin("jvm") version kotlinVersion
    id("org.jetbrains.kotlin.kapt") version kotlinVersion
    `maven-publish`
    signing
}

group = "me.kuku"
version = "0.0.4"

repositories {
    maven("https://nexus.kuku.me/repository/maven-public/")
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    kapt("org.springframework.boot:spring-boot-configuration-processor:$springBootVersion")
    api("me.kuku:utils:$utilsVersion")
    api("org.springframework.boot:spring-boot-autoconfigure:$springBootVersion")
    api("io.ktor:ktor-server-core:$ktorVersion")
    api("io.ktor:ktor-freemarker:$ktorVersion")
    api("io.ktor:ktor-jackson:$ktorVersion")
    api("io.ktor:ktor-server-netty:$ktorVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
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
properties.load(File("nexus.properties").inputStream())

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

        maven {
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = properties.getProperty("sonatype.username")
                password = properties.getProperty("sonatype.password")
            }
        }
    }

    signing {
        sign(publishing.publications)
    }
}
