package io.github.porum.mapping

data class MappedArchive(
    override val realName: String, override val fakeName: String,
    val classes: ObfuscationMap<MappedClass>
) : MappedNode

data class MappedClass(
    override val realName: String, override val fakeName: String,
    val methods: ObfuscationMap<MappedMethod>,
    val fields: ObfuscationMap<MappedField>
) : MappedNode

data class MappedMethod(
    override val realName: String,
    override val fakeName: String,
    val lnStart: Int?,
    val lnEnd: Int?,
    val originalLnStart: Int?,
    val originalLnEnd: Int?,
    val parameters: List<DescriptorType>,
    val returnType: DescriptorType
) : MappedNode

data class MappedField(
    override val realName: String,
    override val fakeName: String,
    val type: DescriptorType
) : MappedNode

fun MappedMethod.fakeDescriptor(): String =
    "$fakeName(${parameters.joinToString("", transform = DescriptorType::descriptor)})${returnType.descriptor}"

fun MappedMethod.realDescriptor(): String =
    "$realName(${parameters.joinToString("", transform = DescriptorType::descriptor)})${returnType.descriptor}"