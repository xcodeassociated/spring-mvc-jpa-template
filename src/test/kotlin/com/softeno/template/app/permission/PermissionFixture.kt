package com.softeno.template.app.permission

import com.softeno.template.app.permission.mapper.PermissionDto

class PermissionFixture {
    companion object {
        fun aPermission(name: String = "some permission", description: String = "some description"): Permission {
            val permission = Permission()
            permission.id = null
            permission.createdDate = null
            permission.createdBy = null
            permission.modifiedBy = null
            permission.modifiedDate = null
            permission.version = null
            permission.name = name
            permission.description = description

            return permission
        }

        fun aPermissionDto(name: String = "some permission", description: String = "some description") = PermissionDto(name = name, description = description,
            id = null, version = null, createdBy = null, createdDate = null, modifiedBy = null, modifiedDate = null)
    }
}