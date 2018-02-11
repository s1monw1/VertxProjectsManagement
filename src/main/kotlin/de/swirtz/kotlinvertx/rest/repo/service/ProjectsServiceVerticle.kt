package de.swirtz.kotlinvertx.rest.repo.service

import de.swirtz.kotlinvertx.rest.*
import de.swirtz.kotlinvertx.rest.data.*
import de.swirtz.kotlinvertx.rest.repo.ProjectsRepository
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.launch
import org.slf4j.LoggerFactory

/**
 *
 * Verticle acting as a service abstraction for the repository of Projects
 */
class ProjectsServiceVerticle(val repo: ProjectsRepository) : CoroutineVerticle() {

    companion object {
        private val LOG = LoggerFactory.getLogger(ProjectsServiceVerticle::class.java)
    }

    /**
     * Starts [ProjectsServiceVerticle] by registering [Handler]s for every event topic we want to accept.
     * These events are normally sent by the [WebVerticle].
     */
    override suspend fun start() {
        LOG.debug("ProjectsServiceVerticle start called.")

        val consumers = mapOf(
            INC_GET_PROJ to getConsumer(),
            INC_GET_ALL to getAllConsumer(),
            INC_PUT_PROJ to putConsumer(),
            INC_POST_PROJ to postConsumer(),
            INC_DEL_PROJ to deleteConsumer(),
            INC_DEL_ALL to deleteAllConsumer()
        )
        consumers.forEach { c ->
            vertx.eventBus().consumer<JsonObject>(c.key, c.value).completionHandler {
                LOG.debug("Consumer for '${c.key}' registered: ${c.value}")
            }
        }
        LOG.debug("Json-Message-Handlers registered and Verticle started.")
    }

    private fun deleteAllConsumer() = { msg: Message<JsonObject> ->
        vertx.launchInDispatcher(INC_DEL_PROJ, ErrorResponse.ApplicationErrorCodes.PROJECT_UNKNOWN, msg) {
            repo.clear()
            Result.success().json.let {
                LOG.debug("Reply Message ProjectsServiceVerticle $it")
                reply(it)
            }

        }
    }

    private fun deleteConsumer() = { msg: Message<JsonObject> ->
        vertx.launchInDispatcher(INC_DEL_PROJ, ErrorResponse.ApplicationErrorCodes.PROJECT_UNKNOWN, msg) {
            val serviceReq = typedBody<DeleteProjectRequest>()
            repo.delete(serviceReq.projectId)
            Result.success().json.let {
                LOG.info("repo access has finished: $it")
                LOG.debug("Reply Message ProjectsServiceVerticle $it")
                reply(it)
            }

        }
    }

    private fun postConsumer() = { msg: Message<JsonObject> ->
        vertx.launchInDispatcher(INC_POST_PROJ, ErrorResponse.ApplicationErrorCodes.PROJECT_EXISTS, msg) {
            val result = repo.save(typedBody())
            LOG.info("repo access has finished: $result")
            result.json.let {
                LOG.debug("Reply Message ProjectsServiceVerticle $it")
                reply(it)
            }

        }
    }


    private fun putConsumer() = { msg: Message<JsonObject> ->
        vertx.launchInDispatcher(INC_PUT_PROJ, ErrorResponse.ApplicationErrorCodes.PROJECT_UNKNOWN, msg) {
            val serviceReq = typedBody<Project>()
            val update = repo.update(serviceReq)
            LOG.info("repo access has finished: $update")
            update.json.let {
                LOG.debug("Reply Message ProjectsServiceVerticle $it")
                reply(it)
            }

        }
    }

    private fun getConsumer() = { msg: Message<JsonObject> ->
        vertx.launchInDispatcher(INC_GET_PROJ, ErrorResponse.ApplicationErrorCodes.PROJECT_UNKNOWN, msg) {
            val serviceReq = typedBody<GetProjectRequest>()
            repo.get(serviceReq.projectId)?.let {
                LOG.info("repo access has finished: $it")
                LOG.debug("Reply Message $it")
                reply(it.json)
            } ?: errorHandler(
                ErrorResponse.ApplicationErrorCodes.PROJECT_UNKNOWN,
                "No Result available for ${serviceReq.projectId}!"
            )
        }
    }

    private fun getAllConsumer() = { msg: Message<JsonObject> ->
        vertx.launchInDispatcher(INC_GET_ALL, ErrorResponse.ApplicationErrorCodes.UNSPECIFIC, msg) {
            val all = repo.getAll()
            JsonArray(all.toList()).let {
                LOG.debug("Reply Message $it")
                reply(it.encode())
            }
        }
    }

    private fun Vertx.launchInDispatcher(
        action: String,
        errCode: ErrorResponse.ApplicationErrorCodes,
        msg: Message<JsonObject>,
        block: suspend Message<JsonObject>.() -> Unit
    ) {
        launch(dispatcher()) {
            try {
                LOG.debug("Action $action: Message ${msg.body()} received!")
                block(msg)
                LOG.debug("Action $action: Replied to Sender")
            } catch (e: Exception) {
                msg.errorHandler(errCode, "Action $action: failed!")
            }
        }
    }

    private fun Message<JsonObject>.errorHandler(errorCode: ErrorResponse.ApplicationErrorCodes, ctxMessage: String) {
        LOG.error("No Result from repo. $ctxMessage!")
        val json = ErrorResponse(ctxMessage, errorCode.code).json
        LOG.debug("Fail Message ProjectsServiceVerticle $json")
        this.fail(errorCode.code, json.encode())
    }

    private inline fun <reified T> Message<JsonObject>.typedBody() = body().toKotlinObject<T>()

}