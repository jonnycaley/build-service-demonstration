package com.jonathancaley.buildservicedemonstration.report

import org.codehaus.groovy.runtime.ProcessGroovyMethods
import org.gradle.api.Project
import org.gradle.internal.jvm.Jvm
import java.io.File

fun getNumberOfDaemons(): Int {
    return runProcess("/bin/bash", "-c", "ps aux | grep GradleDaemon | wc -l")?.toInt()?.minus(2) ?: 0
}

private fun runProcess(vararg args: String): String? = try {
    Runtime.getRuntime().exec(args).run {
        waitFor()
        if (exitValue() == 0)
            ProcessGroovyMethods.getText(this).trim()
        else
            null
    }
} catch (ignored: Exception) {
    null
}