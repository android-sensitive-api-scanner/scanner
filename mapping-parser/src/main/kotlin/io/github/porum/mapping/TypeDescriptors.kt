package io.github.porum.mapping

enum class PrimitiveTypeDescriptor(
    _descriptor: Char
) : DescriptorType {
    BOOLEAN('Z'),
    CHAR('C'),
    BYTE('B'),
    SHORT('S'),
    INT('I'),
    FLOAT('F'),
    LONG('J'),
    DOUBLE('D'),
    VOID('V');

    override val descriptor: String = _descriptor.toString()
}

class ClassTypeDescriptor(
    val classname: String
) : DescriptorType {
    override val descriptor: String = "L${classname.replace('.', '/')};"
}

class ArrayTypeDescriptor(
    val arrayType: DescriptorType
) : DescriptorType {

    override val descriptor: String = "[${arrayType.descriptor}"
}