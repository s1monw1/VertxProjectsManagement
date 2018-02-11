package de.swirtz.kotlinvertx.rest

import de.swirtz.kotlinvertx.rest.configuration.ServiceConfigProvider
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * @author: SIM
 *
 * File created on 06.06.2017.
 */
class ServiceConfigProviderTest {

    @Test
    fun getValTest() {
        val serviceConfigProvider = createProvider()
        assertEquals("myval", serviceConfigProvider.get("mykey"))
    }

    @Test
    fun illegalKeyTest() {
        val serviceConfigProvider = createProvider()
        assertFailsWith(IllegalArgumentException::class) {
            serviceConfigProvider.get("mykey2")
        }
    }

    private fun createProvider(): ServiceConfigProvider = ServiceConfigProvider(propFile = "test.properties",
            localSearchPath = "src/test/resources")

}