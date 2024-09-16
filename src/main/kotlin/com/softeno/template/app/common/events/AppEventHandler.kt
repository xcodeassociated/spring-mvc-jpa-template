package com.softeno.template.app.common.events

import com.softeno.template.app.kafka.KafkaMessage
import com.softeno.template.app.kafka.publisher.KafkaPublisher
import org.apache.commons.logging.LogFactory
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile(value = ["!integration"])
@Component
class AppEventHandler(private val kafkaPublisher: KafkaPublisher) : ApplicationListener<AppEvent> {
    private val log = LogFactory.getLog(javaClass)

    override fun onApplicationEvent(event: AppEvent) {
        log.info("received application event: $event")
        kafkaPublisher.send(event.toKafkaMessage())
    }
}

fun AppEvent.toKafkaMessage() = KafkaMessage(content = this.source)