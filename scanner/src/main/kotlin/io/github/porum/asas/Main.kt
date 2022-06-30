package io.github.porum.asas

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.porum.mapping.MappedArchive
import io.github.porum.mapping.Parsers
import jadx.api.JadxArgs
import jadx.api.JadxArgs.RenameEnum
import jadx.api.plugins.JadxPluginManager
import jadx.api.plugins.input.data.ILoadResult
import jadx.core.clsp.ClsSet
import jadx.core.dex.info.MethodInfo
import jadx.core.dex.instructions.InsnType
import jadx.core.dex.instructions.InvokeNode
import jadx.core.dex.instructions.args.ArgType
import jadx.core.dex.instructions.args.PrimitiveType
import jadx.core.dex.nodes.ClassNode
import jadx.core.dex.nodes.InsnNode
import jadx.core.dex.nodes.RootNode
import jadx.core.dex.visitors.SignatureProcessor
import jadx.core.utils.Utils
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.nio.file.Path
import java.util.*
import kotlin.system.exitProcess

/**
 * Created by panda on 2022/6/26 10:30.
 */

private const val TAB = "   "

private var mappings: MappedArchive? = null
private lateinit var sensitives: List<BriefMethod>

private val foundInsnNodeList: MutableList<InsnNode> = ArrayList()
private val callChainList: MutableList<BriefMethod> = ArrayList()

fun main(vararg args: String) {
    var result = 0
    try {
        result = execute(*args)
    } catch (th: Throwable) {
        System.err.println("Incorrect arguments: $th")
        result = 1
    } finally {
        exitProcess(result)
    }
}

private fun execute(vararg args: String): Int {
    val cmdArgs = CmdArgs()
    if (cmdArgs.processArgs(*args)) {
        return process(cmdArgs)
    }
    return 0
}

private fun process(cmdArgs: CmdArgs): Int {
    val pluginManager = JadxPluginManager().apply { load() }
    val loadedInputs: MutableList<ILoadResult> = ArrayList()
    for (inputPlugin in pluginManager.inputPlugins) {
        loadedInputs.add(inputPlugin.loadFiles(listOf(Path.of(cmdArgs.inputFile))))
    }

    val jadxArgs = JadxArgs()
    jadxArgs.renameFlags = EnumSet.noneOf(RenameEnum::class.java)
    val root = RootNode(jadxArgs)
    root.loadClasses(loadedInputs)

    // from pre-decompilation stage run only SignatureProcessor
    val signatureProcessor = SignatureProcessor().apply { init(root) }
    for (classNode in root.classes) {
        signatureProcessor.visit(classNode)
    }

    // load method insns
    val set = ClsSet(root)
    set.loadFrom(root)

    // loading sensitive api config
    sensitives = Gson().fromJson(File(cmdArgs.sensitiveApiConfig).bufferedReader(), object : TypeToken<List<BriefMethod>>() {}.type)

    for (method in sensitives) {
        search(root.classes, method, method)
    }

    output(cmdArgs)

    return 0
}

private var found = false

private fun search(classes: List<ClassNode>, targetNode: BriefMethod, leafNode: BriefMethod) {
    var targetParentNode: BriefMethod? = null
    classes@ for (clsNode in classes) {
        methods@ for (methodNode in clsNode.methods) {
            if (methodNode.instructions == null) continue
            for (insnNode in methodNode.instructions) {
                if (insnNode?.type != InsnType.INVOKE) continue
                if (foundInsnNodeList.contains(insnNode)) continue@methods
                val callMth: MethodInfo = (insnNode as InvokeNode).callMth
                val cls = callMth.declClass.rawName
                if (cls != targetNode.owner || callMth.shortId != targetNode.descriptor) continue
                if (targetNode == leafNode) {
                    found = true
                    foundInsnNodeList.add(insnNode)
                    callChainList.add(leafNode)
                }
                targetParentNode = BriefMethod(
                    methodNode.methodInfo.declClass.rawName,
                    methodNode.methodInfo.shortId,
                )
                callChainList.add(targetParentNode)
                break@classes
            }
        }
    }

    if (targetParentNode != null) {
        search(classes, targetParentNode, leafNode)
    } else if (found) {
        found = false
        search(classes, leafNode, leafNode)
    }
}

private fun output(cmdArgs: CmdArgs) {
    // loading mapping
    val mappingFile = cmdArgs.mappingFile
    if (mappingFile != null) {
        mappings = Parsers[Parsers.PRO_GUARD]?.parse(File(mappingFile).toURI())
    }

    val outputFile = File(cmdArgs.outputDir, "output_${System.currentTimeMillis()}.txt")
    BufferedWriter(FileWriter(outputFile, false)).use { writer ->
        var depth = 0
        for (i in callChainList.size - 1 downTo 0) {
            val method = callChainList[i]
            val isLeaf = sensitives.any { it.owner == method.owner && it.descriptor == method.descriptor }
            if (!isLeaf) {
                val mappedClass = mappings?.classes?.getByFake(method.owner)
                val owner = mappedClass?.realName ?: method.owner
                val descriptor = mappedClass?.methods?.getByFake(method.descriptor)?.realName ?: method.descriptor
                writer.appendLine(TAB.repeat(depth++) + "$owner.$descriptor")
            } else {
                writer.appendLine(TAB.repeat(depth) + "${method.owner}.${method.descriptor}")
                writer.appendLine()
                depth = 0
            }
        }
    }
}
