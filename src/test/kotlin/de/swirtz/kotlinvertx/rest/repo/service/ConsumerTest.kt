package de.swirtz.kotlinvertx.rest.repo.service

import de.swirtz.kotlinvertx.rest.VertxAwareTest
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.Before
import org.junit.runner.RunWith
import org.slf4j.LoggerFactory

/**
 *
 * File created on 23.06.2017.
 */


@RunWith(VertxUnitRunner::class)
abstract class ConsumerTest : VertxAwareTest() {

    val LOG = LoggerFactory.getLogger(ConsumerTest::class.java)

    @Before
    fun setup(context: TestContext) {
        vertx = rule.vertx()
        vertx.deployVerticle(ProjectsServiceVerticle(inMemRepo), context.asyncAssertSuccess { deployment ->
            repoVertDeploy = deployment
            LOG.debug("ProjectsServiceVerticle accessible: $deployment")
        })
    }
}