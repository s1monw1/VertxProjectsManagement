package de.swirtz.kotlinvertx.rest

import de.swirtz.kotlinvertx.rest.TestUtils.await
import de.swirtz.kotlinvertx.rest.repo.inmemory.InMemoryRepository
import io.vertx.core.Vertx
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.RunTestOnContext
import io.vertx.ext.unit.junit.Timeout
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.launch
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith


@RunWith(VertxUnitRunner::class)
abstract class VertxAwareTest {
    val webVertPort = 8939

    @get:Rule
    var rule = RunTestOnContext()

    @get:Rule
    var timeout: Timeout = Timeout.seconds(5)

    lateinit var repoVertDeploy: String

    val inMemRepo = InMemoryRepository

    companion object {
        lateinit var vertx: Vertx
        @AfterClass
        fun tearDown(context: TestContext) = vertx.close(context.asyncAssertSuccess())
    }

    @Before
    fun initVertx() {
        vertx = rule.vertx()
    }

    @After
    open fun clearRepo(context: TestContext) = await { latch ->
        launch(vertx.dispatcher()) {
            inMemRepo.clear()
            latch.countDown()
        }
    }

    fun undeployProjectsVerticle(context: TestContext) = await { latch ->
        vertx.undeploy(repoVertDeploy) {
            latch.countDown()
        }
    }


}