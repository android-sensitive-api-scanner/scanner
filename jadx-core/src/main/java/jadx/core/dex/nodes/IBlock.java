package jadx.core.dex.nodes;

import jadx.api.ICodeWriter;
import jadx.core.codegen.RegionGen;
import jadx.core.utils.exceptions.CodegenException;

import java.util.List;

public interface IBlock extends IContainer {

	List<InsnNode> getInstructions();

	@Override
	default void generate(RegionGen regionGen, ICodeWriter code) throws CodegenException {
		regionGen.makeSimpleBlock(this, code);
	}
}
