package de.swirtz.kotlinvertx.rest.web

import de.swirtz.kotlinvertx.rest.TestUtils.await
import de.swirtz.kotlinvertx.rest.data.ErrorResponse
import de.swirtz.kotlinvertx.rest.data.Project
import de.swirtz.kotlinvertx.rest.REST_SRV
import io.vertx.ext.unit.TestContext
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.launch
import org.junit.Test

/**
 * @author: SIM
 *
 * File created on 09.06.2017.
 */
class GetAllHandlerTest : WebVerticleTest() {

    @Test
    fun getAllProjectsNoneAvailable(context: TestContext) = await { testLatch ->
        val client = vertx.createHttpClient()
        val handler: ClientRespHandler = { response ->
            LOG.debug("Received response with status code ${response.statusCode()}")
            context.assertEquals(200, response.statusCode())
            response.bodyHandler { body ->
                val projectsJson = body.toJsonArray()
                LOG.debug("Response: $projectsJson")
                context.assertEquals(0, projectsJson.size())
                assertJsonHeader(response, context)
                testLatch.countDown()
            }
        }
        client.get(webVertPort, "localhost", REST_SRV, handler).withJsonAndExceptionHandling(context).end()

    }

    @Test
    fun getAllProjectsOneAvailable(context: TestContext) = await { testLatch ->
        saveAndWait(context, Project("1", "myProj"))
        val client = vertx.createHttpClient()
        val handler: ClientRespHandler = { response ->
            LOG.debug("Received response with status code ${response.statusCode()}")
            context.assertEquals(200, response.statusCode())
            response.bodyHandler { body ->
                val projectsJson = body.toJsonArray()
                LOG.debug("Response: $projectsJson")
                context.assertEquals(1, projectsJson.size())
                context.assertEquals("myProj", projectsJson.getJsonObject(0).getString("name"))
                assertJsonHeader(response, context)
                testLatch.countDown()
            }
        }
        client.get(webVertPort, "localhost", REST_SRV, handler).withJsonAndExceptionHandling(context).end()
    }

    @Test
    fun getAllProjectsMultiAvailable(context: TestContext) = await { testLatch ->
        await(10) { latch ->
            (1..10).forEach {
                launch(vertx.dispatcher()) {
                    inMemRepo.save(Project("$it", "myProj$it"))
                    latch.countDown()
                }
            }
        }

        val client = vertx.createHttpClient()
        val handler: ClientRespHandler = { response ->
            LOG.debug("Received response with status code ${response.statusCode()}")
            context.assertEquals(200, response.statusCode())
            response.bodyHandler { body ->
                val projectsJson = body.toJsonArray()
                LOG.debug("Response: $projectsJson")
                context.assertEquals(10, projectsJson.size())
                assertJsonHeader(response, context)
                testLatch.countDown()
            }
        }
        client.get(webVertPort, "localhost", REST_SRV, handler).withJsonAndExceptionHandling(context).end()
    }

    @Test(timeout = 15_000)
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
        client.get(webVertPort, "localhost", REST_SRV, handler).withJsonAndExceptionHandling(context).end()

    }

}