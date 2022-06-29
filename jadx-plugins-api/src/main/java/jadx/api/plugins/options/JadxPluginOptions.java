package jadx.api.plugins.options;

import jadx.api.plugins.JadxPlugin;

import java.util.List;
import java.util.Map;

public interface JadxPluginOptions extends JadxPlugin {

	void setOptions(Map<String, String> options);

	List<OptionDescription> getOptionsDescriptions();
}
