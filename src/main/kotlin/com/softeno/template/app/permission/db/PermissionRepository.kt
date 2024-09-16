package com.softeno.template.app.permission.db

import com.softeno.template.app.permission.Permission
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.querydsl.QuerydslPredicateExecutor
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

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