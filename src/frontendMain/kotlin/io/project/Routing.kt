package io.project

import io.kvision.navigo.Navigo
import kotlin.js.RegExp

enum class View(val url: String) {
    HOME("/"),
    ARTICLE("/article"),
    PROFILE("/@"),
    LOGIN("/login"),
    REGISTER("/register"),
    EDITOR("/editor"),
    SETTINGS("/settings"),
}

fun Navigo.initialize(): Navigo {
    return on(View.HOME.url, { _ ->
        GLogManager.homePage()
    }).on("${View.ARTICLE.url}/:slug", { params ->
        GLogManager.showArticle(stringParameter(params, "slug"))
    }).on(RegExp("^${View.PROFILE.url}([^/]+)$"), { username ->
        GLogManager.showProfile(username, false)
    }).on(RegExp("^${View.PROFILE.url}([^/]+)/favorites$"), { username ->
        GLogManager.showProfile(username, true)
    }).on(View.LOGIN.url, { _ ->
        GLogManager.loginPage()
    }).on(View.REGISTER.url, { _ ->
        GLogManager.registerPage()
    }).on(View.SETTINGS.url, { _ ->
        GLogManager.settingsPage()
    }).on(View.EDITOR.url, { _ ->
        GLogManager.editorPage()
    }).on("${View.EDITOR.url}/:slug", { params ->
        GLogManager.editorPage(stringParameter(params, "slug"))
    })
}

fun stringParameter(params: dynamic, parameterName: String): String {
    return (params[parameterName]).toString()
}
