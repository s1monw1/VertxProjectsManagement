package de.swirtz.kotlinvertx.rest.web

import de.swirtz.kotlinvertx.rest.TestUtils.await
import de.swirtz.kotlinvertx.rest.REST_SRV
import de.swirtz.kotlinvertx.rest.json
import de.swirtz.kotlinvertx.rest.data.ErrorResponse
import de.swirtz.kotlinvertx.rest.data.Project
import io.vertx.ext.unit.TestContext
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.launch
import org.junit.Test

/**
 * @author: SIM
 *
 * File created on 09.06.2017.
 */
class PutHandlerTest : WebVerticleTest() {

    @Test
    fun updateExistingProject(context: TestContext) = await { testLatch ->
        val client = vertx.createHttpClient()
        val project = Project("id", "projectX")
        saveAndWait(context, project)
        val projectUpd = project.copy(name = "newName").json
        val handler: ClientRespHandler = { response ->
            LOG.debug("Received response with status code ${response.statusCode()}")
            context.assertEquals(200, response.statusCode())
            response.bodyHandler { body ->
                val resp = body.getString(0, body.length())
                LOG.debug("Response: $resp")
                context.assertEquals(projectUpd, body.toJsonObject())
                assertJsonHeader(response, context)
                launch(vertx.dispatcher()) {
                    val all = inMemRepo.getAll()
                    context.assertEquals(1, all.size)
                    val result = inMemRepo.get("id")
                    context.assertEquals("newName", result?.name)
                    testLatch.countDown()
                }
            }
        }
        client.put(webVertPort, "localhost", REST_SRV, handler).withJsonAndExceptionHandling(context).end(projectUpd
                .json.encodePrettily())
    }

    @Test
    fun updateNonExistingProject(context: TestContext) = await { testLatch ->
        val client = vertx.createHttpClient()
        val projectJson = Project("id", "projectX").json
        val jsonBody = projectJson.json.encodePrettily()
        LOG.debug("Json Request: $jsonBody")
        val handler: ClientRespHandler = { response ->
            LOG.debug("Received response with status code ${response.statusCode()}")
            context.assertEquals(400, response.statusCode())
            response.bodyHandler { body ->
                val resp = body.getString(0, body.length())
                LOG.debug("Response: $resp")
                context.assertEquals(ErrorResponse.ApplicationErrorCodes.PROJECT_UNKNOWN.code,
                        body.toJsonObject().getInteger("error"))
                LOG.debug("asserts done")
                testLatch.countDown()

            }
        }
        client.put(webVertPort, "localhost", REST_SRV, handler).withJsonAndExceptionHandling(context).end(jsonBody)
    }

    @Test(timeout = 10_000)
    fun getProjectNoRepoServiceAvailable(context: TestContext) = await { testLatch ->
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
        client.put(webVertPort, "localhost", REST_SRV, handler)
                .withJsonAndExceptionHandling(context).end(Project("id", "name").json.encodePrettily())

    }

}