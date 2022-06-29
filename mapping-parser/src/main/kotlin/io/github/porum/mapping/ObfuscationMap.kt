package io.github.porum.mapping

class ObfuscationMap<V : MappedNode>(
    private val delegate: Map<Pair<String, String>, V> = HashMap()
) : Map<Pair<String, String>, V> by delegate {
    private val realNames = delegate.mapKeys { it.key.first }
    private val fakeNames = delegate.mapKeys { it.key.second }

    fun getByReal(name: String): V? = realNames[name]
    fun getByFake(name: String): V? = fakeNames[name]
}