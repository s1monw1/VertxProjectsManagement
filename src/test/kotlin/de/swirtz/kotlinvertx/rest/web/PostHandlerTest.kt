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
class PostHandlerTest : WebVerticleTest() {

    @Test
    fun postNewProject(context: TestContext) = await() { testLatch ->
        val client = vertx.createHttpClient()
        val project = Project("id", "projectX").json
        val jsonBody = project.json.encodePrettily()
        LOG.debug("Json Request: $jsonBody")
        client.post(webVertPort, "localhost", REST_SRV, { response ->
            LOG.debug("Received response with status code ${response.statusCode()}")
            context.assertEquals(200, response.statusCode())
            response.bodyHandler { body ->
                val resp = body.getString(0, body.length())
                LOG.debug("Response: $resp")
                context.assertEquals(project, body.toJsonObject())
                response.headers().forEach { LOG.info("found header $it") }
                assertJsonHeader(response, context)
                launch(vertx.dispatcher()) {
                    context.assertEquals(1, inMemRepo.getAll().size)
                    context.assertEquals("projectX", inMemRepo.get("id")?.name)
                    testLatch.countDown()
                }
            }
        }).withJsonAndExceptionHandling(context).end(jsonBody)
    }

    @Test
    fun postExistingProject(context: TestContext) = await { testLatch ->
        val client = vertx.createHttpClient()
        val project = Project("id", "projectX")
        val projectJson = project.json
        val jsonBody = projectJson.json.encodePrettily()
        saveAndWait(context, project)
        LOG.debug("Json Request: $jsonBody")
        val handler: ClientRespHandler = { response ->
            LOG.debug("Received response with status code ${response.statusCode()}")
            context.assertEquals(400, response.statusCode())
            response.bodyHandler { body ->
                val resp = body.getString(0, body.length())
                LOG.debug("Response: $resp")
                context.assertEquals(ErrorResponse.ApplicationErrorCodes.PROJECT_EXISTS.code,
                        body.toJsonObject().getInteger("error"))
                LOG.debug("asserts done")
                testLatch.countDown()
            }
        }
        client.post(webVertPort, "localhost", REST_SRV, handler).withJsonAndExceptionHandling(context).end(jsonBody)
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