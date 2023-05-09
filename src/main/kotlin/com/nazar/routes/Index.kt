package com.nazar.routes

import com.nazar.Index
import com.nazar.KweetSession
import com.nazar.dao.DAOFacade
import io.ktor.server.application.*
import io.ktor.server.freemarker.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*


fun Route.index(dao: DAOFacade) {
    get<Index> {
        val user = call.sessions.get<KweetSession>()?.let { dao.user(it.userId) }

        val top = dao.top(10).map { dao.getKweet(it) }
        val latest = dao.latest(10).map { dao.getKweet(it) }

        val etagString =
            user?.userId + "," + top.joinToString { it.id.toString() } + latest.joinToString { it.id.toString() }
        val etag = etagString.hashCode()

        call.respond(
            FreeMarkerContent(
                "index.ftl",
                mapOf("top" to top, "latest" to latest, "user" to user),
                etag.toString()
            )
        )
    }
}
