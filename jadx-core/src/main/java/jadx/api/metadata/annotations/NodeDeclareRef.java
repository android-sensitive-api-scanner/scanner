package jadx.api.metadata.annotations;

import jadx.api.metadata.ICodeAnnotation;
import jadx.api.metadata.ICodeNodeRef;

import java.util.Objects;

public class NodeDeclareRef implements ICodeAnnotation {

	private final ICodeNodeRef node;

	private int defPos;

	public NodeDeclareRef(ICodeNodeRef node) {
		this.node = Objects.requireNonNull(node);
	}

	public ICodeNodeRef getNode() {
		return node;
	}

	public int getDefPos() {
		return defPos;
	}

	public void setDefPos(int defPos) {
		this.defPos = defPos;
	}

	@Override
	public AnnType getAnnType() {
		return AnnType.DECLARATION;
	}

	@Override
	public String toString() {
		return "NodeDeclareRef{" + node + '}';
	}
}
