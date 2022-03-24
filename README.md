## ktor-spring-boot-starer

ktor：https://ktor.io/

整合`Ktor`并提供一些方便的扩展函数


### 使用

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