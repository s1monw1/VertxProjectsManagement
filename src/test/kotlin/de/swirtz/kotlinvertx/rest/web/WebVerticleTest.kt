package de.swirtz.kotlinvertx.rest.web

import de.swirtz.kotlinvertx.rest.TestUtils.await
import de.swirtz.kotlinvertx.rest.VertxAwareTest
import de.swirtz.kotlinvertx.rest.JSON_CONT_TYPE
import de.swirtz.kotlinvertx.rest.data.Project
import de.swirtz.kotlinvertx.rest.repo.service.ProjectsServiceVerticle
import io.vertx.core.http.HttpClientRequest
import io.vertx.core.http.HttpClientResponse
import io.vertx.ext.unit.TestContext
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.launch
import org.junit.Before
import org.slf4j.LoggerFactory

typealias ClientRespHandler = (HttpClientResponse) -> Unit
/**
 * @author: SIM
 *
 * File created on 09.06.2017.
 */
abstract class WebVerticleTest : VertxAwareTest() {

    fun HttpClientRequest.withJsonAndExceptionHandling(context: TestContext): HttpClientRequest
            = putHeader("Accept", "application/json").exceptionHandler { err -> context.fail(err.message) }

    val LOG = LoggerFactory.getLogger(WebVerticleTest::class.java)

    @Before
    fun setup(context: TestContext) = await { latch ->
        vertx.deployVerticle(WebVerticle(webVertPort), context.asyncAssertSuccess {
            LOG.debug("WebVerticle accessible")
            vertx.deployVerticle(ProjectsServiceVerticle(inMemRepo), context.asyncAssertSuccess { deployment ->
                repoVertDeploy = deployment
                LOG.debug("ProjectsServiceVerticle accessible: $deployment")
            })
            latch.countDown()
        })
    }

    fun assertJsonHeader(response: HttpClientResponse, context: TestContext) {
        val headers = response.headers()
        headers.forEach { LOG.info("found header $it") }
        context.assertEquals(JSON_CONT_TYPE, headers.filter { it.key == "Content-Type" }.map { it.value }[0])
    }

    fun saveAndWait(context: TestContext, project: Project) = await { latch ->
        launch(vertx.dispatcher()) {
            inMemRepo.save(project)
            latch.countDown()
        }
    }

}