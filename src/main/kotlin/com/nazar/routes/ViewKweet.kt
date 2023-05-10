package com.nazar.routes

import com.nazar.KweetSession
import com.nazar.ViewKweet
import com.nazar.dao.DAOFacade
import com.nazar.securityCode
import io.ktor.server.application.*
import io.ktor.server.freemarker.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*


fun Route.viewKweet(dao: DAOFacade, hashFunction: (String) -> String) {

    get<ViewKweet> {
        val user = call.sessions.get<KweetSession>()?.let { dao.user(it.userId) }
        val date = System.currentTimeMillis()
        val code = if (user != null) call.securityCode(date, user, hashFunction) else null

        call.respond(
            FreeMarkerContent(
                "view-kweet.ftl",
                mapOf("user" to user, "kweet" to dao.getKweet(it.id), "date" to date, "code" to code),
                user?.userId ?: ""
            )
        )
    }
}
