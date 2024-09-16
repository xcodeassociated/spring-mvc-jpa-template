package com.softeno.template.app.common.error

import com.fasterxml.jackson.annotation.JsonValue

enum class ErrorType(val code: Int) {
    OPTIMISTIC_LOCKING_EXCEPTION(1),
    GENERIC_EXCEPTION(0);

    @JsonValue
    fun toJsonValue(): String {
        return this.name
    }
}
