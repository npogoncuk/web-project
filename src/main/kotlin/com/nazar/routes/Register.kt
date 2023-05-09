package com.nazar.routes

import com.nazar.*
import com.nazar.dao.DAOFacade
import com.nazar.model.User
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.freemarker.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*

fun Route.register(dao: DAOFacade, hashFunction: (String) -> String) {
    post<Register> {
        val user = call.sessions.get<KweetSession>()?.let { dao.user(it.userId) }
        if (user != null) return@post call.redirect(UserPage(user.userId))

        val registration = call.receive<Parameters>()
        val userId = registration["userId"] ?: return@post call.redirect(it)
        val password = registration["password"] ?: return@post call.redirect(it)
        val displayName = registration["displayName"] ?: return@post call.redirect(it)
        val email = registration["email"] ?: return@post call.redirect(it)

        val error = Register(userId, displayName, email)

        when {
            password.length < 6 -> call.redirect(error.copy(error = "Password should be at least 6 characters long"))
            userId.length < 4 -> call.redirect(error.copy(error = "Login should be at least 4 characters long"))
            !userNameValid(userId) -> call.redirect(error.copy(error = "Login should be consists of digits, letters, dots or underscores"))
            dao.user(userId) != null -> call.redirect(error.copy(error = "User with the following login is already registered"))
            else -> {
                val hash = hashFunction(password)
                val newUser = User(userId, email, displayName, hash)

                try {
                    dao.createUser(newUser)
                } catch (e: Throwable) {
                    when {
                        dao.user(userId) != null -> call.redirect(error.copy(error = "User with the following login is already registered"))
                        dao.userByEmail(email) != null -> call.redirect(error.copy(error = "User with the following email $email is already registered"))
                        else -> {
                            application.log.error("Failed to register user", e)
                            call.redirect(error.copy(error = "Failed to register"))
                        }
                    }
                }

                call.sessions.set(KweetSession(newUser.userId))
                call.redirect(UserPage(newUser.userId))
            }
        }
    }

    get<Register> {
        val user = call.sessions.get<KweetSession>()?.let { dao.user(it.userId) }
        if (user != null) {
            call.redirect(UserPage(user.userId))
        } else {
            call.respond(
                FreeMarkerContent(
                    "register.ftl",
                    mapOf("pageUser" to User(it.userId, it.email, it.displayName, ""), "error" to it.error),
                    ""
                )
            )
        }
    }
}
