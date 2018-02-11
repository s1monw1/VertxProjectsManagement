package de.swirtz.kotlinvertx.rest.data

/**
 * This file contains data classes representing json objects
 */

data class ErrorResponse(val message: String, val error: Int, val context: String? = null) {
    enum class ApplicationErrorCodes(val code: Int, val description: String) {
        PROJECT_UNKNOWN(1000, "Project cannot be found in repo."),
        PROJECT_EXISTS(1001, "Project already exists in repo."),
        TIMEOUT(8000, "Request Timed out internally"),
        UNSPECIFIC(9000, "Unspecific");
    }
}

data class GetProjectRequest(val projectId: String)
data class DeleteProjectRequest(val projectId: String)
data class Result(val success: Boolean) {
    companion object {
        fun success() = Result(true)
        fun fail() = Result(false)
    }
}


