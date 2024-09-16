package com.softeno.template.app.permission.service

import com.softeno.template.app.permission.Permission
import com.softeno.template.app.permission.db.PermissionRepository
import com.softeno.template.app.permission.mapper.PermissionDto
import com.softeno.template.app.permission.mapper.toDto
import com.softeno.template.app.permission.mapper.updateFromDto
import jakarta.persistence.EntityManager
import jakarta.persistence.OptimisticLockException
import jakarta.transaction.Transactional
import org.apache.commons.logging.LogFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

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