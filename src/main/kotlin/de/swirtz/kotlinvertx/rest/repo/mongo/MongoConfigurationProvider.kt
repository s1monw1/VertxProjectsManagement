package de.swirtz.kotlinvertx.rest.repo.mongo

import de.bwaldvogel.mongo.MongoServer
import de.bwaldvogel.mongo.backend.h2.H2Backend
import de.swirtz.kotlinvertx.rest.configuration.ServiceConfigProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files

data class MongoClientConfig(val dbName: String, val host: String, val port: Int)

/**
 * Provides method for creating instance of [MongoClientConfig].
 */
object MongoConfigurationProvider {

    private val LOG: Logger = LoggerFactory.getLogger(MongoConfigurationProvider::class.java)

    /**
     * Method for creating [MongoClientConfig] with the help of a [ServiceConfigProvider]. Following properties are mandatory:
     * *mongo.port*, *mongo.host*, *mongo.dbname*
     *
     * If *profile* property is set and contains value "dev", an in-memory database will be started.
     */
    fun createConfig(configProv: ServiceConfigProvider): MongoClientConfig {
        configProv.getNullable("profile")?.let {
            if (it == "dev") {
                LOG.debug("Start application in ###dev### Profile, Start in-memory database")
                val port = configProv.get("mongo.port").toInt()
                MongoServer(H2Backend(Files.createTempFile("db", ".mv").toString())).bind("localhost", port)
            }
        }

        return MongoClientConfig(
            configProv.get("mongo.dbname"),
            configProv.get("mongo.host"),
            configProv.get("mongo.port").toInt()
        )
    }

}