package jadx.api.plugins.input.data;

import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface IMethodData {

	IMethodRef getMethodRef();

	int getAccessFlags();

	@Nullable
	ICodeReader getCodeReader();

	String disassembleMethod();

	List<IJadxAttribute> getAttributes();
}
