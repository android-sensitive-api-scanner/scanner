package jadx.api.plugins.input.data;

import jadx.api.plugins.input.data.attributes.IJadxAttribute;

import java.util.List;

public interface IFieldData extends IFieldRef {

	int getAccessFlags();

	List<IJadxAttribute> getAttributes();
}
