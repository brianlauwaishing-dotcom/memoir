package com.memoir.common.web

import org.springframework.http.HttpStatus

class ApiException(
    val status: HttpStatus,
    val code: String,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
