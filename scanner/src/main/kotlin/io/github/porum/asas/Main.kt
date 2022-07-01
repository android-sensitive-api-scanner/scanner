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
import jadx.core.dex.info.FieldInfo
import jadx.core.dex.info.MethodInfo
import jadx.core.dex.instructions.IndexInsnNode
import jadx.core.dex.instructions.InsnType
import jadx.core.dex.instructions.InvokeNode
import jadx.core.dex.nodes.ClassNode
import jadx.core.dex.nodes.InsnNode
import jadx.core.dex.nodes.RootNode
import jadx.core.dex.visitors.SignatureProcessor
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
private lateinit var sensitives: List<Node>

private val foundInsnNodeList: MutableList<InsnNode> = ArrayList()
private val callChainList: MutableList<Node> = ArrayList()

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
    sensitives = Gson().fromJson(File(cmdArgs.sensitiveApiConfig).bufferedReader(), object : TypeToken<List<Node>>() {}.type)

    for (node in sensitives) {
        search(root.classes, node, node)
    }

    output(cmdArgs)

    return 0
}

private var found = false

private fun search(classes: List<ClassNode>, targetNode: Node, leafNode: Node) {
    var targetParentNode: Node? = null
    classes@ for (clsNode in classes) {
        methods@ for (methodNode in clsNode.methods) {
            if (methodNode.instructions == null) continue@methods
            instructions@ for (insnNode in methodNode.instructions) {
                if (insnNode == null) continue@instructions
                if (foundInsnNodeList.contains(insnNode)) continue@methods
                val owner: String
                val name: String
                val desc: String
                if (targetNode == leafNode) {
                    if (leafNode.descriptor.startsWith('(')) { // invoke-
                        if (insnNode.type != InsnType.INVOKE) continue@instructions
                        val callMth: MethodInfo = (insnNode as InvokeNode).callMth
                        owner = callMth.declClass.rawName
                        name = callMth.name
                        desc = callMth.shortId.substring(callMth.shortId.indexOf('('))
                    } else { // field
                        if (insnNode.type != InsnType.SGET) continue@instructions
                        val fieldInfo = (insnNode as IndexInsnNode).index as FieldInfo
                        owner = fieldInfo.declClass.rawName
                        name = fieldInfo.name
                        desc = fieldInfo.shortId.substring(fieldInfo.shortId.indexOf(':') + 1)
                    }
                } else {
                    if (insnNode.type != InsnType.INVOKE) continue@instructions
                    val callMth: MethodInfo = (insnNode as InvokeNode).callMth
                    owner = callMth.declClass.rawName
                    name = callMth.name
                    desc = callMth.shortId.substring(callMth.shortId.indexOf('('))
                }
                if (owner != targetNode.owner || name != targetNode.name || desc != targetNode.descriptor) continue@instructions
                if (targetNode == leafNode) {
                    found = true
                    foundInsnNodeList.add(insnNode)
                    callChainList.add(leafNode)
                }
                targetParentNode = Node(
                    methodNode.methodInfo.declClass.rawName,
                    methodNode.methodInfo.name,
                    methodNode.methodInfo.shortId.substring(methodNode.methodInfo.shortId.indexOf('(')),
                )
                if (targetParentNode == targetNode) {
                    targetParentNode = null
                    break@classes
                }
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
            val node = callChainList[i]
            val isLeaf = sensitives.any { it.owner == node.owner && it.name == node.name && it.descriptor == node.descriptor }
            if (!isLeaf) {
                val mappedClass = mappings?.classes?.getByFake(node.owner)
                val owner = mappedClass?.realName ?: node.owner
                val descriptor = mappedClass?.methods?.getByFake("${node.name}${node.descriptor}")?.realName ?: node.descriptor
                writer.appendLine(TAB.repeat(depth++) + "$owner.$descriptor")
            } else {
                writer.appendLine(TAB.repeat(depth) + "${node.owner}.${node.name}${node.descriptor}")
                writer.appendLine()
                depth = 0
            }
        }
    }
}
