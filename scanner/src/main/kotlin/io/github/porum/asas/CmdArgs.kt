package io.github.porum.asas

import com.beust.jcommander.Parameter

class CmdArgs {

    @Parameter(description = "<input files> (.apk)")
    lateinit var inputFile: String

    @Parameter(names = ["-mapping"], description = "mapping file")
    lateinit var mappingFile: String

    @Parameter(names = ["-json"], description = "sensitive api config")
    lateinit var sensitiveApiConfig: String

    @Parameter(names = ["-d", "--output-dir"], description = "output directory")
    lateinit var outputDir: String

    fun processArgs(vararg args: String): Boolean {
        val jCommanderWrapper = JCommanderWrapper(this)
        return jCommanderWrapper.parse(*args)
    }
}