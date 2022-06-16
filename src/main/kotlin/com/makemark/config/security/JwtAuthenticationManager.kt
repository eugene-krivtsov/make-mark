package com.makemark.config.security

import com.makemark.model.exception.InvalidBearerToken
import kotlinx.coroutines.reactor.mono
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.Duration

@Component
class JwtAuthenticationManager(
    private val jwtProvider: JwtProvider,
    private val userDetailsService: MmarkUserDetailsService
) : ReactiveAuthenticationManager {

    override fun authenticate(authentication: Authentication?): Mono<Authentication> {
        return Mono.justOrEmpty(authentication)
            .filter { auth -> auth is BearerToken }
            .cast(BearerToken::class.java)
            .flatMap { jwt -> mono { validate(jwt) } }
            .onErrorMap { error -> InvalidBearerToken(error.message) }
    }

    private suspend fun validate(token: BearerToken): Authentication {
        val userId = jwtProvider.getUserIdFromToken(token)
        val user = userDetailsService.findById(userId)
            .cast(MmarkUserDetails::class.java)
            .block(Duration.ofMillis(100)) // TODO:
            ?: error("incorrect user")

        if (jwtProvider.validateToken(token, user)) {
            return UsernamePasswordAuthenticationToken(user.username, user.password, user.authorities)
        }

        throw IllegalArgumentException("Token is not valid.")
    }

}