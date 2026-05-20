package com.memoir

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MemoirBackendApplication

fun main(args: Array<String>) {
	runApplication<MemoirBackendApplication>(*args)
}
