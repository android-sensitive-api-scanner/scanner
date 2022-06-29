package jadx.api.plugins.input.data;

import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface IClassData {
	IClassData copy();

	String getInputFileName();

	String getType();

	int getAccessFlags();

	@Nullable
	String getSuperType();

	List<String> getInterfacesTypes();

	void visitFieldsAndMethods(ISeqConsumer<IFieldData> fieldsConsumer, ISeqConsumer<IMethodData> mthConsumer);

	List<IJadxAttribute> getAttributes();

	String getDisassembledCode();
}
