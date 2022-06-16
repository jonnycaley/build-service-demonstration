package com.jonathancaley.buildservicedemonstration.services

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.execution.RunRootBuildWorkBuildOperationType
import org.gradle.internal.operations.BuildOperationDescriptor
import org.gradle.internal.operations.BuildOperationListener
import org.gradle.internal.operations.OperationFinishEvent
import org.gradle.internal.operations.OperationIdentifier
import org.gradle.internal.operations.OperationProgressEvent
import org.gradle.internal.operations.OperationStartEvent

abstract class BuildDurationService : BuildService<BuildServiceParameters.None>, BuildOperationListener, AutoCloseable {

    private var buildStartTime: Long? = null
    var buildDuration: Long? = null
    var configurationDuration: Long? = null
    var configurationPhaseFailed = true

    override fun started(p0: BuildOperationDescriptor, p1: OperationStartEvent) {}

    override fun progress(p0: OperationIdentifier, p1: OperationProgressEvent) {}

    override fun finished(buildOperationDescriptor: BuildOperationDescriptor, operationFinishEvent: OperationFinishEvent) {
        if (buildOperationDescriptor.details is RunRootBuildWorkBuildOperationType.Details) {
            /**
             * Runs when configuration phase has finished
             */
            configurationPhaseFailed = false

            val details: RunRootBuildWorkBuildOperationType.Details? = buildOperationDescriptor.details as RunRootBuildWorkBuildOperationType.Details?
            buildStartTime = details?.buildStartTime

            details?.buildStartTime?.let { buildStartTime ->
                val firstTaskStartTime = operationFinishEvent.startTime
                this.configurationDuration = firstTaskStartTime - buildStartTime
            }
        }
    }

    override fun close() {
        buildStartTime?.let {
            buildDuration = System.currentTimeMillis() - it
        }
    }
}