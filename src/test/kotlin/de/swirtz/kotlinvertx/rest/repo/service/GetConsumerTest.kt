package de.swirtz.kotlinvertx.rest.repo.service

import de.swirtz.kotlinvertx.rest.INC_GET_PROJ
import de.swirtz.kotlinvertx.rest.TestUtils.await
import de.swirtz.kotlinvertx.rest.data.ErrorResponse
import de.swirtz.kotlinvertx.rest.data.GetProjectRequest
import de.swirtz.kotlinvertx.rest.data.Project
import de.swirtz.kotlinvertx.rest.json
import de.swirtz.kotlinvertx.rest.toKotlinObject
import io.vertx.core.AsyncResult
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.launch
import org.junit.Test

class GetConsumerTest : ConsumerTest() {

    @Test
    fun testGetMessageNoMatch(context: TestContext) = await { testLatch ->
        vertx.eventBus().send(INC_GET_PROJ, GetProjectRequest("1").json) { msg: AsyncResult<Message<Any>> ->
            LOG.debug("Got answer ${msg.result()}")
            context.assertTrue(msg.failed())
            context.assertEquals(null, msg.result())
            context.assertNotNull(msg.cause())
            val errorResp = msg.cause().message?.toKotlinObject<ErrorResponse>()
            context.assertEquals(ErrorResponse.ApplicationErrorCodes.PROJECT_UNKNOWN.code, errorResp?.error)
            testLatch.countDown()
        }
    }

    @Test
    fun testGetMessageWithMatch(context: TestContext) = await { testLatch ->
        launch(vertx.dispatcher()) {
            inMemRepo.save(Project("1", "proj"))
            val result = awaitResult<Message<Any>> {
                vertx.eventBus().send(
                    INC_GET_PROJ, GetProjectRequest("1").json,
                        it)
            }
            val res = result?.body()?.toString()
            LOG.debug("Got answer $res")
            val project = JsonObject(res).toKotlinObject<Project>()
            context.assertEquals("proj", project.name)
            testLatch.countDown()
        }
    }
}