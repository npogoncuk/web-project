package io.project.layout.articles

import io.project.GLogManager
import io.project.GLogState
import io.project.model.Article
import io.project.model.Comment
import io.kvision.core.Container
import io.kvision.core.onClick
import io.kvision.html.TAG
import io.kvision.html.div
import io.kvision.html.image
import io.kvision.html.link
import io.kvision.html.p
import io.kvision.html.span
import io.kvision.html.tag
import io.kvision.types.toStringF

fun Container.articleComment(state: GLogState, comment: Comment, article: Article) {
    div(className = "card") {
        div(className = "card-block") {
            p(comment.body, className = "card-text")
        }
        div(className = "card-footer", rich = true) {
            val imageSrc =
                comment.author?.image?.ifBlank { null } ?: "https://static.productionready.io/images/smiley-cyrus.jpg"
            link("", "#/@${comment.author?.username}", className = "comment-author") {
                image(imageSrc, className = "comment-author-img")
            }
            +" &nbsp; "
            link(comment.author?.username ?: "", "#/@${comment.author?.username}", className = "comment-author")
            val createdAtFormatted = comment.createdAt?.toStringF("MMMM D, YYYY")
            span(createdAtFormatted, className = "date-posted")
            if (state.user != null && state.user.username == comment.author?.username) {
                span(className = "mod-options") {
                    tag(TAG.I, className = "ion-trash-a").onClick {
                        GLogManager.articleCommentDelete(article.slug!!, comment.id!!)
                    }
                }
            }
        }
    }
}
