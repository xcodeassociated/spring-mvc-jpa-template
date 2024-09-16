package com.softeno.template.app.common.db

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Version
import org.hibernate.annotations.OptimisticLockType
import org.hibernate.annotations.OptimisticLocking
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.util.UUID

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