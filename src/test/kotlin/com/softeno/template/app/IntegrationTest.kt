package com.softeno.template.app

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.ninjasquad.springmockk.MockkBean
import com.softeno.template.SoftenoMvcJpaApp
import com.softeno.template.app.permission.PermissionFixture.Companion.aPermission
import com.softeno.template.app.permission.PermissionFixture.Companion.aPermissionDto
import com.softeno.template.app.permission.db.PermissionRepository
import io.mockk.every
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest(
    classes = [SoftenoMvcJpaApp::class],
    properties = ["spring.profiles.active=integration"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient(timeout = "6000")
@EnableConfigurationProperties
@ConfigurationPropertiesScan("com.softeno")
abstract class BaseIntegrationTest {

    @Autowired
    lateinit var permissionRepository: PermissionRepository

    @Autowired
    lateinit var webTestClient: WebTestClient

    @Container
    var postgreSQLContainer = PostgreSQLContainer("postgres:15.2-alpine")
        .withDatabaseName("application")
        .withUsername("admin")
        .withPassword("admin")


    @BeforeEach
    fun init() {
        // ...
    }

    @AfterEach
    fun cleanup() {
        permissionRepository.deleteAll()
    }

}

class ContextLoadsTest : BaseIntegrationTest() {

    @Test
    fun testConnection() {
        assertTrue(postgreSQLContainer.isRunning)
    }
}

class PermissionTest : BaseIntegrationTest() {

    @Test
    fun shouldReturnEmptyPermissionResponse() {
        webTestClient.get().uri("/permissions")
            .exchange()
            .expectStatus().isOk()
            .expectBody().jsonPath("content").isEmpty
    }

    @Test
    fun shouldRetrievePermission() {
        val aPermission = aPermission()
        permissionRepository.save(aPermission)

        webTestClient.get().uri("/permissions")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("content.[0].name").isEqualTo(aPermission.name!!)
            .jsonPath("content.[0].description").isEqualTo(aPermission.description!!)
    }

    @Test
    fun shouldPersistPermission() {
        val aPermissionDto = aPermissionDto()

        webTestClient.post().uri("/permissions")
            .body(BodyInserters.fromValue(aPermissionDto))
            .exchange()
            .expectStatus().isOk

        assertEquals(permissionRepository.findAll().size, 1)
        assertEquals(permissionRepository.findAll()[0].name!!, aPermissionDto.name)
        assertEquals(permissionRepository.findAll()[0].description!!, aPermissionDto.description)
    }
}

class PermissionTestMockk : BaseIntegrationTest() {

    @MockkBean
    @Order(value = Ordered.HIGHEST_PRECEDENCE)
    lateinit var permissionRepositoryMock: PermissionRepository

    @BeforeEach
    fun initMockkRepository() {
        every { permissionRepositoryMock.deleteAll() }.answers { }
    }

    @Test
    fun shouldPersistAndRetrievePermission() {
        val aPermission = aPermission()

        every { permissionRepositoryMock.findAll(any<Pageable>()) }.answers { PageImpl(listOf(aPermission)) }

        webTestClient.get().uri("/permissions")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("content.[0].name").isEqualTo(aPermission.name!!)
            .jsonPath("content.[0].description").isEqualTo(aPermission.description!!)
    }
}

data class SampleResponseDto(val data: String)

class ExternalControllerTest : BaseIntegrationTest(), ExternalApiAbility {

    @Autowired
    private lateinit var webclient: WebClient

    private val wiremock: WireMockServer = WireMockServer(options().port(4500))

    @BeforeEach
    fun `setup wiremock`() {
        wiremock.start()
    }

    @AfterEach
    fun `stop wiremock`() {
        wiremock.stop()
        wiremock.resetAll()
    }

    @Test
    fun `mock external service with wiremock`() {
        // given
        mockGetId(wiremock)

        val expected = SampleResponseDto(data = "1")

        // expect
        val response = webclient.get().uri("http://localhost:4500/sample/100")
            .retrieve()
            .bodyToMono(SampleResponseDto::class.java)
            .block()

        assertEquals(expected, response)
    }

    @Test
    fun `test external controller`() {
        // given
        mockGetId(wiremock)

        // expect
        webTestClient.get().uri("/external/1")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("data").isEqualTo("""
                {
                    "data": "1"
                }
            """.trimIndent())
    }
}

interface ExternalApiAbility {

    fun mockGetId(wiremock: WireMockServer) {
        wiremock.stubFor(
            get(urlMatching("/sample/.*"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                    {
                        "data": "1"
                    }
                """.trimIndent()
                        )
                )
        )
    }
}


