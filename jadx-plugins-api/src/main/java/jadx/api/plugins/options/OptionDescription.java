package jadx.api.plugins.options;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface OptionDescription {

	String name();

	String description();

	/**
	 * Possible values.
	 * Empty if not a limited set
	 */
	List<String> values();

	/**
	 * Default value.
	 * Null if required
	 */
	@Nullable
	String defaultValue();
}
