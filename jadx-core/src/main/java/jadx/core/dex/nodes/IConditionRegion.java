package jadx.core.dex.nodes;

import jadx.core.dex.regions.conditions.IfCondition;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface IConditionRegion extends IRegion {

	@Nullable
	IfCondition getCondition();

	/**
	 * Blocks merged into condition
	 * Needed for backtracking
	 * TODO: merge into condition object ???
	 */
	List<BlockNode> getConditionBlocks();

	void invertCondition();

	boolean simplifyCondition();

	int getConditionSourceLine();
}
