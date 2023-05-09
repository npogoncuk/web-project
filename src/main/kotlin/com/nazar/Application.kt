package com.nazar

import com.mchange.v2.c3p0.*
import com.nazar.dao.DAOFacade
import com.nazar.model.User
import com.nazar.model.Kweet
import com.nazar.dao.DAOFacadeCache
import com.nazar.dao.DAOFacadeDatabase
import com.nazar.plugins.*
import freemarker.cache.*
import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.freemarker.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.conditionalheaders.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.partialcontent.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.Resources
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.util.*
import kotlinx.serialization.Serializable
import org.h2.*
import org.jetbrains.exposed.sql.*
import java.io.*
import java.net.*
import java.util.concurrent.*
import javax.crypto.*
import javax.crypto.spec.*

@Resource("/")
class Index()

@Resource("/post-new")
class PostNew()

@Resource("/kweet/{id}/delete")
class KweetDelete(val id: Int)

@Resource("/kweet/{id}")
data class ViewKweet(val id: Int)

@Resource("/user/{user}")
data class UserPage(val user: String)

@Resource("/register")
data class Register(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val error: String = ""
)

@Resource("/login")
data class Login(val userId: String = "", val error: String = "")

@Resource("/logout")
class Logout()

data class KweetSession(val userId: String)

val hashKey = hex("6819b57a326945c1968f45236589")


val dir = File("build/db")

val pool = ComboPooledDataSource().apply {
    driverClass = Driver::class.java.name
    jdbcUrl = "jdbc:h2:file:${dir.canonicalFile.absolutePath}"
    user = ""
    password = ""
}

val hmacKey = SecretKeySpec(hashKey, "HmacSHA1")


val dao: DAOFacade = DAOFacadeCache(DAOFacadeDatabase(Database.connect(pool)), File(dir.parentFile, "ehcache"))

fun Application.main() {
    dao.init()
    environment.monitor.subscribe(ApplicationStopped) { pool.close() }
    mainWithDependencies(dao)
}
fun Application.mainWithDependencies(dao: DAOFacade) {
    install(DefaultHeaders)
    install(CallLogging)
    install(ConditionalHeaders)
    install(PartialContent)
    install(Resources)
    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
    }
    install(Sessions) {
        cookie<KweetSession>("SESSION") {
            transform(SessionTransportTransformerMessageAuthentication(hashKey))
        }
    }
    val hashFunction = { s: String -> hash(s) }

    routing {
        //styles()
        //index(dao)
        //postNew(dao, hashFunction)
        //delete(dao, hashFunction)
        //userPage(dao)
        //viewKweet(dao, hashFunction)

        //login(dao, hashFunction)
        //register(dao, hashFunction)
    }

    //configureTemplating()
    //configureSecurity()
    //configureHTTP()
    //configureMonitoring()
    //configureSerialization()
    //configureDatabases()
    //configureRouting()
}

fun hash(password: String): String {
    val hmac = Mac.getInstance("HmacSHA1")
    hmac.init(hmacKey)
    return hex(hmac.doFinal(password.toByteArray(Charsets.UTF_8)))
}
suspend inline fun <reified T: Any> ApplicationCall.redirect(resource: T) {
    respondRedirect(application.href(resource))
}

fun ApplicationCall.securityCode(date: Long, user: User, hashFunction: (String) -> String) =
    hashFunction("$date:${user.userId}:${request.host()}:${refererHost()}")

fun ApplicationCall.verifyCode(date: Long, user: User, code: String, hashFunction: (String) -> String) =
    securityCode(date, user, hashFunction) == code &&
            (System.currentTimeMillis() - date).let { it > 0 && it < TimeUnit.MILLISECONDS.convert(2, TimeUnit.HOURS) }

fun ApplicationCall.refererHost() = request.header(HttpHeaders.Referrer)?.let { URI.create(it).host }


private val userIdPattern = "[a-zA-Z0-9_\\.]+".toRegex()

internal fun userNameValid(userId: String) = userId.matches(userIdPattern)