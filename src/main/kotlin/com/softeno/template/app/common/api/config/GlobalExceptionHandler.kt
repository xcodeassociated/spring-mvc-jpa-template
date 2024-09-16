package com.softeno.template.app.common.api.config

import com.softeno.template.app.common.error.ErrorType
import jakarta.persistence.OptimisticLockException
import org.apache.commons.logging.LogFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import java.time.Instant

@ControllerAdvice
class GlobalExceptionHandler {
    private val log = LogFactory.getLog(javaClass)

    @ExceptionHandler(value = [OptimisticLockException::class])
    fun handleOptimisticLockingException(e: Exception, request: WebRequest): ResponseEntity<Any> {
        log.error("[exception handler]: optimistic exception: ${e.message}, request: ${request.headerNames}")
        val errorType = ErrorType.OPTIMISTIC_LOCKING_EXCEPTION
        val errorDetails = ErrorDetails(timestamp = Instant.now(), errorType = errorType, errorCode = errorType.code,
            message = e.message, request = request.getDescription(true))
        return ResponseEntity(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(value = [Exception::class])
    fun handleException(e: Exception, request: WebRequest): ResponseEntity<Any> {
        log.error("[exception handler]: generic exception: ${e.message}, request: ${request.getDescription(true)}")
        val errorType = ErrorType.GENERIC_EXCEPTION
        val errorDetails = ErrorDetails(timestamp = Instant.now(), errorType = errorType, errorCode = errorType.code,
            message = e.message, request = request.getDescription(true))
        return ResponseEntity(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}

data class ErrorDetails(val timestamp: Instant, val errorType: ErrorType, val errorCode: Int, val message: String?, val request: String?)
