package com.softeno.template

import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Specification

@Testcontainers
@SpringBootTest(classes = SoftenoMvcJpaApp,
        properties = "spring.profiles.active=integration",
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableConfigurationProperties
@ConfigurationPropertiesScan("com.softeno")
class BaseAppSpec extends Specification {
    @Shared
    PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:15.2-alpine")
            .withDatabaseName("application")
            .withUsername("admin")
            .withPassword("admin")
}
