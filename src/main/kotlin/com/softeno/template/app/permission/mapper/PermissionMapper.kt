package com.softeno.template.app.permission.mapper

import com.softeno.template.app.permission.Permission

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
