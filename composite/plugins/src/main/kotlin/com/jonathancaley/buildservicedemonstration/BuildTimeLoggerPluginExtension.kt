package com.jonathancaley.buildservicedemonstration

import org.gradle.api.provider.Property

abstract class BuildTimeLoggerPluginExtension {
    abstract val enableConsole: Property<Boolean>
}