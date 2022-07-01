package io.github.porum.asas

data class Node(
    val owner: String,
    val name: String,
    val descriptor: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Node

        if (owner != other.owner) return false
        if (name != other.name) return false
        if (descriptor != other.descriptor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = owner.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + descriptor.hashCode()
        return result
    }
}