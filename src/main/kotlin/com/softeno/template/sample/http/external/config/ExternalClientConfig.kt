package com.softeno.template.sample.http.external.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient

@ConfigurationProperties(prefix = "com.softeno.external")
data class ExternalClientConfig(val url: String = "", val name: String = "")

@Profile(value = ["!integration"])
@Configuration
class WebClientConfig {

    @Bean
    fun authorizedClientManager(clients: ClientRegistrationRepository, service: OAuth2AuthorizedClientService): OAuth2AuthorizedClientManager {
        val manager = AuthorizedClientServiceOAuth2AuthorizedClientManager(clients, service)
        val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
            .clientCredentials()
            .build()
        manager.setAuthorizedClientProvider(authorizedClientProvider)
        return manager
    }

    @Bean(value = ["external"])
    fun webClient(authorizedClientManager: OAuth2AuthorizedClientManager, config: ExternalClientConfig): WebClient {
        val oauth2 = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
        oauth2.setDefaultClientRegistrationId("keycloak")
        return WebClient.builder()
            .filter(oauth2)
            .baseUrl(config.url)
            .build()
    }

}