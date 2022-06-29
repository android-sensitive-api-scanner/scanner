package jadx.core.dex.visitors.typeinference;

import jadx.core.dex.instructions.args.ArgType;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Set;

public class TypeInfo {
	private ArgType type = ArgType.UNKNOWN;

	private final Set<ITypeBound> bounds = new LinkedHashSet<>();

	@NotNull
	public ArgType getType() {
		return type;
	}

	public void setType(ArgType type) {
		this.type = type;
	}

	public Set<ITypeBound> getBounds() {
		return bounds;
	}

	@Override
	public String toString() {
		return "TypeInfo{type=" + type + ", bounds=" + bounds + '}';
	}
}
