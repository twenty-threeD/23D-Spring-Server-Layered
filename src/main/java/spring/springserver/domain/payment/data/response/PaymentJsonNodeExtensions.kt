package spring.springserver.domain.payment.data.response

import com.fasterxml.jackson.databind.JsonNode

internal fun JsonNode.textOrNull(fieldName: String): String? {

    val node = path(fieldName)

    return if (node.isMissingNode || node.isNull) null else node.asText()
}

internal fun JsonNode.longOrNull(fieldName: String): Long? {

    val node = path(fieldName)

    return if (node.isMissingNode || node.isNull) null else node.asLong()
}

internal fun JsonNode.intOrNull(fieldName: String): Int? {

    val node = path(fieldName)

    return if (node.isMissingNode || node.isNull) null else node.asInt()
}

internal fun JsonNode.booleanOrNull(fieldName: String): Boolean? {

    val node = path(fieldName)

    return if (node.isMissingNode || node.isNull) null else node.asBoolean()
}

internal fun <T> JsonNode.objectOrNull(fieldName: String, mapper: (JsonNode) -> T): T? {

    val node = path(fieldName)

    return if (node.isMissingNode || node.isNull) null else mapper(node)
}

internal fun <T> JsonNode.listOrNull(fieldName: String, mapper: (JsonNode) -> T): List<T>? {

    val node = path(fieldName)

    return if (node.isMissingNode || node.isNull || !node.isArray) {

        null
    } else {

        node.map(mapper)
    }
}
