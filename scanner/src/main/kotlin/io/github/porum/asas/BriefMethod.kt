package io.github.porum.asas

data class BriefMethod(
    val owner: String,
    val descriptor: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BriefMethod

        if (owner != other.owner) return false
        if (descriptor != other.descriptor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = owner.hashCode()
        result = 31 * result + descriptor.hashCode()
        return result
    }
}