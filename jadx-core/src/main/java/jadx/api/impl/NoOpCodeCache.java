package jadx.api.impl;

import jadx.api.ICodeCache;
import jadx.api.ICodeInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NoOpCodeCache implements ICodeCache {

	public static final NoOpCodeCache INSTANCE = new NoOpCodeCache();

	@Override
	public void add(String clsFullName, ICodeInfo codeInfo) {
		// do nothing
	}

	@Override
	public void remove(String clsFullName) {
		// do nothing
	}

	@Override
	@NotNull
	public ICodeInfo get(String clsFullName) {
		return ICodeInfo.EMPTY;
	}

	@Override
	public @Nullable String getCode(String clsFullName) {
		return null;
	}

	@Override
	public boolean contains(String clsFullName) {
		return false;
	}

	@Override
	public void close() {
		// do nothing
	}

	@Override
	public String toString() {
		return "NoOpCodeCache";
	}
}
