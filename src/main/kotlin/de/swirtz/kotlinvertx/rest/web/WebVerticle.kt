package de.swirtz.kotlinvertx.rest.web

import de.swirtz.kotlinvertx.rest.INC_GET_ALL
import de.swirtz.kotlinvertx.rest.JSON_CONT_TYPE
import de.swirtz.kotlinvertx.rest.REST_SRV
import de.swirtz.kotlinvertx.rest.data.ErrorResponse
import de.swirtz.kotlinvertx.rest.data.ErrorResponse.ApplicationErrorCodes.UNSPECIFIC
import de.swirtz.kotlinvertx.rest.json
import de.swirtz.kotlinvertx.rest.web.HandlerCreationService.httpDeleteAllHandler
import de.swirtz.kotlinvertx.rest.web.HandlerCreationService.httpDeleteHandler
import de.swirtz.kotlinvertx.rest.web.HandlerCreationService.httpGetAllHandler
import de.swirtz.kotlinvertx.rest.web.HandlerCreationService.httpGetHandler
import de.swirtz.kotlinvertx.rest.web.HandlerCreationService.httpPostHandler
import de.swirtz.kotlinvertx.rest.web.HandlerCreationService.httpPutHandler
import io.vertx.core.eventbus.Message
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServer
import io.vertx.ext.healthchecks.HealthCheckHandler
import io.vertx.ext.healthchecks.Status
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.ResponseContentTypeHandler
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.launch
import org.slf4j.LoggerFactory
import java.net.HttpURLConnection

/**
 * Verticle to start a Webserver with different routes.
 */
class WebVerticle(private val port: Int = 8080) : CoroutineVerticle() {

    companion object {
        private val LOG = LoggerFactory.getLogger(WebVerticle::class.java)
    }

    private lateinit var server: HttpServer

    override suspend fun stop() {
        server.close {
            LOG.info("WebServer not listening anymore.")
        }
    }

    override suspend fun start() {
        LOG.debug("WebVerticle start called; Going to register Router")
        val router = Router.router(vertx).apply {
            fun getGeneralRoute() = routeWithRegex("$REST_SRV.*")
            getGeneralRoute().handler(BodyHandler.create())
            getGeneralRoute().handler(ResponseContentTypeHandler.create())
            HealthCheckHandler.create(vertx).register("Event-getAll-Receiver-Check") { proc ->
                launch(vertx.dispatcher()) {
                    try {
                        awaitResult<Message<Any>> {
                            vertx.eventBus().send(INC_GET_ALL, null, it)
                        }.also { proc.complete(Status.OK()) }
                    } catch (e: Exception) {
                        LOG.error("Health check for getAll failed!", e)
                        proc.complete(Status.KO())
                    }
                }
            }.let {
                    route("/health").handler(it)
                }

            getGeneralRoute().failureHandler { failureCtx ->
                val jsonObject = failureCtx.failure().run {
                    LOG.debug("FailureHandler called with failure '$this'")
                    ErrorResponse(message ?: "Undefined Error", UNSPECIFIC.code).json
                }

                failureCtx.response().setStatusCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
                    .end(jsonObject.encodePrettily())
            }
            route("/static/*").handler(StaticHandler.create())
            extendWithRestEndpoints()
        }

        server = vertx.createHttpServer().requestHandler(router::accept).listen(port) {
            LOG.info("WebServer listening on $port")
        }

    }

    private suspend fun Router.extendWithRestEndpoints() {
        fun projectsRestRoute(method: HttpMethod, path: String, handler: (RoutingContext) -> Unit) =
            route(method, path).produces(JSON_CONT_TYPE).handler(handler)

        projectsRestRoute(HttpMethod.GET, "$REST_SRV/:projectId", httpGetHandler(vertx))
        projectsRestRoute(HttpMethod.GET, REST_SRV, httpGetAllHandler(vertx))
        projectsRestRoute(HttpMethod.PUT, REST_SRV, httpPutHandler(vertx))
        projectsRestRoute(HttpMethod.POST, REST_SRV, httpPostHandler(vertx))
        projectsRestRoute(HttpMethod.DELETE, "$REST_SRV/:projectId", httpDeleteHandler(vertx))
        projectsRestRoute(HttpMethod.DELETE, REST_SRV, httpDeleteAllHandler(vertx))
    }


}