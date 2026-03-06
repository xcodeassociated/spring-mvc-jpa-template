package com.softeno.template.app.kafka

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import org.apache.commons.logging.LogFactory
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller

@ConfigurationProperties(prefix = "com.softeno.kafka")
data class KafkaApplicationProperties(val tx: String, val rx: String, val keycloak: String)

@Controller
class KafkaSampleController(
    private val props: KafkaApplicationProperties
) {
    private val log = LogFactory.getLog(javaClass)

    @KafkaListener(id = "\${spring.kafka.consumer.group-id}", topics = ["\${com.softeno.kafka.rx}"])
    fun listen(record: ConsumerRecord<String, JsonNode>) {
        log.info("[kafka] rx (${props.rx}): ${record.key()}: ${record.value()}")
    }
}

@Component
class KafkaSampleProducer(
    private val producer: KafkaTemplate<String, KafkaMessage>,
    private val props: KafkaApplicationProperties
) {
    private val log = LogFactory.getLog(javaClass)

    fun send(message: KafkaMessage) {
        log.info("[kafka] tx (${props.tx}): $message")
        producer.send(props.tx, message)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class KafkaMessage(val content: String)