package jadx.core.dex.attributes.nodes;

import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.instructions.args.ArgType;

import java.util.List;

public class GenericInfoAttr implements IJadxAttribute {
	private final List<ArgType> genericTypes;
	private boolean explicit;

	public GenericInfoAttr(List<ArgType> genericTypes) {
		this.genericTypes = genericTypes;
	}

	public List<ArgType> getGenericTypes() {
		return genericTypes;
	}

	public boolean isExplicit() {
		return explicit;
	}

	public void setExplicit(boolean explicit) {
		this.explicit = explicit;
	}

	@Override
	public AType<GenericInfoAttr> getAttrType() {
		return AType.GENERIC_INFO;
	}

	@Override
	public String toString() {
		return "GenericInfoAttr{" + genericTypes + ", explicit=" + explicit + '}';
	}
}
