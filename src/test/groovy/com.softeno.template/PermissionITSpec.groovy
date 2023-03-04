package com.softeno.template

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus

class PermissionITSpec extends BaseAppSpec {

    @Autowired
    TestRestTemplate testRestTemplate

    @Autowired
    ObjectMapper objectMapper

    def "HTTP 200 /permissions and no data"() {
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
}
