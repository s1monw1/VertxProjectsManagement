package de.swirtz.kotlinvertx.rest

import de.swirtz.kotlinvertx.rest.configuration.ServiceConfigProvider
import de.swirtz.kotlinvertx.rest.repo.mongo.MongoConfigurationProvider
import de.swirtz.kotlinvertx.rest.repo.mongo.MongoRepository
import de.swirtz.kotlinvertx.rest.repo.service.ProjectsServiceVerticle
import de.swirtz.kotlinvertx.rest.web.WebVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Verticle
import io.vertx.core.Vertx
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Main entry point which starts the application by deploying our [io.vertx.core.Verticle]s. It also creates and
 * contains the only [Vertx] instance of the whole application
 */

private val LOG: Logger = LoggerFactory.getLogger("Projects-Service")

fun Vertx.deploy(verticle: Verticle, opt: DeploymentOptions = DeploymentOptions()) {
    deployVerticle(verticle, opt, { deploy ->
        LOG.info("${verticle::class} has been deployed? ${deploy.succeeded()}")
        if (!deploy.succeeded()) {
            LOG.error("${verticle::class} deploy failed: ${deploy.cause()}", deploy.cause())
        }
    })
}

fun main(args: Array<String>) {
    with(Vertx.vertx()) {
        val configProvider = ServiceConfigProvider(cmdLineArgs = args)
        val repo = MongoRepository(MongoConfigurationProvider.createConfig(configProvider), this)

        deploy(WebVerticle(configProvider.get("server.port").toInt()))
        deploy(ProjectsServiceVerticle(repo))
    }

}

