package de.swirtz.kotlinvertx.rest.web

import de.swirtz.kotlinvertx.rest.*
import de.swirtz.kotlinvertx.rest.data.DeleteProjectRequest
import de.swirtz.kotlinvertx.rest.data.ErrorResponse
import de.swirtz.kotlinvertx.rest.data.GetProjectRequest
import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.ReplyException
import io.vertx.core.eventbus.ReplyFailure
import io.vertx.core.http.HttpServerResponse
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.launch
import org.slf4j.LoggerFactory
import java.net.HttpURLConnection


object HandlerCreationService {
    private val LOG = LoggerFactory.getLogger(HandlerCreationService::class.java)

    suspend fun httpGetHandler(vertx: Vertx): (RoutingContext) -> Unit = { ctx ->
        vertx.launchInDispatcher(ctx) {
            val projectId = request().getParam("projectId")
            logIncomingRequest(this, "GET", projectId)
            vertx.eventBus().sendWithLogging(INC_GET_PROJ, GetProjectRequest(projectId).json, response())
        }
    }

    suspend fun httpGetAllHandler(vertx: Vertx): (RoutingContext) -> Unit = { ctx ->
        vertx.launchInDispatcher(ctx) {
            logIncomingRequest(this, "GET_ALL")
            vertx.eventBus().sendWithLogging(INC_GET_ALL, null, response())
        }
    }

    suspend fun httpDeleteHandler(vertx: Vertx): (RoutingContext) -> Unit = { ctx ->
        vertx.launchInDispatcher(ctx) {
            val projectId = request().getParam("projectId")
            logIncomingRequest(this, "DELETE", projectId)
            vertx.eventBus().sendWithLogging(INC_DEL_PROJ, DeleteProjectRequest(projectId).json, response())
        }
    }

    suspend fun httpDeleteAllHandler(vertx: Vertx): (RoutingContext) -> Unit = { ctx ->
        vertx.launchInDispatcher(ctx) {
            logIncomingRequest(this, "DELETE_ALL")
            vertx.eventBus().sendWithLogging(INC_DEL_ALL, null, response())
        }
    }

    suspend fun httpPutHandler(vertx: Vertx): (RoutingContext) -> Unit = { ctx ->
        vertx.launchInDispatcher(ctx) {
            logIncomingRequest(this, "PUT", bodyAsJson)
            vertx.eventBus().sendWithLogging(INC_PUT_PROJ, bodyAsJson.json, response())
        }
    }

    suspend fun httpPostHandler(vertx: Vertx): (RoutingContext) -> Unit = { ctx ->
        vertx.launchInDispatcher(ctx) {
            logIncomingRequest(this, "POST", bodyAsJson)
            vertx.eventBus().sendWithLogging(INC_POST_PROJ, bodyAsJson.json, response())
        }
    }

    private fun logIncomingRequest(ctx: RoutingContext, type: String, contextInfo: Any? = "-") {
        LOG.debug("Got request from ${ctx.request().remoteAddress()}, $type with $contextInfo")
    }

    private fun Vertx.launchInDispatcher(ctx: RoutingContext, block: suspend RoutingContext.() -> Unit) {
        launch(dispatcher()) {
            ctx.block()
        }
    }

    private suspend fun EventBus.sendWithLogging(action: String, request: Any?, response: HttpServerResponse) {
        LOG.debug("Send $action with $request via EventBus")
        try {
            val reply = awaitResult<Message<Any>> { send(action, request, it) }
            LOG.debug("Got reply: {}", reply)
            handleReply(action, response, reply)
        } catch (e: ReplyException) {
            LOG.error("$action: negative reply received")
            handleReplyException(action, response, e)
        }

    }

    private fun handleReply(action: String, response: HttpServerResponse, reply: Message<Any>) {
        LOG.debug("$action: handle reply")
        reply.body().toString().let {
            LOG.debug("$action: positive reply received: $it")
            response.setStatusCode(HttpURLConnection.HTTP_OK).end(it)
            LOG.debug("Forwarded $it as response with 200")
        }
    }


    private fun handleReplyException(action: String, response: HttpServerResponse, e: ReplyException) {
        val errMsgInternalError = when (e.failureType()) {
            ReplyFailure.RECIPIENT_FAILURE -> {
                val jsonReply = e.message
                response.setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST).end(jsonReply)
                LOG.debug("Ended Request with: $jsonReply")
                return
            }
            ReplyFailure.TIMEOUT -> "Timeout bei Warten auf Antwort zu $action"
            ReplyFailure.NO_HANDLERS -> "Kein Handler für $action"
            else -> "Unbekannter Fehler"
        }
        ErrorResponse(errMsgInternalError, ErrorResponse.ApplicationErrorCodes.UNSPECIFIC.code).let {
            response.setStatusCode(HttpURLConnection.HTTP_INTERNAL_ERROR).end(it.json.encodePrettily())
            LOG.error("Internal Error: Ended Request with: $it")
        }

    }
}