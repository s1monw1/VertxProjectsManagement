package de.swirtz.kotlinvertx.rest.web

import de.swirtz.kotlinvertx.rest.TestUtils.await
import de.swirtz.kotlinvertx.rest.data.ErrorResponse
import de.swirtz.kotlinvertx.rest.data.Project
import de.swirtz.kotlinvertx.rest.REST_SRV
import io.vertx.ext.unit.TestContext
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.launch
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.fail

/**
 * @author: SIM
 *
 * File created on 09.06.2017.
 */
class DeleteAllHandlerTest : WebVerticleTest() {

    @Test
    fun deleteAllProjectsNoneAvailable(context: TestContext) = await { testLatch ->
        val client = vertx.createHttpClient()
        client.delete(webVertPort, "localhost", REST_SRV, { response ->
            LOG.debug("Received response with status code ${response.statusCode()}")
            context.assertEquals(200, response.statusCode())
            response.bodyHandler { body ->
                val toJsonObject = body.toJsonObject()
                LOG.debug("Response: $toJsonObject")
                val jsonObject = toJsonObject.getBoolean("success")
                context.assertTrue(jsonObject)
                assertJsonHeader(response, context)
                launch(vertx.dispatcher()) {
                    context.assertEquals(0, inMemRepo.getAll().size)
                    testLatch.countDown()
                }
            }
        }).putHeader("Accept", "application/json").exceptionHandler { err -> context.fail(err.message) }.end()

    }

    @Test
    fun deleteAllProjectsAvailable(context: TestContext) = await { testLatch ->
        val latch = CountDownLatch(10)
        (1..10).forEach {
            launch(vertx.dispatcher()) {
                inMemRepo.save(Project("$it", "myProj$it"))
                latch.countDown()
            }
        }
        if (!latch.await(2, TimeUnit.SECONDS)) fail()
        val client = vertx.createHttpClient()
        val handler: ClientRespHandler = { response ->
            LOG.debug("Received response with status code ${response.statusCode()}")
            context.assertEquals(200, response.statusCode())
            response.bodyHandler { body ->
                val toJsonObject = body.toJsonObject()
                LOG.debug("Response: $toJsonObject")
                val jsonObject = toJsonObject.getBoolean("success")
                context.assertTrue(jsonObject)
                assertJsonHeader(response, context)
                launch(vertx.dispatcher()) {
                    context.assertEquals(0, inMemRepo.getAll().size)
                    testLatch.countDown()
                }
            }
        }
        client.delete(webVertPort, "localhost", REST_SRV, handler).withJsonAndExceptionHandling(context).end()

    }

    @Test(timeout = 10_000)
    fun deleteAllProjectsNoneAvailableNoRepoServiceAvailable(context: TestContext) = await { testLatch ->
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
        client.delete(webVertPort, "localhost", "${REST_SRV}/all", handler).withJsonAndExceptionHandling(context).end()

    }

}