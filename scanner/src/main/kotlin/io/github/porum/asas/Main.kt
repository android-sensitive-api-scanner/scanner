package io.github.porum.asas

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.porum.mapping.*
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

private lateinit var mappings: MappedArchive
private lateinit var sensitives: List<TargetMethod>

private val refChainList: MutableList<Pair<String, MappedMethod>> = ArrayList()
private val foundInsnNodeList: MutableList<InsnNode> = ArrayList()

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

    // loading mapping
    mappings = Parsers[Parsers.PRO_GUARD]?.parse(File(cmdArgs.mappingFile).toURI()) ?: return 1
    // loading sensitive api config
    sensitives = Gson().fromJson(File(cmdArgs.sensitiveApiConfig).bufferedReader(), object : TypeToken<List<TargetMethod>>() {}.type)

    for (method in sensitives) {
        search(root.classes, method, method)
    }

    output(cmdArgs)

    return 0
}

private var found = false

private fun search(classes: List<ClassNode>, targetNode: TargetMethod, leafNode: TargetMethod) {
    var targetParentNode: TargetMethod? = null
    classes@ for (clsNode in classes) {
        methods@ for (methodNode in clsNode.methods) {
            if (methodNode.instructions == null) continue
            for (insnNode in methodNode.instructions) {
                if (insnNode?.type != InsnType.INVOKE) continue
                if (foundInsnNodeList.contains(insnNode)) continue@methods
                val callMth: MethodInfo = (insnNode as InvokeNode).callMth
                val mappedClass: MappedClass = mappings.classes.getByFake(callMth.declClass.rawName) ?: continue
                if (mappedClass.realName != targetNode.owner) continue
                val mappedMethod: MappedMethod = mappedClass.methods.getByFake(callMth.getDescriptor()) ?: continue
                if (mappedMethod.realDescriptor() != targetNode.descriptor) continue
                if (targetNode == leafNode) {
                    found = true
                    foundInsnNodeList.add(insnNode)
                    refChainList.add(Pair(mappedClass.realName, mappedMethod))
                }
                val parentNodeMappedClass = mappings.classes.getByFake(methodNode.methodInfo.declClass.rawName) ?: break@classes
                val parentNodeMappedMethod = parentNodeMappedClass.methods.getByFake(methodNode.methodInfo.getDescriptor()) ?: break@classes
                targetParentNode = TargetMethod(
                    parentNodeMappedClass.realName,
                    parentNodeMappedMethod.realDescriptor()
                )
                refChainList.add(Pair(parentNodeMappedClass.realName, parentNodeMappedMethod))
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
    val outputFile = File(cmdArgs.outputDir, "output_${System.currentTimeMillis()}.txt")
    BufferedWriter(FileWriter(outputFile, false)).use { writer ->
        var depth = 0
        for (i in refChainList.size - 1 downTo 0) {
            val className = refChainList[i].first
            val mappedMethod = refChainList[i].second
            writer.appendLine(TAB.repeat(depth++) + "$className.${mappedMethod.realDescriptor()}")
            val isLeaf = sensitives.any { it.owner == className && it.descriptor == mappedMethod.realDescriptor() }
            if (isLeaf) {
                writer.appendLine()
                depth = 0
            }
        }
    }
}

private fun MethodInfo.getDescriptor(): String {
    val sb = StringBuilder()
    sb.append(name)
    sb.append('(')
    for (arg in argumentsTypes) {
        sb.append(arg.signature())
    }
    sb.append(')')
    if (returnType != null) {
        sb.append(returnType.signature())
    }
    return sb.toString()
}

private fun ArgType.signature(): String? {
    if (primitiveType == PrimitiveType.OBJECT) {
        val obj = getObject()
        val originalObject = mappings.classes.getByFake(obj)?.realName ?: obj
        return Utils.makeQualifiedObjectName(originalObject)
    }
    return if (primitiveType == PrimitiveType.ARRAY) {
        '['.toString() + arrayElement.signature()
    } else primitiveType.shortName
}