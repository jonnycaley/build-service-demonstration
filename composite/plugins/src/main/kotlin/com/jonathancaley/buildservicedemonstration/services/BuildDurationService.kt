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

abstract class BuildDurationService : BuildService<BuildServiceParameters.None>, BuildOperationListener {

    var buildDuration: Long? = null

    var configurationDuration: Long? = null
    var configurationPhaseFailed = true

    override fun started(p0: BuildOperationDescriptor, p1: OperationStartEvent) {}

    override fun progress(p0: OperationIdentifier, p1: OperationProgressEvent) {}

    override fun finished(buildOperationDescriptor: BuildOperationDescriptor, operationFinishEvent: OperationFinishEvent) {
        if (buildOperationDescriptor.details is RunRootBuildWorkBuildOperationType.Details) {
            /**
             * Runs when build phase finishes, therefore we can assume configuration phase passed
             */
            configurationPhaseFailed = false

            val details: RunRootBuildWorkBuildOperationType.Details? = buildOperationDescriptor.details as RunRootBuildWorkBuildOperationType.Details?
            details?.buildStartTime?.let { buildStartTime ->
                buildDuration = System.currentTimeMillis() - buildStartTime

                val firstTaskStartTime = operationFinishEvent.startTime
                this.configurationDuration = firstTaskStartTime - buildStartTime
            }
        }
    }
}