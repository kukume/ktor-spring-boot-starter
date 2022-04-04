## ktor-spring-boot-starter

ktor：https://ktor.io/

整合`Ktor`并提供一些方便的扩展函数


### 使用

#### 引入（gradle.kts）

使用我的仓库：https://nexus.kuku.me/#browse/browse:maven-releases:me%2Fkuku%2Fktor-spring-boot-starter

```kotlin
repositories {
    maven { url = uri("https://nexus.kuku.me/repository/maven-public/") }
}
```

引入

```kotlin
implementation("me.kuku:ktor-spring-boot-starter:0.0.4")
```

#### Routing

```kotlin

@Component
class KtorRouting {
    
    fun Routing.ro() {
        route("") {
            get("/") {
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