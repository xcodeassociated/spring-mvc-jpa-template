package com.softeno.template.app.config.security

import org.apache.commons.logging.LogFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import java.util.Optional

class AuditorAwareImpl : AuditorAware<String> {
    private val log = LogFactory.getLog(javaClass)

    override fun getCurrentAuditor(): Optional<String> {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication == null || !authentication.isAuthenticated) {
            return Optional.of("system")
        }

        return when (authentication.principal) {
            is String -> Optional.of(authentication.principal as String)
            is Jwt -> {
                val principal = (authentication.principal as Jwt).claims["sub"] as String
                log.debug("[auditor] authentication principal: $principal")

                Optional.of(principal)
            }
            else -> Optional.of("system")
        }
    }
}

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
class AuditConfiguration {

    @Bean
    fun auditorProvider(): AuditorAware<String> {
        return AuditorAwareImpl()
    }
}