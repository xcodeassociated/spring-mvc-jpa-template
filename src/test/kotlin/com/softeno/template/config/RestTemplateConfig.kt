package com.softeno.template.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RestTemplateConfig {

    @Autowired
    lateinit var server: ServletWebServerApplicationContext

    @Bean
    fun testRestTemplate(): TestRestTemplate {
        val restTemplate = RestTemplateBuilder().rootUri("http://localhost:" + server.webServer.port)
        return TestRestTemplate(restTemplate);
    }

}