package com.softeno.template.sample.http.external.client

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class ExternalServiceClient(@Qualifier(value = "external") private val webClient: WebClient) {
    private val log = LogFactory.getLog(javaClass)

    @CircuitBreaker(name = "fallbackExample", fallbackMethod = "localCacheFallback")
    fun fetchExternalResource(id: String): String? {
        return webClient.get().uri("/${id}")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(String::class.java)
            .block()
    }

    private fun localCacheFallback(id: String, e: Throwable): String? {
        log.error("fallback: $id, $e")
        return "fallback"
    }

}