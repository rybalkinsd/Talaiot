package com.cdsap.talaiot.entities

data class TaskLength(
    val ms: Long,
    val taskName: String,
    val taskPath: String,
    val state: TaskMessageState,
    val rootNode: Boolean = false,
    val module: String,
    val taskDependencies: List<String>
)
