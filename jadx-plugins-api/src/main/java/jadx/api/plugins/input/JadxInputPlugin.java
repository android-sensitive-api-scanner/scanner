package jadx.api.plugins.input;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.input.data.ILoadResult;

import java.nio.file.Path;
import java.util.List;

public interface JadxInputPlugin extends JadxPlugin {
	ILoadResult loadFiles(List<Path> input);
}
