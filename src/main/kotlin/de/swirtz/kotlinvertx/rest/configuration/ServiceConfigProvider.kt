package de.swirtz.kotlinvertx.rest.configuration

import org.slf4j.LoggerFactory
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

/**
 * Merges Properties from *.properties files found in [localSearchPath] with [cmdLineArgs], which will take
 * precedence over local properties if provided with equal name.
 * Properties from cmdLineArgs should look like "--propertyname=propertyvalue". Every property found like this will
 * override properties in localSearchPath.
 */
class ServiceConfigProvider(
    private val propFile: String = "projects.properties",
    private val localSearchPath: String = "src/main/resources",
    cmdLineArgs: Array<String>? = null
) {

    private val configuration = mutableMapOf<String, String>()

    companion object {
        private val LOG = LoggerFactory.getLogger(ServiceConfigProvider::class.java)
    }

    init {
        LOG.debug("Init with cmdLineArgs='${cmdLineArgs?.joinToString(separator = ", ")}'")
        populateFromFile()
        populateFromCmdArgs(cmdLineArgs)
        LOG.debug("Init finished. Got ${configuration.size} values")
    }

    fun get(key: String) =
        configuration[key] ?: throw IllegalArgumentException("No configured value present for key='{$key}'")

    fun getNullable(key: String) = configuration[key]

    private fun populateFromCmdArgs(cmdLineArgs: Array<String>?) = cmdLineArgs?.let { args ->
        args.map { it.removePrefix("--").split("=", ":") }
            .filter { it.size == 2 }
            .forEach { configuration[it[0]] = it[1] }
    }

    private fun populateFromFile() = getPropertiesStream().use {
        with(Properties()) {
            load(it)
            forEach {
                configuration[it.key as String] = it.value as String
            }
        }
    }

    private fun getPropertiesStream(): InputStream {
        LOG.debug("Try to load properties file: $this")
        with(Paths.get(localSearchPath, propFile)) {
            if (Files.exists(this)
            ) {
                LOG.debug("Found properties in search path: $this")
                return Files.newInputStream(this)
            }
        }
        LOG.debug("Load from Classpath")
        return ServiceConfigProvider::class.java.classLoader.getResourceAsStream(propFile) ?: throw
        IllegalStateException("$propFile not found in $localSearchPath or classpath!")
    }


}