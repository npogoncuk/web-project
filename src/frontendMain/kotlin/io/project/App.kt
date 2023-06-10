package io.project

import io.project.layout.articles.article
import io.project.layout.homePage
import io.project.layout.profilePage
import io.project.layout.shared.headerNav
import io.project.layout.users.loginPage
import io.project.layout.users.registerPage
import io.project.layout.users.settingsPage
import io.kvision.Application
import io.kvision.html.header
import io.kvision.html.main
import io.kvision.module
import io.kvision.pace.Pace
import io.kvision.pace.PaceOptions
import io.kvision.panel.ContainerType
import io.kvision.panel.root
import io.kvision.require
import io.kvision.routing.Routing
import io.kvision.startApplication
import io.kvision.state.bind
import io.project.layout.shared.footer
import io.project.layout.users.editorPage

class App : Application() {

    override fun start() {
        Routing.init()
        Pace.init(require("pace-progressbar/themes/green/pace-theme-bounce.css"))
        Pace.setOptions(PaceOptions(manual = true))
        GLogManager.initialize()
        root("kvapp", containerType = ContainerType.NONE, addRow = false) {
            header().bind(GLogManager.glogStore) { state ->
                headerNav(state)
            }
            main().bind(GLogManager.glogStore) { state ->
                if (!state.appLoading) {
                    when (state.view) {
                        View.HOME -> {
                            homePage(state)
                        }
                        View.ARTICLE -> {
                           article(state)
                        }
                        View.PROFILE -> {
                            profilePage(state)
                        }
                        View.LOGIN -> {
                            loginPage(state)
                        }
                        View.REGISTER -> {
                            registerPage(state)
                        }
                        View.EDITOR -> {

                            editorPage(state)
                        }
                        View.SETTINGS -> {
                            settingsPage(state)
                        }
                    }
                }
            }
            footer()
        }
    }
}

fun main() {
    startApplication(::App, module.hot)
}
