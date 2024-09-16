package com.softeno.template.app.kafka.receiver

import com.fasterxml.jackson.databind.JsonNode
import org.apache.commons.logging.LogFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Controller


@Profile(value = ["!integration"])
@Controller
class KafkaReceiver {
    private val log = LogFactory.getLog(javaClass)

    @KafkaListener(id = "template-mvc-jpa-0", topics = ["\${com.softeno.kafka.rx}"])
    fun onMessage(payload: JsonNode) {
        log.info("received payload: $payload")
        // todo: handle incoming event
    }

}