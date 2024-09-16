package com.softeno.template.app.kafka

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class KafkaMessage(val content: String)