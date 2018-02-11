package de.swirtz.kotlinvertx.rest.repo.mongo

import de.swirtz.kotlinvertx.rest.MONGO_COL_NAME
import de.swirtz.kotlinvertx.rest.data.Project
import de.swirtz.kotlinvertx.rest.json
import de.swirtz.kotlinvertx.rest.repo.ProjectsRepository
import de.swirtz.kotlinvertx.rest.toKotlinObject
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.mongo.MongoClient
import io.vertx.ext.mongo.MongoClientDeleteResult
import io.vertx.kotlin.coroutines.awaitResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class MongoRepository(config: MongoClientConfig, vertx: Vertx) : ProjectsRepository {
    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(MongoRepository::class.java)
        private val emptyDoc = JsonObject()
        private const val idField = "_id"
    }

    private val client = MongoClient.createShared(vertx, config.json)

    private fun String.query() = mapOf(idField to this).json

    override suspend fun get(projectId: String): Project? =
        errorAware("Get document with $projectId from $MONGO_COL_NAME not possible") {
            val reply: JsonObject? = awaitResult<JsonObject> { h ->
                client.findOne(MONGO_COL_NAME, projectId.query(), emptyDoc, h)
            }
            LOG.debug("Got reply: $reply")
            reply?.toKotlinObject<Project>()
        }


    override suspend fun getAll(): Set<Project> = errorAware("Get all documents from $MONGO_COL_NAME not possible") {
        val reply: List<JsonObject>? = awaitResult<List<JsonObject>> { h ->
            client.find(MONGO_COL_NAME, emptyDoc, h)
        }
        reply?.map { it.toKotlinObject<Project>() }?.toSet() ?: setOf()
    }


    override suspend fun save(project: Project): Project = errorAware("Cannot save Project $project!") {
        if (!project.isValid()) {
            throw IllegalArgumentException("Project values not valid")
        }
        val reply: String = awaitResult { h ->
            client.insert(MONGO_COL_NAME, project.json, h)
        }
        LOG.debug("insert result: $reply")
        project
    }

    override suspend fun update(project: Project) = errorAware("Replacing $project not possible") {
        if (!project.isValid()) {
            throw IllegalArgumentException("Project values not valid")
        }

        val reply: JsonObject = awaitResult { h ->
            client.findOneAndReplace(MONGO_COL_NAME, project._id.query(), project.json, h)
        }

        LOG.debug("Replaced $project, Result: $reply")
        reply.toKotlinObject<Project>()
    }

    override suspend fun delete(projectId: String) = errorAware("No document removed from $MONGO_COL_NAME") {
        LOG.debug("clear called")
        val reply: MongoClientDeleteResult = awaitResult { h ->
            client.removeDocument(MONGO_COL_NAME, projectId.query(), h)
        }
        if (reply.removedCount == 0L) {
            LOG.error("No document removed from $MONGO_COL_NAME")
            throw IllegalArgumentException("Project unknown")
        } else {
            LOG.debug("Removed Document $projectId from collection, Result: ${reply.removedCount}")
        }
    }


    override suspend fun delete(project: Project) = delete(project._id)

    override suspend fun clear() = errorAware("Removing all documents from $MONGO_COL_NAME not possible") {
        LOG.debug("clear called")
        val reply: MongoClientDeleteResult? = awaitResult<MongoClientDeleteResult> { h ->
            client.removeDocuments(MONGO_COL_NAME, emptyDoc, h)
        }
        LOG.debug("Removed documents from collection, Result: ${reply?.removedCount}")
    }

    private suspend fun <R> errorAware(msg: String, block: suspend () -> R): R {
        return try {
            block()
        } catch (e: Exception) {
            throw IllegalArgumentException(msg, e)
        }
    }

    private fun Project.isValid() = _id.isNotEmpty() && name.isNotEmpty()
}



