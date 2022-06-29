package jadx.core.dex.visitors.typeinference;

import jadx.core.dex.instructions.InsnType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class TypeUpdateRegistry {

	private final Map<InsnType, List<ITypeListener>> listenersMap = new EnumMap<>(InsnType.class);

	public void add(InsnType insnType, ITypeListener listener) {
		listenersMap.computeIfAbsent(insnType, k -> new ArrayList<>(3)).add(listener);
	}

	@NotNull
	public List<ITypeListener> getListenersForInsn(InsnType insnType) {
		List<ITypeListener> list = listenersMap.get(insnType);
		if (list == null) {
			return Collections.emptyList();
		}
		return list;
	}
}
