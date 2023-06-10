package io.project

import io.project.model.Article
import io.project.model.Comment
import io.project.model.User
import io.kvision.redux.RAction

enum class FeedType {
    USER,
    GLOBAL,
    TAG,
    PROFILE,
    PROFILE_FAVORITED
}

data class GLogState(
    val appLoading: Boolean = true,
    val view: View = View.HOME,
    val user: User? = null,
    val articlesLoading: Boolean = false,
    val articles: List<Article>? = null,
    val articlesCount: Int = 0,
    val article: Article? = null,
    val articleComments: List<Comment>? = null,
    val selectedPage: Int = 0,
    val feedType: FeedType = FeedType.GLOBAL,
    val selectedTag: String? = null,
    val profile: User? = null,
    val tagsLoading: Boolean = false,
    val tags: List<String>? = null,
    val editorErrors: List<String>? = null,
    val editedArticle: Article? = null,
    val loginErrors: List<String>? = null,
    val settingsErrors: List<String>? = null,
    val registerErrors: List<String>? = null,
    val registerUserName: String? = null,
    val registerEmail: String? = null
) {
    val pageSize = when (feedType) {
        FeedType.USER, FeedType.GLOBAL, FeedType.TAG -> 10
        FeedType.PROFILE, FeedType.PROFILE_FAVORITED -> 5
    }

    private fun linkClassName(view: View) = if (this.view == view) "nav-link active" else "nav-link"

    val homeLinkClassName = linkClassName(View.HOME)
    val loginLinkClassName = linkClassName(View.LOGIN)
    val registerLinkClassName = linkClassName(View.REGISTER)
    val editorLinkClassName = linkClassName(View.EDITOR)
    val settingsLinkClassName = linkClassName(View.SETTINGS)
    val profileLinkClassName =
        if (view == View.PROFILE && profile?.username == user?.username) "nav-link active" else "nav-link"
}

sealed class GLogAction : RAction {
    object AppLoaded : GLogAction()
    object HomePage : GLogAction()
    data class SelectFeed(
        val feedType: FeedType,
        val tag: String?,
        val profile: User?
    ) : GLogAction()

    object ArticlesLoading : GLogAction()
    data class ArticlesLoaded(val articles: List<Article>, val articlesCount: Int) : GLogAction()
    data class SelectPage(val selectedPage: Int) : GLogAction()

    data class ShowArticle(val article: Article) : GLogAction()
    data class ShowArticleCommets(val articleComments: List<Comment>) : GLogAction()
    data class ArticleUpdated(val article: Article) : GLogAction()

    object TagsLoading : GLogAction()
    data class TagsLoaded(val tags: List<String>) : GLogAction()

    data class AddComment(val comment: Comment) : GLogAction()
    data class DeleteComment(val id: Int) : GLogAction()

    data class ProfilePage(val feedType: FeedType) : GLogAction()
    data class ProfileFollowChanged(val user: User) : GLogAction()

    object LoginPage : GLogAction()
    data class Login(val user: User) : GLogAction()
    data class LoginError(val errors: List<String>) : GLogAction()

    object SettingsPage : GLogAction()
    data class SettingsError(val errors: List<String>) : GLogAction()

    object RegisterPage : GLogAction()
    data class RegisterError(val username: String?, val email: String?, val errors: List<String>) : GLogAction()

    object Logout : GLogAction()

    data class EditorPage(val article: Article?) : GLogAction()
    data class EditorError(
        val article: Article,
        val errors: List<String>
    ) : GLogAction()
}

fun glogReducer(state: GLogState, action: GLogAction): GLogState = when (action) {
    is GLogAction.AppLoaded -> {
        state.copy(appLoading = false)
    }
    is GLogAction.HomePage -> {
        state.copy(view = View.HOME, articles = null)
    }
    is GLogAction.SelectFeed -> {
        state.copy(
            feedType = action.feedType,
            selectedTag = action.tag,
            profile = action.profile,
            selectedPage = 0
        )
    }
    is GLogAction.ArticlesLoading -> {
        state.copy(articlesLoading = true)
    }
    is GLogAction.ArticlesLoaded -> {
        state.copy(articlesLoading = false, articles = action.articles, articlesCount = action.articlesCount)
    }
    is GLogAction.SelectPage -> {
        state.copy(selectedPage = action.selectedPage)
    }
    is GLogAction.ShowArticle -> {
        state.copy(view = View.ARTICLE, article = action.article)
    }
    is GLogAction.ShowArticleCommets -> {
        state.copy(view = View.ARTICLE, articleComments = action.articleComments)
    }
    is GLogAction.ArticleUpdated -> {
        if (state.view == View.ARTICLE) {
            state.copy(article = action.article)
        } else {
            state.copy(articles = state.articles?.map {
                if (it.slug == action.article.slug) {
                    action.article
                } else {
                    it
                }
            })
        }
    }
    is GLogAction.TagsLoading -> {
        state.copy(tagsLoading = true)
    }
    is GLogAction.TagsLoaded -> {
        state.copy(tagsLoading = false, tags = action.tags)
    }
    is GLogAction.AddComment -> {
        state.copy(articleComments = listOf(action.comment) + (state.articleComments ?: listOf()))
    }
    is GLogAction.DeleteComment -> {
        state.copy(articleComments = state.articleComments?.filterNot { it.id == action.id })
    }
    is GLogAction.ProfilePage -> {
        state.copy(view = View.PROFILE, feedType = action.feedType, articles = null)
    }
    is GLogAction.ProfileFollowChanged -> {
        if (state.view == View.PROFILE) {
            state.copy(profile = action.user)
        } else {
            state.copy(article = state.article?.copy(author = action.user))
        }
    }
    is GLogAction.LoginPage -> {
        state.copy(view = View.LOGIN, loginErrors = null)
    }
    is GLogAction.Login -> {
        state.copy(user = action.user)
    }
    is GLogAction.LoginError -> {
        state.copy(user = null, loginErrors = action.errors)
    }
    is GLogAction.SettingsPage -> {
        state.copy(view = View.SETTINGS, settingsErrors = null)
    }
    is GLogAction.SettingsError -> {
        state.copy(settingsErrors = action.errors)
    }
    is GLogAction.RegisterPage -> {
        state.copy(view = View.REGISTER, registerErrors = null, registerUserName = null, registerEmail = null)
    }
    is GLogAction.RegisterError -> {
        state.copy(registerErrors = action.errors, registerUserName = action.username, registerEmail = action.email)
    }
    is GLogAction.Logout -> {
        GLogState(appLoading = false)
    }
    is GLogAction.EditorPage -> {
        state.copy(
            view = View.EDITOR,
            editorErrors = null,
            editedArticle = action.article
        )
    }
    is GLogAction.EditorError -> {
        state.copy(
            editorErrors = action.errors,
            editedArticle = action.article
        )
    }
}
