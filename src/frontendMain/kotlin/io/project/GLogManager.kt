package io.project

import io.kvision.navigo.Navigo
import io.kvision.redux.createReduxStore
import io.kvision.remote.ServiceException
import io.project.helpers.withProgress
import io.project.model.Article
import io.project.model.User
import kotlinx.browser.localStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import org.w3c.dom.get
import org.w3c.dom.set
import org.w3c.fetch.RequestInit

object GLogManager : CoroutineScope by CoroutineScope(Dispatchers.Default + SupervisorJob()) {

    const val JWT_TOKEN = "jwtToken"

    private val routing = Navigo(null, true, "#")
    val glogStore = createReduxStore(::glogReducer, GLogState())

    val userService = UserService {
        this.authRequest()
    }
    val articleService = ArticleService {
        this.authRequest()
    }

    private fun RequestInit.authRequest(): Unit {
        getJwtToken()?.let {
            this.headers["Authorization"] = "Token $it"
        }
    }

    fun initialize() {
        routing.initialize().resolve()
        if (getJwtToken() != null) {
            withProgress {
                try {
                    val user = userService.user()
                    glogStore.dispatch(GLogAction.Login(user))
                    saveJwtToken(user.token!!)
                    afterInitialize(FeedType.USER)
                } catch (e: Exception) {
                    console.log("Invalid JWT Token")
                    deleteJwtToken()
                    afterInitialize(FeedType.GLOBAL)
                }
            }
        } else {
            afterInitialize(FeedType.GLOBAL)
        }
    }

    private fun afterInitialize(feedType: FeedType) {
        glogStore.dispatch(GLogAction.AppLoaded)
        if (glogStore.getState().view == View.HOME) {
            selectFeed(feedType)
            loadTags()
        }
    }

    fun redirect(view: View) {
        routing.navigate(view.url)
    }

    fun loginPage() {
        glogStore.dispatch(GLogAction.LoginPage)
    }

    fun login(email: String?, password: String?) {
        withProgress {
            try {
                val user = userService.login(email, password)
                glogStore.dispatch(GLogAction.Login(user))
                saveJwtToken(user.token!!)
                routing.navigate(View.HOME.url)
            } catch (e: ServiceException) {
                glogStore.dispatch(GLogAction.LoginError(parseErrors(e.message)))
            }
        }
    }

    fun settingsPage() {
        glogStore.dispatch(GLogAction.SettingsPage)
    }

    fun settings(image: String?, username: String?, bio: String?, email: String?, password: String?) {
        withProgress {
            try {
                val user = userService.settings(image, username, bio, email, password)
                glogStore.dispatch(GLogAction.Login(user))
                saveJwtToken(user.token!!)
                routing.navigate("${View.PROFILE.url}${user.username}")
            } catch (e: Exception) {
                glogStore.dispatch(GLogAction.SettingsError(parseErrors(e.message)))
            }
        }
    }

    fun registerPage() {
        glogStore.dispatch(GLogAction.RegisterPage)
    }

    fun register(username: String?, email: String?, password: String?) {
        withProgress {
            try {
                val user = userService.register(username, email, password)
                glogStore.dispatch(GLogAction.Login(user))
                saveJwtToken(user.token!!)
                routing.navigate(View.HOME.url)
            } catch (e: Exception) {
                glogStore.dispatch(GLogAction.RegisterError(username, email, parseErrors(e.message)))
            }
        }
    }

    fun logout() {
        deleteJwtToken()
        glogStore.dispatch(GLogAction.Logout)
        routing.navigate(View.HOME.url)
    }

    fun homePage() {
        glogStore.dispatch(GLogAction.HomePage)
        val state = glogStore.getState()
        if (!state.appLoading) {
            if (state.user != null) {
                selectFeed(FeedType.USER)
            } else {
                selectFeed(FeedType.GLOBAL)
            }
            loadTags()
        }
    }

    fun selectFeed(feedType: FeedType, selectedTag: String? = null, profile: User? = null) {
        glogStore.dispatch(GLogAction.SelectFeed(feedType, selectedTag, profile))
        loadArticles()
    }

    fun selectPage(page: Int) {
        glogStore.dispatch(GLogAction.SelectPage(page))
        loadArticles()
    }

    fun showArticle(slug: String) {
        withProgress {
            try {
                val article = async { articleService.article(slug) }
                val articleComments = async { articleService.articleComments(slug) }
                glogStore.dispatch(GLogAction.ShowArticle(article.await()))
                glogStore.dispatch(GLogAction.ShowArticleCommets(articleComments.await()))
            } catch (e: Exception) {
                routing.navigate(View.HOME.url)
            }
        }
    }

    fun articleComment(slug: String, comment: String?) {
        withProgress {
            val newComment = articleService.articleComment(slug, comment)
            glogStore.dispatch(GLogAction.AddComment(newComment))
        }
    }

    fun articleCommentDelete(slug: String, id: Int) {
        withProgress {
            articleService.articleCommentDelete(slug, id)
            glogStore.dispatch(GLogAction.DeleteComment(id))
        }
    }

    fun toggleFavoriteArticle(article: Article) {
        if (glogStore.getState().user != null) {
            withProgress {
                val articleUpdated = articleService.articleFavorite(article.slug!!, !article.favorited)
                glogStore.dispatch(GLogAction.ArticleUpdated(articleUpdated))
            }
        } else {
            routing.navigate(View.LOGIN.url)
        }
    }

    fun showProfile(username: String, favorites: Boolean) {
        val feedType = if (favorites) FeedType.PROFILE_FAVORITED else FeedType.PROFILE
        glogStore.dispatch(GLogAction.ProfilePage(feedType))
        withProgress {
            val user = userService.profile(username)
            selectFeed(feedType, null, user)
        }
    }

    fun toggleProfileFollow(user: User) {
        if (glogStore.getState().user != null) {
            withProgress {
                val changedUser = userService.profileFollow(user.username!!, !user.following!!)
                glogStore.dispatch(GLogAction.ProfileFollowChanged(changedUser))
            }
        } else {
            routing.navigate(View.LOGIN.url)
        }
    }

    private fun loadArticles() {
        glogStore.dispatch(GLogAction.ArticlesLoading)
        withProgress {
            val state = glogStore.getState()
            val limit = state.pageSize
            val offset = state.selectedPage * limit
            val articleDto = when (state.feedType) {
                FeedType.USER -> articleService.feed(offset, limit)
                FeedType.GLOBAL -> articleService.articles(null, null, null, offset, limit)
                FeedType.TAG -> articleService.articles(state.selectedTag, null, null, offset, limit)
                FeedType.PROFILE -> articleService.articles(null, state.profile?.username, null, offset, limit)
                FeedType.PROFILE_FAVORITED -> articleService.articles(
                    null,
                    null,
                    state.profile?.username,
                    offset,
                    limit
                )
            }
            glogStore.dispatch(GLogAction.ArticlesLoaded(articleDto.articles, articleDto.articlesCount))
        }
    }

    private fun loadTags() {
        glogStore.dispatch(GLogAction.TagsLoading)
        withProgress {
            val tags = articleService.tags()
            glogStore.dispatch(GLogAction.TagsLoaded(tags))
        }
    }

    fun editorPage(slug: String? = null) {
        if (slug == null) {
            glogStore.dispatch(GLogAction.EditorPage(null))
        } else {
            withProgress {
                val article = articleService.article(slug)
                glogStore.dispatch(GLogAction.EditorPage(article))
            }
        }
    }

    fun createArticle(title: String?, description: String?, body: String?, tags: String?) {
        withProgress {
            val tagList = tags?.split(" ")?.toList() ?: emptyList()
            try {
                val article = articleService.createArticle(title, description, body, tagList)
                routing.navigate(View.ARTICLE.url + "/" + article.slug)
            } catch (e: Exception) {
                glogStore.dispatch(
                    GLogAction.EditorError(
                        Article(
                            title = title,
                            description = description,
                            body = body,
                            tagList = tagList
                        ), parseErrors(e.message)
                    )
                )
            }
        }
    }

    fun updateArticle(slug: String, title: String?, description: String?, body: String?, tags: String?) {
        withProgress {
            val tagList = tags?.split(" ")?.toList() ?: emptyList()
            try {
                val article = articleService.updateArticle(slug, title, description, body, tagList)
                routing.navigate(View.ARTICLE.url + "/" + article.slug)
            } catch (e: Exception) {
                glogStore.dispatch(
                    GLogAction.EditorError(
                        glogStore.getState().editedArticle!!.copy(
                            title = title,
                            description = description,
                            body = body,
                            tagList = tagList
                        ), parseErrors(e.message)
                    )
                )
            }
        }
    }

    fun deleteArticle(slug: String) {
        withProgress {
            articleService.deleteArticle(slug)
            routing.navigate(View.HOME.url)
        }
    }

    fun getJwtToken(): String? {
        return localStorage[JWT_TOKEN]
    }

    private fun saveJwtToken(token: String) {
        localStorage[JWT_TOKEN] = token
    }

    private fun deleteJwtToken() {
        localStorage.removeItem(JWT_TOKEN)
    }

    private fun parseErrors(message: String?): List<String> {
        return message?.split("\n")?.toList() ?: emptyList()
    }
}
