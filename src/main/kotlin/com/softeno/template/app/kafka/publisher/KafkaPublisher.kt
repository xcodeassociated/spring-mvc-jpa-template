package com.softeno.template.app.kafka.publisher

import com.softeno.template.app.kafka.KafkaMessage
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Profile(value = ["!integration"])
@Service
class KafkaPublisher(
    private val kafkaTemplate: KafkaTemplate<String, KafkaMessage>,
    @Value("\${com.softeno.kafka.tx}") private val topic: String
) {
    private val log = LogFactory.getLog(javaClass)

    fun send(message: KafkaMessage) {
        log.info("sending kafka message: $message")
        kafkaTemplate.send(topic, message)
    }
}