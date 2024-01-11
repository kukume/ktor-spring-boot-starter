## ktor-spring-boot-starter

ktor：https://ktor.io/

整合`Ktor`并提供一些扩展函数，版本号前缀与ktor官方一致

JSON序列化使用SpringBoot的ObjectMapper


### 使用

#### 引入（gradle.kts）

引入

```kotlin
repositories {
    maven("https://nexus.kuku.me/repository/maven-public/")
    mavenCentral()
}


implementation("me.kuku:ktor-spring-boot-starter:2.3.6.1")
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

### sponsor
[dartnode](https://dartnode.com/)

<img width="117" alt="image" src="https://github.com/kukume/ktor-spring-boot-starter/assets/45278810/97e887d2-1b59-44f1-9f71-ef167f12ec55">

