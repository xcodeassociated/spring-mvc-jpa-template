package com.softeno.template.app.permission.api

import com.softeno.template.app.common.events.AppEvent
import com.softeno.template.app.permission.db.getPageRequest
import com.softeno.template.app.permission.mapper.PermissionDto
import com.softeno.template.app.permission.service.PermissionService
import org.apache.commons.logging.LogFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
class PermissionController(
    private val permissionService: PermissionService,
    private val applicationEventPublisher: ApplicationEventPublisher
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
}