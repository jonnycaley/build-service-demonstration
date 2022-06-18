package com.jonathancaley.buildservicedemonstration.services

import com.jonathancaley.buildservicedemonstration.report.BuildReport
import com.jonathancaley.buildservicedemonstration.report.getNumberOfDaemons
import org.gradle.api.internal.tasks.execution.statistics.TaskExecutionStatistics
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.internal.operations.BuildOperationDescriptor
import org.gradle.internal.operations.BuildOperationListener
import org.gradle.internal.operations.OperationFinishEvent
import org.gradle.internal.operations.OperationIdentifier
import org.gradle.internal.operations.OperationProgressEvent
import org.gradle.internal.operations.OperationStartEvent

abstract class BuildReporterService : BuildService<BuildReporterService.Params>, BuildOperationListener, AutoCloseable {

    interface Params : BuildServiceParameters {
        /**
         * CACHEABLE VALUES
         *
         * These parameters are obtained from the configuration cache when
         * enabled and used and thus must not change in between cached runs.
         * For example, the project name is defined in settings.gradle and
         * changing it will require the configuration cache to be invalidated
         * so we are safe to parse it as a parameter as we know it won't change
         * during cached runs.
         */
        fun getBuildTaskNames(): Property<String>
        fun getGradleVersion(): Property<String>
        fun getIsEnabledConsole(): Property<Boolean>

        fun getBuildDurationServiceProvider(): Property<Provider<BuildDurationService>>
        fun getBuildTaskServiceProvider(): Property<Provider<BuildTaskService>>
    }

    /**
     * UN-CACHEABLE VALUES
     *
     * These parameters cannot be obtained from the configuration cache when
     * enabled and used as they could change in between cached runs.
     * For example, The number of daemons running could increase in between
     * cached runs due to another process starting. Therefore, we cannot parse
     * this as a BuildReporterService.Params otherwise it will be cached and not
     * changed between configuration cached runs.
     */
    private val daemonsRunning: Int
        get() = getNumberOfDaemons()

    override fun started(p0: BuildOperationDescriptor, p1: OperationStartEvent) {}

    override fun progress(p0: OperationIdentifier, p1: OperationProgressEvent) {}

    override fun finished(buildOperationDescriptor: BuildOperationDescriptor, operationFinishEvent: OperationFinishEvent) {}

    override fun close() {
        logBuildStats()
    }

    private fun logBuildStats() {
        val buildDurationService = parameters.getBuildDurationServiceProvider().get().get()
        val buildTaskService = parameters.getBuildTaskServiceProvider().get().get()
        val buildReport = getBuildReport(buildDurationService, buildTaskService)

        val logger = Logging.getLogger("console-logger")
        logger.lifecycle(buildReport.toString())
    }

    private fun getBuildReport(
        buildDurationService: BuildDurationService,
        buildTaskService: BuildTaskService,
    ): BuildReport {
        val taskExecutionStatistics = getTasksExecutionStatistics(buildTaskService)
        val buildFailureMessage = if (buildTaskService.buildPhaseFailed) {
            buildTaskService.buildPhaseFailureMessage ?: "Build failed"
        } else if (buildDurationService.configurationPhaseFailed) {
            "Configuration Phase Failed"
        } else {
            null
        }
        val buildFailed = (buildFailureMessage != null)

        return BuildReport(
            totalElapsedBuildTimeMs = buildDurationService.buildDuration,
            configurationTimeMs = buildDurationService.configurationDuration,
            taskExecutionStatistics = taskExecutionStatistics,
            buildFailed = buildFailed,
            buildFailureMessage = buildFailureMessage,
            buildTaskNames = parameters.getBuildTaskNames().get(),
            gradleVersion = parameters.getGradleVersion().get(),
            daemonsRunning = daemonsRunning
        )
    }

    private fun getTasksExecutionStatistics(buildTaskService: BuildTaskService): TaskExecutionStatistics {
        return TaskExecutionStatistics(
            buildTaskService.executedTasksCount,
            buildTaskService.fromCacheTasksCount,
            buildTaskService.upToDateTasksCount
        )
    }
}