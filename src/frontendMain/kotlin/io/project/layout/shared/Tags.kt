package io.project.layout.shared

import io.project.GLogManager
import io.project.GLogState
import io.project.FeedType
import io.kvision.core.Container
import io.kvision.html.div
import io.kvision.html.link
import io.kvision.html.p

fun Container.tags(state: GLogState) {
    div(className = "sidebar") {
        p("Popular Tags")
        if (state.tagsLoading) {
            div("Loading tags...", className = "post-preview")
        } else if (!state.tags.isNullOrEmpty()) {
            div(className = "tag-list") {
                state.tags.forEach { tag ->
                    link(tag, "", className = "tag-pill tag-default").onClick {
                        it.preventDefault()
                        GLogManager.selectFeed(FeedType.TAG, selectedTag = tag)
                    }
                }
            }
        } else {
            div("No tags are here... yet.", className = "post-preview")
        }
    }
}
