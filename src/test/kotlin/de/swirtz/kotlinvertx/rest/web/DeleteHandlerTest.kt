package de.swirtz.kotlinvertx.rest.web

import de.swirtz.kotlinvertx.rest.TestUtils.await
import de.swirtz.kotlinvertx.rest.data.ErrorResponse
import de.swirtz.kotlinvertx.rest.data.Project
import de.swirtz.kotlinvertx.rest.REST_SRV
import io.vertx.ext.unit.TestContext
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.launch
import org.junit.Test

class DeleteHandlerTest : WebVerticleTest() {

    @Test
    fun deleteProjectNotAvailable(context: TestContext) = await { testLatch ->
        val client = vertx.createHttpClient()
        val handler: ClientRespHandler = { response ->
            LOG.debug("Received response with status code ${response.statusCode()}")
            context.assertEquals(400, response.statusCode())
            response.bodyHandler { body ->
                val resp = body.getString(0, body.length())
                LOG.debug("Response: $resp")
                context.assertEquals(ErrorResponse.ApplicationErrorCodes.PROJECT_UNKNOWN.code,
                        body.toJsonObject().getInteger("error"))
                assertJsonHeader(response, context)
                testLatch.countDown()
            }
        }
        client.delete(webVertPort, "localhost", "${REST_SRV}/1", handler).withJsonAndExceptionHandling(context).end()

    }

    @Test
    fun deleteProjectIsAvailable(context: TestContext) = await { testLatch ->
        saveAndWait(context, Project("1", "myProj"))
        saveAndWait(context, Project("2", "myProj"))
        val client = vertx.createHttpClient()
        val handler: ClientRespHandler = { response ->
            LOG.debug("Received response with status code ${response.statusCode()}")
            context.assertEquals(200, response.statusCode())
            response.bodyHandler { body ->
                val toJsonObject = body.toJsonObject()
                LOG.debug("Response: $toJsonObject")
                val jsonObject = toJsonObject.getBoolean("success")
                context.assertNotNull(jsonObject)
                assertJsonHeader(response, context)
                launch(vertx.dispatcher()) {
                    context.assertEquals(1, inMemRepo.getAll().size)
                    testLatch.countDown()
                }
            }
        }
        client.delete(webVertPort, "localhost", "${REST_SRV}/1", handler).withJsonAndExceptionHandling(context).end()

    }

    @Test(timeout = 10_000)
    fun deleteProjectNoRepoServiceAvailable(context: TestContext) = await { testLatch ->
        undeployProjectsVerticle(context)

        val client = vertx.createHttpClient()
        val handler: ClientRespHandler = { response ->
            LOG.debug("Received response with status code ${response.statusCode()}")
            context.assertEquals(500, response.statusCode())
            response.bodyHandler { body ->
                val resp = body.getString(0, body.length())
                LOG.debug("Response: $resp")
                context.assertEquals(ErrorResponse.ApplicationErrorCodes.UNSPECIFIC.code,
                        body.toJsonObject().getInteger("error"))
                LOG.debug("asserts done")
                testLatch.countDown()
            }
        }
        client.delete(webVertPort, "localhost", "${REST_SRV}/1", handler).withJsonAndExceptionHandling(context).end()

    }

}