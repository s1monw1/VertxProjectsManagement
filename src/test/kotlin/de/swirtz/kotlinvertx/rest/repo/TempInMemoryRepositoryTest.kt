package de.swirtz.kotlinvertx.rest.repo

import de.swirtz.kotlinvertx.rest.TestUtils.assertEntrySize
import de.swirtz.kotlinvertx.rest.TestUtils.await
import de.swirtz.kotlinvertx.rest.VertxAwareTest
import de.swirtz.kotlinvertx.rest.data.Project
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.launch
import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import de.swirtz.kotlinvertx.rest.repo.inmemory.InMemoryRepository as repo

/**
 * @author: SIM
 *
 * File created on 02.06.2017.
 */
class TempInMemoryRepositoryTest : VertxAwareTest() {

    @After
    fun clearRepo() = await { latch ->
        launch(vertx.dispatcher()) {
            repo.clear()
            latch.countDown()
        }
    }


    @Test
    fun saveTest() = await { latch ->
        launch(vertx.dispatcher()) {
            val project = createSimpleEntity()
            repo.save(project)
            assertEntrySize(repo, 1)
            latch.countDown()
        }
    }


    @Test
    fun saveTwiceTest() = await { latch ->
        launch(vertx.dispatcher()) {
            val project = createSimpleEntity()
            repo.save(project)
            try {
                repo.save(project)
                fail()
            } catch (e: Exception) {
                assertEquals(IllegalArgumentException::class.java, e::class.java)
            }
            latch.countDown()
        }
    }


    @Test
    fun updateTest() = await { latch ->
        launch(vertx.dispatcher()) {
            val project = createSimpleEntity()
            repo.save(project)
            val newName = "$createSimpleEntityName-renamed"
            val projRenamed = Project(createSimpleEntityId, newName)
            repo.update(projRenamed)
            assertEquals(1, repo.getAll().size)
            assertEquals(newName, repo.get(createSimpleEntityId)?.name)
            latch.countDown()
        }
    }

    @Test
    fun updateNonExistingTest() = await { latch ->
        launch(vertx.dispatcher()) {
            val projRenamed = Project(createSimpleEntityId, "bla")
            try {
                repo.update(projRenamed)
                fail()
            } catch (e: Exception) {
                assertEquals(IllegalArgumentException::class.java, e::class.java)
            }
            latch.countDown()
        }
    }

    @Test
    fun deleteTest() = await { latch ->
        launch(VertxAwareTest.vertx.dispatcher()) {
            val project = createSimpleEntity()
            repo.save(project)
            assertEntrySize(repo, 1)
            repo.delete(project)
            assertEntrySize(repo, 0)
            latch.countDown()
        }
    }


    @Test
    fun deleteByIdTest() = await { latch ->
        launch(VertxAwareTest.vertx.dispatcher()) {
            val project = createSimpleEntity()
            repo.save(project)
            assertEntrySize(repo, 1)
            repo.delete(createSimpleEntityId)
            assertEntrySize(repo, 0)
            latch.countDown()
        }
    }


    @Test
    fun deleteNotExistingTest() = await { latch ->
        launch(VertxAwareTest.vertx.dispatcher()) {
            try {
                repo.delete("anyId")
                fail()
            } catch (e: Exception) {
                assertEquals(IllegalArgumentException::class.java, e::class.java)
            }
            latch.countDown()
        }
    }

    private val createSimpleEntityId = "001"
    private val createSimpleEntityName = "konnektor-bauen"
    private fun createSimpleEntity(): Project = Project(createSimpleEntityId, createSimpleEntityName)
}