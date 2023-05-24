package io.project.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import io.project.model.User
import io.project.utils.Cipher
import org.springframework.stereotype.Service
import java.util.*

@Service
class JwtProvider {

    fun decodeJWT(token: String): DecodedJWT = JWT.require(Cipher.algorithm).build().verify(token)

    fun createJWT(user: User): String = JWT.create().withIssuedAt(Date()).withSubject(user.username)
            // expiresAt now + 1 day and night ( = 1  day and night * 24 hours * 60 minutes * 60 seconds * 1000 milliseconds)
        .withExpiresAt(Date(System.currentTimeMillis() + 1 * 24 * 60 * 60 * 1000))
        .sign(Cipher.algorithm)
}
