package jadx.api.metadata;

import jadx.api.metadata.impl.CodeMetadataStorage;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.BiFunction;

public interface ICodeMetadata {

	ICodeMetadata EMPTY = CodeMetadataStorage.empty();

	@Nullable
	ICodeAnnotation getAt(int position);

	@Nullable
	ICodeAnnotation getClosestUp(int position);

	@Nullable
	ICodeAnnotation searchUp(int position, ICodeAnnotation.AnnType annType);

	@Nullable
	ICodeAnnotation searchUp(int position, int limitPos, ICodeAnnotation.AnnType annType);

	/**
	 * Iterate code annotations from {@code startPos} to smaller positions.
	 *
	 * @param visitor
	 *                return not null value to stop iterations
	 */
	@Nullable
	<T> T searchUp(int startPos, BiFunction<Integer, ICodeAnnotation, T> visitor);

	/**
	 * Iterate code annotations from {@code startPos} to higher positions.
	 *
	 * @param visitor
	 *                return not null value to stop iterations
	 */
	@Nullable
	<T> T searchDown(int startPos, BiFunction<Integer, ICodeAnnotation, T> visitor);

	/**
	 * Get current node at position (can be enclosing class or method)
	 */
	@Nullable
	ICodeNodeRef getNodeAt(int position);

	/**
	 * Any definition of class or method below position
	 */
	@Nullable
	ICodeNodeRef getNodeBelow(int position);

	Map<Integer, ICodeAnnotation> getAsMap();

	Map<Integer, Integer> getLineMapping();
}
