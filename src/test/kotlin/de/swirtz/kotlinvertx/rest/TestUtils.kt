package de.swirtz.kotlinvertx.rest

import de.swirtz.kotlinvertx.rest.repo.ProjectsRepository
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.fail


object TestUtils {

    /**
     * @param num: number of [CountDownLatch::countDown]s to wait for
     * @param block: You need to call [CountDownLatch::countDown] when the block is finished!
     */
    fun await(num: Int = 1, block: (latch: CountDownLatch) -> Unit) {
        CountDownLatch(num).let {
            launch(CommonPool) {
                block(it)
            }
            if (!it.await(4, TimeUnit.SECONDS)) {
                fail()
            }
        }

    }

    fun assertEntrySize(repo: ProjectsRepository, size: Int) = await { latch ->
        launch(VertxAwareTest.vertx.dispatcher()) {
            assertEquals(size, repo.getAll().size)
            latch.countDown()
        }
    }
}
