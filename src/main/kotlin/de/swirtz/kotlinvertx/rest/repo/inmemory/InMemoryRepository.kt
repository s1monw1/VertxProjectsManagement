package de.swirtz.kotlinvertx.rest.repo.inmemory

import de.swirtz.kotlinvertx.rest.data.Project
import de.swirtz.kotlinvertx.rest.repo.ProjectsRepository
import org.slf4j.LoggerFactory

/**
 *  This implementation of [ProjectsRepository] uses an internal map and won't keep the entities after the JVM
 *  terminates.
 */
object InMemoryRepository : ProjectsRepository {
    private val projects: MutableSet<Project> = mutableSetOf()
    private val LOG = LoggerFactory.getLogger(InMemoryRepository::class.java)

    override suspend fun get(projectId: String): Project? {
        LOG.info("get called with $projectId")
        return find(projectId)
    }

    override suspend fun getAll(): Set<Project> {
        LOG.info("getAll called. Current size of internal set: ${projects.size}")
        return projects.toSet()
    }

    override suspend fun save(project: Project): Project {
        LOG.info("save called. $project")
        find(project._id)?.let {
            with("Project with ID ${project._id} already exists!") {
                LOG.debug(this)
                throw IllegalArgumentException(this)
            }
        }
        return if (projects.add(project)) project
        else throw IllegalStateException("Element $project cannot be added")
    }

    override suspend fun update(project: Project): Project {
        LOG.info("update called. $project")
        if (!deleteIfExists(project._id)) {
            with("Project with ID ${project._id} cannot be updated because it does not " +
                    "exist.") {
                LOG.debug(this)
                throw IllegalArgumentException(this)
            }
        }
        return if (projects.add(project)) project
        else throw IllegalStateException("Element $project cannot be updated")
    }

    override suspend fun delete(projectId: String) {
        if (!deleteIfExists(projectId))
            throw IllegalArgumentException("Project with projectId=$projectId cannot be found.")
    }

    override suspend fun delete(project: Project) = delete(project._id)

    override suspend fun clear() {
        LOG.info("clearAll called. Size of internal set before clear: ${projects.size}")
        projects.clear()
    }

    private fun deleteIfExists(projectId: String): Boolean =
            find(projectId)?.let { LOG.info("delete $it"); projects.remove(it) } == true

    private fun find(projectId: String) = projects.asSequence().filter { it._id == projectId }.firstOrNull()

}