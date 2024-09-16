package com.softeno.template.app

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

import java.sql.ResultSet
import java.sql.Statement

class SoftenoAppSpec extends BaseAppSpec {

    @Autowired
    ApplicationContext context

    def "contextLoads"() {
        expect:
        context.id == "SoftenoJpaPostgresApp"
    }

    def "container is running"() {
        expect:
        postgreSQLContainer.isRunning()
    }

    def "waits until postgres accepts jdbc connections"() {

        given: "a jdbc connection"
        HikariConfig hikariConfig = new HikariConfig()
        hikariConfig.setJdbcUrl(postgreSQLContainer.jdbcUrl)
        hikariConfig.setUsername("admin")
        hikariConfig.setPassword("admin")
        HikariDataSource ds = new HikariDataSource(hikariConfig)

        when: "querying the database"
        Statement statement = ds.getConnection().createStatement()
        statement.execute("SELECT 1")
        ResultSet resultSet = statement.getResultSet()
        resultSet.next()

        then: "result is returned"
        int resultSetInt = resultSet.getInt(1)
        resultSetInt == 1

        cleanup:
        ds.close()
    }


}
