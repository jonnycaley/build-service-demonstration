package com.jonathancaley.buildservicedemonstration.report

import org.gradle.api.internal.tasks.execution.statistics.TaskExecutionStatistics

data class BuildReport(
    val totalElapsedBuildTimeMs: Long?,
    val configurationTimeMs: Long?,
    val taskExecutionStatistics: TaskExecutionStatistics,
    val buildFailed: Boolean,
    val buildFailureMessage: String?,
    val buildTaskNames: String,
    val gradleVersion: String,
    val daemonsRunning: Int
)
