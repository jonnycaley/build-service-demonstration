package com.jonathancaley.buildservicedemonstration.services

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFailureResult
import org.gradle.tooling.events.task.TaskFinishEvent
import java.util.concurrent.atomic.AtomicInteger

abstract class BuildTaskService : BuildService<BuildServiceParameters.None>, OperationCompletionListener {

    val cachedTasksCount = AtomicInteger(0)
    val upToDateTasksCount = AtomicInteger(0)
    val executedTasksCount = AtomicInteger(0)

    var buildPhaseFailureMessage: String? = null
    val buildPhaseFailed: Boolean
        get() = buildPhaseFailureMessage != null

    override fun onFinish(event: FinishEvent?) {
        if (event == null || event !is TaskFinishEvent)
            return

        when {
            event.isFromCache() -> {
                cachedTasksCount.incrementAndGet()
            }
            event.isUpToDate() -> {
                upToDateTasksCount.incrementAndGet()
            }
            event.isSuccess() -> {
                executedTasksCount.incrementAndGet()
            }
        }

        if (event.result is TaskFailureResult) {
            buildPhaseFailureMessage = (event.result as TaskFailureResult).failures.firstOrNull()?.message ?: "${event.displayName} Failed without message"
        }
    }

    /**
     * The following functions are hacky workarounds to obtain a task execution result.
     * They are what I found to be the most consistent approach of obtaining this information.
     * As of this date and on gradle 7.4.2 they work. If task results are ever to change in the future,
     * these will need to be updated.
     */
    private fun FinishEvent.isUpToDate(): Boolean {
        return this.displayName.endsWith("UP-TO-DATE")
    }

    private fun FinishEvent.isFromCache(): Boolean {
        return this.displayName.endsWith("FROM-CACHE")
    }

    private fun FinishEvent.isSuccess(): Boolean {
        return this.displayName.endsWith("SUCCESS")
    }
}