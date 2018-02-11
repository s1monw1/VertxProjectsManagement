package de.swirtz.kotlinvertx.rest.repo

import de.swirtz.kotlinvertx.rest.data.Project


/**
 * The interface for a [Project] repository. Contains simple CRUD operations, methods may suspend.
 *
 */
interface ProjectsRepository {

    /**
     * Looks for [Project] with [projectId] in this repository.
     * @return the [Project], might be ``null``
     */
    suspend fun get(projectId: String): Project?

    /**
     * Looks for *all* [Project] in this repository.
     * @return all [Project]s
     */
    suspend fun getAll(): Set<Project>

    /**
     * Saves a new [project] to this repository. Existing Projects must be updated and cannot be overridden with this
     * method. If an attempt of overriding is requested, an exception will be thrown.
     * @return the new/updated [project]
     */
    suspend fun save(project: Project): Project


    /**
     * Updates an existing [project] in this repository. Project must exist, otherwise an exception will be thrown.
     * @return the updated [project]
     */
    suspend fun update(project: Project): Project

    /**
     * Deletes [project] if it can be found in this repository. Might throw exception if it cannot be found.
     */
    suspend fun delete(project: Project)

    /**
     * Deletes [Project] with corresponding [projectId] if it can be found in this repository. Might throw exception
     * if it cannot be found.
     */
    suspend fun delete(projectId: String)

    /**
     * Deletes ALL [Project]s found in this repository.
     */
    suspend fun clear()
}