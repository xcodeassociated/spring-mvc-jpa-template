package com.softeno.template

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus

class PermissionITSpec extends BaseAppSpec {

    @Autowired
    TestRestTemplate testRestTemplate

    def "HTTP 200 /permissions and no data"() {
        expect:
        def expectedBody = "{\"content\":[],\"pageable\":{\"sort\":{\"empty\":false,\"sorted\":true,\"unsorted\":false},\"offset\":0,\"pageNumber\":0,\"pageSize\":10,\"paged\":true,\"unpaged\":false},\"last\":true,\"totalPages\":0,\"totalElements\":0,\"first\":true,\"size\":10,\"number\":0,\"sort\":{\"empty\":false,\"sorted\":true,\"unsorted\":false},\"numberOfElements\":0,\"empty\":true}"

        def response = testRestTemplate.getForEntity("/permissions", String.class)
        response.statusCode == HttpStatus.OK
        response.body == expectedBody
    }
}
