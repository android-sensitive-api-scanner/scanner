package jadx.core.dex.visitors.typeinference;

import jadx.core.dex.instructions.args.SSAVar;

import java.util.List;

public interface ITypeConstraint {

	List<SSAVar> getRelatedVars();

	boolean check(TypeSearchState state);
}
