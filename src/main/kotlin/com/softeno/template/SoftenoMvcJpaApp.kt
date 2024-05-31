package com.softeno.template

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import jakarta.persistence.*
import jakarta.transaction.Transactional
import org.apache.commons.logging.LogFactory
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.hibernate.annotations.OptimisticLockType
import org.hibernate.annotations.OptimisticLocking
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.convert.converter.Converter
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.domain.*
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.data.querydsl.QuerydslPredicateExecutor
import org.springframework.data.repository.query.Param
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.*
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.security.oauth2.jwt.*
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.web.SecurityFilterChain
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.WebRequest
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.reactive.function.client.WebClient
import java.time.Instant
import java.util.*


@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
@OptimisticLocking(type = OptimisticLockType.VERSION)
open class BaseEntity {

	constructor(uuid: UUID) {
		this.uuid = uuid
	}

	@Column(updatable = false)
	var uuid: UUID

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	var id: Long? = null

	@CreatedDate
	@Column(nullable = false, updatable = false)
	var createdDate: Long? = null

	@LastModifiedDate
	var modifiedDate: Long? = null

	@CreatedBy
	@Column(nullable = false, updatable = false)
	var createdBy: String? = null

	@LastModifiedBy
	var modifiedBy: String? = null

	@Version
	var version: Long? = null

	override fun equals(other: Any?): Boolean {
		return other is BaseEntity && (uuid == other.uuid)
	}

	override fun hashCode(): Int {
		return uuid.hashCode()
	}

	override fun toString(): String {
		return "${javaClass.simpleName}(id = $id, uuid = $uuid, version = $version)"
	}
}

@Entity
@Table(name = "permissions")
class Permission(uuid: UUID = UUID.randomUUID()) : BaseEntity(uuid) {

	@Column(unique = true, nullable = false)
	var name: String? = null

	@Column(nullable = false, columnDefinition = "TEXT")
	var description: String? = null
}

data class PermissionDto(
	val id: Long?,
	val createdBy: String?,
	val createdDate: Long?,
	val modifiedBy: String?,
	val modifiedDate: Long?,
	val version: Long?,

	val name: String,
	val description: String
)

fun Permission.toDto(): PermissionDto {
	return PermissionDto(
		id = this.id,
		createdBy = this.createdBy,
		createdDate = this.createdDate,
		modifiedBy = this.modifiedBy,
		modifiedDate = this.modifiedDate,
		version = this.version,

		name = this.name!!,
		description = this.description!!
	)
}

fun Permission.updateFromDto(permissionDto: PermissionDto): Permission {
	this.name = permissionDto.name
	this.description = permissionDto.description
	this.version = permissionDto.version
	return this
}

@Repository
interface PermissionRepository : JpaRepository<Permission, Long>, QuerydslPredicateExecutor<Permission> {
	override fun findAll(pageable: Pageable): Page<Permission>

	@Modifying
	@Query("UPDATE Permission p SET p.name = :name, p.description = :description, p.version = :newVersion, p.modifiedDate = :modifiedDate, p.modifiedBy = :modifiedBy WHERE p.id = :id AND p.version = :version")
	fun updatePermissionNameAndDescriptionByIdAudited(
		@Param("id") id: Long, @Param("name") name: String, @Param("description") description: String, @Param("version") version: Long,
		@Param("newVersion") newVersion: Long, @Param("modifiedBy") modifiedBy: String, @Param("modifiedDate") modifiedDate: Long
	): Int

	@Query("SELECT p.version FROM Permission p WHERE p.id = :id")
	fun findVersionById(@Param("id") id: Long): Long
}

fun getPageRequest(page: Int, size: Int, sort: String, direction: String) =
	Sort.by(Sort.Order(if (direction == "ASC") Sort.Direction.ASC else Sort.Direction.DESC, sort))
		.let { PageRequest.of(page, size, it) }

@Service
class PermissionService(
	private val permissionRepository: PermissionRepository,
	private val entityManager: EntityManager
) {
	private val log = LogFactory.getLog(javaClass)

	fun getAllPermissions(pageable: Pageable): Page<PermissionDto> {
		return permissionRepository.findAll(pageable).map { it.toDto() }
	}

	fun getPermission(id: Long): PermissionDto {
		return permissionRepository.findById(id).get().toDto()
	}

	@Transactional
	fun createPermission(permissionDto: PermissionDto): PermissionDto {
		val permission = Permission()
		permission.name = permissionDto.name
		permission.description = permissionDto.description
		return permissionRepository.save(permission).toDto()
	}

	@Transactional
	fun updatePermission(id: Long, permissionDto: PermissionDto): PermissionDto {
		val permission = entityManager.find(Permission::class.java, id)
		entityManager.detach(permission)
		permission.updateFromDto(permissionDto)
		return entityManager.merge(permission).toDto()
	}

	@Transactional
	fun updatePermissionJpql(id: Long, permissionDto: PermissionDto): PermissionDto {
		val currentVersion = permissionRepository.findVersionById(id)
		if (currentVersion != permissionDto.version) {
			throw OptimisticLockException("Version mismatch")
		}

		val newVersion = permissionDto.version + 1
		val currentTime = System.currentTimeMillis()
		val modifiedBy = "system" // todo: get from security context

		val affectedRows = permissionRepository
			.updatePermissionNameAndDescriptionByIdAudited(
				id, permissionDto.name, permissionDto.description, currentVersion, newVersion, modifiedBy, currentTime)

		log.debug("[updatePermissionJpql] affectedRows: $affectedRows")
		return permissionRepository.findById(id).get().toDto()
	}

	fun deletePermission(id: Long) {
		permissionRepository.deleteById(id)
	}
}

@RestController
@RequestMapping("")
class PermissionController(
	private val permissionService: PermissionService,
	private val applicationEventPublisher: ApplicationEventPublisher,
	private val externalServiceClient: ExternalServiceClient
) {
	private val log = LogFactory.getLog(javaClass)

	@GetMapping("/permissions")
	fun getPermissions(@RequestParam(required = false, defaultValue = "0") page: Int,
					   @RequestParam(required = false, defaultValue = "10") size: Int,
					   @RequestParam(required = false, defaultValue = "id") sort: String,
					   @RequestParam(required = false, defaultValue = "ASC") direction: String
	): ResponseEntity<Page<PermissionDto>> {
		val result = permissionService.getAllPermissions(getPageRequest(page, size, sort, direction))
		return ResponseEntity.ok(result)
	}

	@GetMapping("/permissions/{id}")
	fun getPermission(@PathVariable id: Long): ResponseEntity<PermissionDto> {
		val result = permissionService.getPermission(id)
		return ResponseEntity.ok(result)
	}

	@PostMapping("/permissions")
	fun createPermission(@RequestBody permissionDto: PermissionDto): ResponseEntity<PermissionDto> {
		val result = permissionService.createPermission(permissionDto)
		log.info("sending event: PERMISSION_CREATED_JPA: ${result.id}")
		applicationEventPublisher.publishEvent(AppEvent("PERMISSION_CREATED_JPA: ${result.id}"))
		return ResponseEntity.ok(result)
	}

	@PutMapping("/permissions/{id}")
	fun updatePermission(@PathVariable id: Long, @RequestBody permissionDto: PermissionDto): ResponseEntity<PermissionDto> {
		val result = permissionService.updatePermission(id, permissionDto)
		return ResponseEntity.ok(result)
	}

	@PutMapping("/permissions/{id}/jpql")
	fun updatePermissionJpql(@PathVariable id: Long, @RequestBody permissionDto: PermissionDto): ResponseEntity<PermissionDto> {
		val result = permissionService.updatePermissionJpql(id, permissionDto)
		return ResponseEntity.ok(result)
	}

	@DeleteMapping("/permissions/{id}")
	fun deletePermission(@PathVariable id: Long) {
		permissionService.deletePermission(id)
	}

	@GetMapping("/error")
	fun error(@RequestParam(required = false, defaultValue = "generic error") message: String) {
		throw RuntimeException(message)
	}

	// todo: move to new controller
	@GetMapping("/external/{id}")
	fun getExternalResource(@PathVariable id: String): ResponseEntity<String> {
		val data = externalServiceClient.fetchExternalResource(id)
		return ResponseEntity.ok(data)
	}

}

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

@ControllerAdvice
class GlobalExceptionHandler {
	private val log = LogFactory.getLog(javaClass)

	@ExceptionHandler(value = [OptimisticLockException::class])
	fun handleOptimisticLockingException(e: Exception, request: WebRequest): ResponseEntity<Any> {
		log.error("[exception handler]: optimistic exception: ${e.message}, request: ${request.headerNames}")
		val errorType = ErrorType.OPTIMISTIC_LOCKING_EXCEPTION
		val errorDetails = ErrorDetails(timestamp = Instant.now(), errorType = errorType, errorCode = errorType.code,
			message = e.message, request = request.getDescription(true))
		return ResponseEntity(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR)
	}

	@ExceptionHandler(value = [Exception::class])
	fun handleException(e: Exception, request: WebRequest): ResponseEntity<Any> {
		log.error("[exception handler]: generic exception: ${e.message}, request: ${request.getDescription(true)}")
		val errorType = ErrorType.GENERIC_EXCEPTION
		val errorDetails = ErrorDetails(timestamp = Instant.now(), errorType = errorType, errorCode = errorType.code,
			message = e.message, request = request.getDescription(true))
		return ResponseEntity(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR)
	}
}

enum class ErrorType(val code: Int) {
	OPTIMISTIC_LOCKING_EXCEPTION(1),
	GENERIC_EXCEPTION(0);

	@JsonValue
	fun toJsonValue(): String {
		return this.name
	}

}

data class ErrorDetails(val timestamp: Instant, val errorType: ErrorType, val errorCode: Int, val message: String?, val request: String?)

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

@Configuration
@EnableKafka
class KafkaConfig {

	@Bean
	fun kafkaListenerContainerFactory(consumerFactory: ConsumerFactory<String, JsonNode>) =
		ConcurrentKafkaListenerContainerFactory<String, JsonNode>().also { it.consumerFactory = consumerFactory }

	@Bean
	fun consumerFactory() = DefaultKafkaConsumerFactory<String, JsonNode>(consumerProps)

	val consumerProps = mapOf(
		ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:9094",
		ConsumerConfig.GROUP_ID_CONFIG to "sample-group-jvm-jpa",
		ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
		ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
		JsonDeserializer.USE_TYPE_INFO_HEADERS to false,
		JsonDeserializer.TRUSTED_PACKAGES to "*",
		JsonDeserializer.VALUE_DEFAULT_TYPE to JsonNode::class.java,
		ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest"
	)

	@Bean
	fun producerFactory() = DefaultKafkaProducerFactory<String, KafkaMessage>(senderProps)

	val senderProps = mapOf(
		ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:9094",
		ProducerConfig.LINGER_MS_CONFIG to 10,
		ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
		ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java
	)

	@Bean
	fun kafkaTemplate(producerFactory: ProducerFactory<String, KafkaMessage>) = KafkaTemplate(producerFactory)
}

@Controller
class KafkaListenerController {
	private val log = LogFactory.getLog(javaClass)

	@KafkaListener(id = "template-mvc-jpa-0", topics = ["\${com.softeno.kafka.rx}"])
	fun onMessage(payload: JsonNode) {
		log.info("received payload: $payload")
		// todo: handle incoming event
	}

}

@JsonIgnoreProperties(ignoreUnknown = true)
data class KafkaMessage(val content: String)

@Service
class KafkaService(
	private val kafkaTemplate: KafkaTemplate<String, KafkaMessage>,
	@Value("\${com.softeno.kafka.tx}") private val topic: String
) {
	private val log = LogFactory.getLog(javaClass)

	fun send(message: KafkaMessage) {
		log.info("sending kafka message: $message")
		kafkaTemplate.send(topic, message)
	}
}

data class AppEvent(val source: String) : ApplicationEvent(source)

@Component
class SampleApplicationEventPublisher(private val kafkaService: KafkaService) : ApplicationListener<AppEvent> {
	private val log = LogFactory.getLog(javaClass)

	override fun onApplicationEvent(event: AppEvent) {
		log.info("received application event: $event")
		kafkaService.send(event.toKafkaMessage())
	}
}

fun AppEvent.toKafkaMessage() = KafkaMessage(content = this.source)

@EnableJpaRepositories
@EnableTransactionManagement
@EnableConfigurationProperties
@ConfigurationPropertiesScan("com.softeno")
@SpringBootApplication
class SoftenoMvcJpaApp

fun main(args: Array<String>) {
	runApplication<SoftenoMvcJpaApp>(*args)
}

