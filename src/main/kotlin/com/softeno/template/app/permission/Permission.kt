package com.softeno.template.app.permission

import com.softeno.template.app.common.db.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "permissions")
class Permission(uuid: UUID = UUID.randomUUID()) : BaseEntity(uuid) {

	@Column(unique = true, nullable = false)
	var name: String? = null

	@Column(nullable = false, columnDefinition = "TEXT")
	var description: String? = null
}