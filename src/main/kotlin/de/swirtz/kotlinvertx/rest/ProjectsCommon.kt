package de.swirtz.kotlinvertx.rest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vertx.core.json.JsonObject

/**
 * Definiert projektweite Werte.
 */

const val MONGO_COL_NAME = "projects"

const val JSON_CONT_TYPE = "application/json"
const val REST_SRV = "/projects"

const val INC_GET_PROJ = "rest.request.get"
const val INC_PUT_PROJ = "rest.request.put"
const val INC_POST_PROJ = "rest.request.post"
const val INC_GET_ALL = "rest.request.get.all"
const val INC_DEL_PROJ = "rest.request.del"
const val INC_DEL_ALL = "rest.request.del.all"

/**
 * Extension function for JSONObject to enable unmarshalling a json object into a kotlin object
 */
inline fun <reified T> JsonObject.toKotlinObject(): T =
    jacksonObjectMapper().readValue(encode(), T::class.java)

inline fun <reified T> String.toKotlinObject(): T = JsonObject(this).toKotlinObject()

/**
 * Extends all types with a property containing the instance's json representation
 */
val <T> T.json: JsonObject
    get() = JsonObject.mapFrom(this)