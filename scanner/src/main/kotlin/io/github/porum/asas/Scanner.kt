package io.github.porum.asas

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import io.github.porum.mapping.MappedArchive
import io.github.porum.mapping.Parsers
import jadx.api.JadxArgs
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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.nio.file.Path
import java.util.*

/**
 * Created by panda on 2023/8/18 15:00
 */

private const val TAB = "   "

class Scanner : CliktCommand() {
  val apk: String by argument().help { "apk file" }
  val apis: String by option().prompt("sensitive apis json file").help { "sensitive apis json file" }
  val output: String by option().prompt("output directory").help { "output directory" }
  val mapping: String? by option().help { "mapping file" }

  private lateinit var sensitiveAPIs: List<Node>
  private val foundInsnNodeList: MutableList<InsnNode> = ArrayList()
  private val callChainList: MutableList<Node> = ArrayList()

  @OptIn(ExperimentalSerializationApi::class)
  override fun run() {
    val pluginManager = JadxPluginManager().apply { load() }
    val loadedInputs: MutableList<ILoadResult> = ArrayList()
    for (inputPlugin in pluginManager.inputPlugins) {
      loadedInputs.add(inputPlugin.loadFiles(listOf(Path.of(apk))))
    }

    val jadxArgs = JadxArgs()
    jadxArgs.renameFlags = EnumSet.noneOf(JadxArgs.RenameEnum::class.java)
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

    // load sensitive apis
    sensitiveAPIs = Json.decodeFromStream(File(apis).inputStream())

    val size = sensitiveAPIs.size
    for (i in 0 until size) {
      val node = sensitiveAPIs[i]
      echo("[${i + 1}/$size] searching ${node.owner}.${node.name}")
      search(root.classes, node, node)
    }

    output()
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

  private fun output() {
    // load mapping info
    val mappedArchive: MappedArchive? = mapping?.let {
      Parsers[Parsers.PRO_GUARD]?.parse(File(it).toURI())
    }

    val outputFile = File(File(output).also { it.mkdir() }, "output_${System.currentTimeMillis()}.txt")
    BufferedWriter(FileWriter(outputFile, false)).use { writer ->
      var depth = 0
      for (i in callChainList.size - 1 downTo 0) {
        val node = callChainList[i]
        val isLeaf = sensitiveAPIs.any { it.owner == node.owner && it.name == node.name && it.descriptor == node.descriptor }
        if (!isLeaf) {
          val mappedClass = mappedArchive?.classes?.getByFake(node.owner)
          val owner = mappedClass?.realName ?: node.owner
          val descriptor = mappedClass?.methods?.getByFake("${node.name}${node.descriptor}")?.realName ?: node.descriptor
          writer.appendLine(TAB.repeat(depth++) + "$owner.$descriptor")
        } else {
          if (node.descriptor.startsWith('(')) {
            writer.appendLine(TAB.repeat(depth) + "${node.owner}.${node.name}${node.descriptor}")
          } else {
            writer.appendLine(TAB.repeat(depth) + "${node.owner}.${node.name}:${node.descriptor}")
          }
          writer.appendLine()
          depth = 0
        }
      }
    }

    echo(outputFile.absolutePath)
  }

}