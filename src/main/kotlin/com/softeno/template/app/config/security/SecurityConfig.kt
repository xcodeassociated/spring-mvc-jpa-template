package com.softeno.template.app.config.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.MappedJwtClaimSetConverter
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import java.util.Collections
import kotlin.collections.set

@Profile(value = ["!integration"])
@EnableWebSecurity
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig {

    class Jwt2AuthenticationConverter : Converter<Jwt, Collection<GrantedAuthority>> {
        override fun convert(jwt: Jwt): Collection<GrantedAuthority> {
            val realmAccess = jwt.claims.getOrDefault("realm_access", mapOf<String, Any>()) as Map<String, Any>
            val realmRoles = (realmAccess["roles"] ?: listOf<String>()) as Collection<String>

            return realmRoles
                .map { role: String -> SimpleGrantedAuthority(role) }.toList()
        }

    }

    class AuthenticationConverter: Converter<Jwt, AbstractAuthenticationToken> {
        override fun convert(jwt: Jwt): AbstractAuthenticationToken {
            return JwtAuthenticationToken(jwt, Jwt2AuthenticationConverter().convert(jwt))
        }

    }

    class UsernameSubClaimAdapter : Converter<Map<String, Any>, Map<String, Any>> {
        private val delegate = MappedJwtClaimSetConverter.withDefaults(Collections.emptyMap())
        override fun convert(claims: Map<String, Any>): Map<String, Any> {
            val convertedClaims = delegate.convert(claims)
            val username = convertedClaims?.get("sub") as String
            convertedClaims["sub"] = username
            return convertedClaims
        }
    }

    fun jwtDecoder(issuer: String, jwkSetUri: String): JwtDecoder {
        val jwtDecoder: NimbusJwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build()
        jwtDecoder.setClaimSetConverter(UsernameSubClaimAdapter())
        jwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer))
        return jwtDecoder
    }

    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf("*")
        configuration.allowedMethods = listOf("*")
        configuration.allowedHeaders = listOf("*")
        configuration.exposedHeaders = listOf("*")

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        // note: swagger can be restricted by cors
        return source
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity,
                            @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}") issuer: String,
                            @Value("\${spring.security.oauth2.client.provider.keycloak.jwk-set-uri}") jwkSetUri: String
    ): SecurityFilterChain {
        return http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .authorizeHttpRequests {
                it.requestMatchers(
                    // monitoring
                    "/actuator/**",
                    // springdocs
                    "/swagger-ui.html",
                    "/webjars/**",
                    "/swagger-resources/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**").permitAll()
                it.requestMatchers("/permissions/**", "/external/**", "/error/**")
                    .hasAuthority("ROLE_ADMIN")
            }
            .oauth2ResourceServer { rss ->
                rss.jwt { jwtDecoder(issuer, jwkSetUri) }
                rss.jwt { it.jwtAuthenticationConverter { jwt ->
                    AuthenticationConverter().convert(jwt)
                } }
            }
            .build()
    }
}