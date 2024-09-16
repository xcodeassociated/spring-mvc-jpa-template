package com.softeno.template.app

import com.softeno.template.SoftenoMvcJpaApp
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Specification

@Testcontainers
@SpringBootTest(classes = SoftenoMvcJpaApp,
        properties = "spring.profiles.active=integration",
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableConfigurationProperties
@EmbeddedKafka(partitions = 1, brokerProperties = ["listeners=PLAINTEXT://localhost:9092", "port=9092"])
@ConfigurationPropertiesScan("com.softeno")
class BaseAppSpec extends Specification {
    @Shared
    PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:15.2-alpine")
            .withDatabaseName("application")
            .withUsername("admin")
            .withPassword("admin")
}
