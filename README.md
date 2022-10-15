## ktor-spring-boot-starter

ktor：https://ktor.io/

整合`Ktor`并提供一些扩展函数，版本号前缀与ktor官方一致


### 使用

#### 引入（gradle.kts）

引入

```kotlin
repositories {
    maven("https://nexus.kuku.me/repository/maven-public/")
    mavenCentral()
}


implementation("me.kuku:ktor-spring-boot-starter:2.1.2.2")
```

#### Routing

```kotlin

@Component
class KtorRouting {
    
    fun Routing.ro() {
        route("") {
            get {
                call.respond("Hello World")
            }
        }
    }
}

```

#### Module（异常处理）

```kotlin
@Component
class KtorModule {
    
    fun Application.statusPage() {
        install(StatusPages) {
            exception<Throwable> { cause ->
                call.respond(HttpStatusCode.InternalServerError, Result.failure(cause.message ?: "服务器内部错误", null))
            }
        }
    }
}
```

#### 配置Jackson

```kotlin
@Component
class MyJackson: JacksonConfiguration {
    // 实现其方法（函数）
}
```