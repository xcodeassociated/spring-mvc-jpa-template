package com.softeno.template.config

import com.softeno.template.ExternalClientConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

@Profile(value = ["integration"])
@Configuration
class WebClientConfig {


    @Bean(value = ["external"])
    fun buildWebClient(config: ExternalClientConfig): WebClient {
        return WebClient.builder()
            .baseUrl(config.url)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build()
    }
}