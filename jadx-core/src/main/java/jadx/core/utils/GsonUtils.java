package jadx.core.utils;

import com.google.gson.*;

import java.lang.reflect.Type;

public class GsonUtils {

	public static <T> InterfaceReplace<T> interfaceReplace(Class<T> replaceCls) {
		return new InterfaceReplace<>(replaceCls);
	}

	private static final class InterfaceReplace<T> implements JsonSerializer<T>, JsonDeserializer<T> {
		private final Class<T> replaceCls;

		private InterfaceReplace(Class<T> replaceCls) {
			this.replaceCls = replaceCls;
		}

		@Override
		public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			return context.deserialize(json, this.replaceCls);
		}

		@Override
		public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
			return context.serialize(src, this.replaceCls);
		}
	}
}
