package de.swirtz.kotlinvertx.rest.web

import de.swirtz.kotlinvertx.rest.TestUtils.await
import de.swirtz.kotlinvertx.rest.data.ErrorResponse
import de.swirtz.kotlinvertx.rest.data.Project
import de.swirtz.kotlinvertx.rest.REST_SRV
import io.vertx.ext.unit.TestContext
import org.junit.Test

/**
 * @author: SIM
 *
 * File created on 09.06.2017.
 */
class GetHandlerTest : WebVerticleTest() {

    @Test
    fun getProjectNotAvailable(context: TestContext)= await { testLatch ->
        val client = vertx.createHttpClient()
        val handler: ClientRespHandler = { response ->
            LOG.debug("Received response with status code ${response.statusCode()}")
            context.assertEquals(400, response.statusCode())
            response.bodyHandler { body ->
                val resp = body.getString(0, body.length())
                LOG.debug("Response: $resp")
                context.assertEquals(ErrorResponse.ApplicationErrorCodes.PROJECT_UNKNOWN.code,
                        body.toJsonObject().getInteger("error"))
                response.headers().forEach { LOG.info("found header $it") }
                assertJsonHeader(response, context)
                testLatch.countDown()
            }
        }
        client.get(webVertPort, "localhost", "${REST_SRV}/1", handler).withJsonAndExceptionHandling(context).end()

    }

    @Test
    fun getProjectIsAvailable(context: TestContext)= await { testLatch ->
        saveAndWait(context, Project("1", "myProj"))
        val client = vertx.createHttpClient()
        val handler: ClientRespHandler = { response ->
            LOG.debug("Received response with status code ${response.statusCode()}")
            context.assertEquals(200, response.statusCode())
            response.bodyHandler { body ->
                val jsonObject = body.toJsonObject()
                LOG.debug("Response: $jsonObject")
                context.assertNotNull(jsonObject)
                context.assertEquals(null, jsonObject.getString("error"))
                context.assertEquals("myProj", jsonObject.getString("name"))

                assertJsonHeader(response, context)
                testLatch.countDown()
            }
        }
        client.get(webVertPort, "localhost", "${REST_SRV}/1", handler).withJsonAndExceptionHandling(context).end()

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
        client.get(webVertPort, "localhost", "${REST_SRV}/1", handler).withJsonAndExceptionHandling(context).end()

    }

}