package com.jonathancaley.buildservicedemonstration.services

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFailureResult
import org.gradle.tooling.events.task.TaskFinishEvent

abstract class BuildTaskService : BuildService<BuildServiceParameters.None>, OperationCompletionListener {

    var fromCacheTasksCount = 0
    var upToDateTasksCount = 0
    var executedTasksCount = 0

    var buildPhaseFailureMessage: String? = null
    val buildPhaseFailed: Boolean
        get() = buildPhaseFailureMessage != null

    override fun onFinish(event: FinishEvent?) {
        if (event == null || event !is TaskFinishEvent)
            return

        when {
            event.isFromCache() -> {
                fromCacheTasksCount++
            }
            event.isUpToDate() -> {
                upToDateTasksCount++
            }
            event.isSuccess() -> {
                executedTasksCount++
            }
        }

        if (event.result is TaskFailureResult) {
            buildPhaseFailureMessage = (event.result as TaskFailureResult).failures.firstOrNull()?.message ?: "${event.displayName} Failed without message"
        }
    }

    /**
     * The following functions are hacky workarounds to obtain a task execution result.
     * They are what I found to be the most consistent approach of obtaining this information (I tried
     * using similar logic [here](https://github.com/jrodbx/agp-sources/blob/3b6b17156dfcc8717c1bf217743cea8d15e034d2/7.1.3/com.android.tools.build/gradle/com/android/build/gradle/internal/profile/AnalyticsResourceManager.kt#L158).
     * but failed as explained [here](https://github.com/gradle/gradle/issues/5252).
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