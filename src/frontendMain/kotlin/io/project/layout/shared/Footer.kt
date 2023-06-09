package io.project.layout.shared

import io.project.View
import io.kvision.core.Container
import io.kvision.html.div
import io.kvision.html.footer
import io.kvision.html.link
import io.kvision.html.span

fun Container.footer() {
    footer {
        div(className = "container") {
            link("GLog", "#${View.HOME.url}", className = "logo-font")
            span(
                "Nazar Pohonchuk Project", rich = true, className = "attribution"
            )
        }
    }
}
