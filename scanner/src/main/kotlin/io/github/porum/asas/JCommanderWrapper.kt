package io.github.porum.asas

import com.beust.jcommander.JCommander
import com.beust.jcommander.ParameterException

class JCommanderWrapper<T>(private val obj: T) {

    private val jCommander = JCommander.newBuilder().addObject(obj).build()

    fun parse(vararg args: String): Boolean {
        return try {
            jCommander.parse(*args)
            true
        } catch (e: ParameterException) {
            System.err.println("Arguments parse error: ${e.message}")
            false
        }
    }
}