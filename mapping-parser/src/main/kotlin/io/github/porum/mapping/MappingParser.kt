package io.github.porum.mapping

import java.io.InputStream
import java.net.URI
import java.net.URL

interface MappingParser {
    val name: String

    fun parse(mappingsIn: InputStream) : MappedArchive

    fun parse(mappings: URL) : MappedArchive = parse(mappings.openStream())

    fun parse(mappings: URI): MappedArchive = parse(mappings.toURL())
}