package com.softeno.template.app.permission

import com.softeno.template.app.permission.mapper.PermissionDto

class PermissionFixture {
    companion object {
        @JvmStatic
        fun somePermissionDto(name: String, description: String) = PermissionDto(name = name, description = description,
            id = null, version = null, createdBy = null, createdDate = null, modifiedBy = null, modifiedDate = null)
    }
}