@file:JvmName("PermissionFixture")
package com.softeno.template.fixture

import com.softeno.template.PermissionDto

class PermissionFixture {
    companion object {
        @JvmStatic
        fun somePermissionDto(name: String, description: String) = PermissionDto(name = name, description = description,
            id = null, version = null, createdBy = null, createdDate = null, modifiedBy = null, modifiedDate = null)
    }
}