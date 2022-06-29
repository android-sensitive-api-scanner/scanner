package io.github.porum.mapping.parsers

import io.github.porum.mapping.*
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

private const val CLASS_REGEX = """^(\S+) -> (\S+):$"""
private const val FIELD_REGEX = """^(\S+) (\S+) -> (\S+)$"""
private const val METHOD_REGEX = """^((?<from>\d+):(?<to>\d+):)?(?<ret>[^:]+)\s(?<name>[^:]+)\((?<args>.*)\)((:(?<originalFrom>\d+))?(:(?<originalTo>\d+))?)?\s->\s(?<obf>[^:]+)"""

private const val ARCHIVE_NAME = "<none>"

object ProGuardMappingParser : MappingParser {
    private val classMatcher = Regex(CLASS_REGEX)
    private val methodMatcher = Regex(METHOD_REGEX)
    private val fieldMatcher = Regex(FIELD_REGEX)

    override val name: String = "proguard"

    override fun parse(mappingsIn: InputStream): MappedArchive =
        BufferedReader(InputStreamReader(mappingsIn)).use { reader ->
            // Converts a list to an ObfuscationMap
            fun <T : MappedNode> List<T>.toObfuscationMapping(mapper: (String, T) -> String = { it, _ -> it }): ObfuscationMap<T> =
                ObfuscationMap(associateBy { mapper(it.realName, it) to mapper(it.fakeName, it) })

            // List of classes in archive.
            val classes = ArrayList<MappedClass>()

            // If there are no lines to read return an empty mapped archive.
            if (!reader.ready()) return MappedArchive(ARCHIVE_NAME, ARCHIVE_NAME, ObfuscationMap())

            // Read the first line
            var line: String = reader.readLine().trim()

            // Start reading lines
            while (reader.ready()) {
                // Match for a class
                val gv = classMatcher.matchEntire(line)?.groupValues

                // Check if the result is null, if so read the next line and continue.
                if (gv == null) {
                    // Read a new line so we aren't stuck reading the same line forever.
                    line = reader.readLine().trim()

                    continue
                }

                // Now that we have a class, get the real and fake names
                val (_, realName, fakeName) = gv

                // List of methods in class
                val methods = ArrayList<MappedMethod>()
                // List of fields in class
                val fields = ArrayList<MappedField>()

                // Start reading lines
                while (reader.ready()) {
                    // Update the line
                    line = reader.readLine().trim() // Read a new line

                    // Check if the current line matched is a method definition
                    if (methodMatcher.matches(line)) {
                        // Get the result of this line, we know the match is not null due to the check above
                        val result = methodMatcher.matchEntire(line)!!.groups as MatchNamedGroupCollection

                        // Read the groups, map the types, and add a method node to the methods
                        methods.add(
                            MappedMethod(
                                realName = result["name"]!!.value, // Real name
                                fakeName = result["obf"]!!.value, // Fake name
                                lnStart = result["from"]?.value?.toIntOrNull(), // Start line
                                lnEnd = result["to"]?.value?.toIntOrNull(), // End line
                                originalLnStart = result["originalFrom"]?.value?.toIntOrNull(),
                                originalLnEnd = result["originalTo"]?.value?.toIntOrNull(),
                                parameters = if (result["args"]?.value.isNullOrEmpty()) emptyList() else result["args"]!!.value.split(',').map(::toTypeDescriptor), // Parameters
                                returnType = toTypeDescriptor(result["ret"]!!.value) // Return type
                            )
                        )
                    }
                    // Else, if the current line is a field
                    else if (fieldMatcher.matches(line)) {
                        // Again, match and get group values
                        val result = fieldMatcher.matchEntire(line)!!.groupValues

                        // Create a mapped field and add to the fields list
                        fields.add(
                            MappedField(
                                result[2], // Real name
                                result[3], // Fake name
                                toTypeDescriptor(result[1]) // Type
                            )
                        )
                    }

                    // # {"id":"sourceFile","fileName":"xxx.kt"}
                    else if (line.startsWith("#")) {
                        continue
                    }

                    else break
                }

                // Method and field reading is done, create a mapped class and add it to the classes list.
                classes.add(
                    MappedClass(
                        realName,
                        fakeName,
                        methods.toObfuscationMapping { name, node -> // Mapping method names to their bytecode equivalent as overloading is possible, and we can't relly on ambiguity of names alone.
                            "$name(${
                                node.parameters.joinToString(
                                    "",
                                    transform = DescriptorType::descriptor
                                )
                            })${node.returnType.descriptor}"
                        },
                        fields.toObfuscationMapping()
                    )
                )
            }

            // All classes read, can create a mapped archive and return.
            return MappedArchive(ARCHIVE_NAME, ARCHIVE_NAME, classes.toObfuscationMapping())
        }

    private fun toTypeDescriptor(desc: String): DescriptorType = when (desc) {
        "boolean" -> PrimitiveTypeDescriptor.BOOLEAN
        "char" -> PrimitiveTypeDescriptor.CHAR
        "byte" -> PrimitiveTypeDescriptor.BYTE
        "short" -> PrimitiveTypeDescriptor.SHORT
        "int" -> PrimitiveTypeDescriptor.INT
        "float" -> PrimitiveTypeDescriptor.FLOAT
        "long" -> PrimitiveTypeDescriptor.LONG
        "double" -> PrimitiveTypeDescriptor.DOUBLE
        "void" -> PrimitiveTypeDescriptor.VOID
        else -> {
            if (desc.endsWith("[]")) {
                val type = desc.removeSuffix("[]")

                ArrayTypeDescriptor(toTypeDescriptor(type))
            } else ClassTypeDescriptor(desc)
        }
    }
}