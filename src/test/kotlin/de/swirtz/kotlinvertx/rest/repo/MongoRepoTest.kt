package de.swirtz.kotlinvertx.rest.repo

import de.bwaldvogel.mongo.MongoServer
import de.bwaldvogel.mongo.backend.h2.H2Backend
import de.swirtz.kotlinvertx.rest.TestUtils.assertEntrySize
import de.swirtz.kotlinvertx.rest.TestUtils.await
import de.swirtz.kotlinvertx.rest.VertxAwareTest
import de.swirtz.kotlinvertx.rest.data.Project
import de.swirtz.kotlinvertx.rest.repo.mongo.MongoClientConfig
import de.swirtz.kotlinvertx.rest.repo.mongo.MongoRepository
import io.vertx.ext.unit.TestContext
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.launch
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.fail

@Ignore("Problems on MacOS")
class MongoRepoTest : VertxAwareTest() {

    private lateinit var repo: MongoRepository
    lateinit var server: MongoServer
    private val path: Path = Paths.get("src/test/resources/db.mv")

    @After
    override fun clearRepo(context: TestContext) = await { latch ->
        launch(vertx.dispatcher()) {
            repo.clear()
            server.shutdownNow()
            Files.deleteIfExists(path)
            latch.countDown()
        }
    }

    @Before
    fun setup(context: TestContext) {
        Files.deleteIfExists(path)
        Files.createFile(path)
        server = MongoServer(H2Backend(path.toString()))
        val port = 27019
        server.bind("localhost", port)

        val mongoConf = MongoClientConfig("projects_db", "localhost", port)
        repo = MongoRepository(mongoConf, vertx)
    }


    @Test
    fun getTest(context: TestContext) = await { latch ->
        val _id = "myid"
        launch(vertx.dispatcher()) {
            val save = repo.save(Project(_id, "name"))
            assertNotNull(save)
            assertEntrySize(repo, 1)
            assertEquals(1, repo.getAll().size)
            val proj = repo.get(_id)
            assertEquals("name", proj?.name)
            latch.countDown()
        }
    }

    @Test
    fun getUnknownTest(context: TestContext) = await { latch ->
        launch(vertx.dispatcher()) {
            assertNull(repo.get("myid"))
            latch.countDown()
        }
    }

    @Test
    fun saveTest(context: TestContext) = await { latch ->
        launch(vertx.dispatcher()) {
            val save = repo.save(Project("myid", "name"))
            assertNotNull(save)
            assertEntrySize(repo, 1)
            latch.countDown()
        }
    }

    @Test
    fun saveTwiceTest(context: TestContext) = await { latch ->
        val project = Project("myid", "name")
        launch(vertx.dispatcher()) {
            repo.save(project)
            assertEntrySize(repo, 1)
            try {
                repo.save(project.copy(name = "different"))
                fail()
            } catch (e: Exception) {
                assertEquals(IllegalArgumentException::class, e::class)
            }
            assertEntrySize(repo, 1)
            latch.countDown()

        }
    }

    @Test
    fun updateTest(context: TestContext) = await { latch ->
        val project = Project("myid", "name")
        launch(vertx.dispatcher()) {
            repo.save(project)
            assertEntrySize(repo, 1)
            val update = repo.update(project.copy(name = "changed"))
            assertNotNull(update)
            assertEntrySize(repo, 1)
            latch.countDown()
        }
    }

    @Test
    fun deleteTest(context: TestContext) = await { latch ->
        val project = Project("myid", "name")
        launch(vertx.dispatcher()) {
            repo.save(project)
            assertEntrySize(repo, 1)
            repo.delete(project)
            assertEquals(0, repo.getAll().size)
            latch.countDown()
        }
    }

    @Test
    fun deleteByIDTest(context: TestContext) = await { latch ->
        val _id = "myid"
        launch(vertx.dispatcher()) {
            repo.save(Project(_id, "name"))
            assertEntrySize(repo, 1)
            repo.delete(_id)
            assertEquals(0, repo.getAll().size)
            latch.countDown()
        }
    }

    @Test
    fun deleteUnknownTest(context: TestContext) = await { latch ->
        launch(vertx.dispatcher()) {
            try {
                repo.delete("myid")
                fail()
            } catch (e: Exception) {
                assertEquals(IllegalArgumentException::class, e::class)
            }
            latch.countDown()
        }
    }

}