package de.swirtz.kotlinvertx.rest.data

data class Project(
        /** values are public for serialization, [_id] is expected by MongoDB* */
        val _id: String,
        val name: String
)

