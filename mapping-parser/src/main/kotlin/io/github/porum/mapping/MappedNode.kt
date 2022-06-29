package io.github.porum.mapping

interface MappedNode {
    val realName: String // The de-obfuscated name
    val fakeName: String // Obfuscated name
}