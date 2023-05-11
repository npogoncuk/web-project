package com.nazar.routes

import com.nazar.*
import com.nazar.dao.DAOFacade
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.freemarker.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*


fun Route.postNew(dao: DAOFacade, hashFunction: (String) -> String) {

    get<PostNew> {
        val user = call.sessions.get<KweetSession>()?.let { dao.user(it.userId) }

        if (user == null) {
            call.redirect(Login())
        } else {
            val date = System.currentTimeMillis()
            val code = call.securityCode(date, user, hashFunction)

            call.respond(
                FreeMarkerContent(
                    "new-kweet.ftl",
                    mapOf("user" to user, "date" to date, "code" to code),
                    user.userId
                )
            )
        }
    }

    post<PostNew> {
        val user = call.sessions.get<KweetSession>()?.let { dao.user(it.userId) }

        val post = call.receive<Parameters>()
        val date = post["date"]?.toLongOrNull() ?: return@post call.redirect(it)
        val code = post["code"] ?: return@post call.redirect(it)
        val text = post["text"] ?: return@post call.redirect(it)

        if (user == null || !call.verifyCode(date, user, code, hashFunction)) {
            call.redirect(Login())
        } else {
            val id = dao.createKweet(user.userId, text, null)
            call.redirect(ViewKweet(id))
        }
    }
}