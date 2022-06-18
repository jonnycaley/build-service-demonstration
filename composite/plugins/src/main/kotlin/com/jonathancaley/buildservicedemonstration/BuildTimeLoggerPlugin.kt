package com.jonathancaley.buildservicedemonstration

import com.jonathancaley.buildservicedemonstration.services.BuildDurationService
import com.jonathancaley.buildservicedemonstration.services.BuildReporterService
import com.jonathancaley.buildservicedemonstration.services.BuildTaskService
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.Provider
import org.gradle.internal.build.event.BuildEventListenerRegistryInternal
import org.gradle.internal.service.ServiceRegistry
import org.gradle.invocation.DefaultGradle

class BuildTimeLoggerPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        val extension = project.extensions.create("buildTimeLogger", BuildTimeLoggerPluginExtension::class.java)

        val buildDurationService = registerBuildDurationService(gradle)
        val buildTaskService = registerBuildTaskService(gradle)
        registerBuildReporterService(gradle, extension, buildDurationService, buildTaskService)
    }

    private fun registerBuildTaskService(gradle: Gradle): Provider<BuildTaskService> {
        val registry = gradle.serviceRegistry()[BuildEventListenerRegistryInternal::class.java]
        val buildTaskService = gradle.sharedServices.registerIfAbsent("build-task-service", BuildTaskService::class.java) { }

        registry.onTaskCompletion(buildTaskService)

        return buildTaskService
    }

    private fun registerBuildDurationService(gradle: Gradle): Provider<BuildDurationService> {
        val registry = gradle.serviceRegistry()[BuildEventListenerRegistryInternal::class.java]
        val buildDurationService = gradle.sharedServices.registerIfAbsent("build-duration-service", BuildDurationService::class.java) { }

        registry.onOperationCompletion(buildDurationService)

        return buildDurationService
    }

    private fun registerBuildReporterService(
        gradle: Gradle,
        extension: BuildTimeLoggerPluginExtension,
        buildDurationService: Provider<BuildDurationService>,
        buildTaskService: Provider<BuildTaskService>
    ): Provider<BuildReporterService> {
        val registry = gradle.serviceRegistry()[BuildEventListenerRegistryInternal::class.java]
        val buildReporterService = gradle.sharedServices.registerIfAbsent("build-reporter-service", BuildReporterService::class.java) { service ->
            service.parameters.getBuildTaskNames().set(gradle.startParameter.taskNames.joinToString())
            service.parameters.getGradleVersion().set(gradle.gradleVersion)
            service.parameters.getIsEnabledConsole().set(extension.enableConsole)

            service.parameters.getBuildDurationServiceProvider().set(buildDurationService)
            service.parameters.getBuildTaskServiceProvider().set(buildTaskService)
        }

        registry.onOperationCompletion(buildReporterService) // gives gradle a reason to instantiate the build service and call onClose at the end of the build

        return buildReporterService
    }
}

fun Gradle.serviceRegistry(): ServiceRegistry = (this as DefaultGradle).services