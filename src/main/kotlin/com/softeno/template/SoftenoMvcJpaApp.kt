package com.softeno.template

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement

@SpringBootApplication
@EnableJpaRepositories
@EnableTransactionManagement
@EnableConfigurationProperties
@ConfigurationPropertiesScan("com.softeno")
class SoftenoMvcJpaApp

fun main(args: Array<String>) {
	runApplication<SoftenoMvcJpaApp>(*args)
}

