package com.softeno.template

import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.*
import jakarta.transaction.Transactional
import org.apache.commons.logging.LogFactory
import org.hibernate.annotations.OptimisticLockType
import org.hibernate.annotations.OptimisticLocking
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.WebRequest
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

	@Column(nullable = false)
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
class PermissionController(private val permissionService: PermissionService) {

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

// todo: get from security context
class AuditorAwareImpl : AuditorAware<String> {
	override fun getCurrentAuditor(): Optional<String> {
		return Optional.of("system")
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

@EnableJpaRepositories
@EnableJpaAuditing
@EnableTransactionManagement
@SpringBootApplication
class SoftenoMvcJpaApp

fun main(args: Array<String>) {
	runApplication<SoftenoMvcJpaApp>(*args)
}

