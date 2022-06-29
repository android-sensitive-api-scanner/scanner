package io.github.porum.mapping

import io.github.porum.mapping.parsers.ProGuardMappingParser

object Parsers : ServiceMapCollector<String, MappingParser>({ it.name }) {
    init {
        add(ProGuardMappingParser)
    }

    val PRO_GUARD: String = ProGuardMappingParser.name
}