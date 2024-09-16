package com.softeno.template.app.common.events

import org.springframework.context.ApplicationEvent

data class AppEvent(val source: String) : ApplicationEvent(source)
