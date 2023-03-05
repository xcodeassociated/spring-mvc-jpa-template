package com.softeno.template

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.softeno.template.fixture.PermissionFixture
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus

class PermissionITSpec extends BaseAppSpec {

    @Autowired
    TestRestTemplate testRestTemplate

    @Autowired
    ObjectMapper objectMapper

    def "get permissions ok with no data"() {
        expect:
        def expectedContent = [] as PermissionDto[]
        def expectedTotalElements = 0
        def expectedTotalPages = 0

        def response = testRestTemplate.getForEntity("/permissions", String.class)
        def responseBody = objectMapper.readTree(response.body) as JsonNode
        def reader = objectMapper.readerForArrayOf(PermissionDto.class)

        def responseContent = reader.readValue(responseBody.get("content")) as PermissionDto[]

        response.statusCode == HttpStatus.OK
        responseBody.get("totalElements").asInt() == expectedTotalElements
        responseBody.get("totalPages").asInt() == expectedTotalPages
        responseContent == expectedContent
    }

    def "post new permission"() {
        given:
        def expectedPermissionName = UUID.randomUUID().toString()
        def expectedPermissionDescription = UUID.randomUUID().toString()
        def permission = PermissionFixture.somePermissionDto(expectedPermissionName, expectedPermissionDescription)

        expect:
        def response = testRestTemplate.postForEntity("/permissions", permission, String.class)
        def result = objectMapper.readValue(response.body, PermissionDto.class)
        response.statusCode == HttpStatus.OK
        result.name == expectedPermissionName
        result.description == expectedPermissionDescription
    }
}