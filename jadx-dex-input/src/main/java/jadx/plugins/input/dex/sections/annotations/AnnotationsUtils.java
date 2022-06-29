package jadx.plugins.input.dex.sections.annotations;

import jadx.api.plugins.input.data.annotations.EncodedType;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.annotations.IAnnotation;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class AnnotationsUtils {

	@SuppressWarnings("unchecked")
	public static <T> T getValue(IAnnotation ann, String name, EncodedType type, T defValue) {
		if (ann == null || ann.getValues() == null || ann.getValues().isEmpty()) {
			return defValue;
		}
		EncodedValue encodedValue = ann.getValues().get(name);
		if (encodedValue == null || encodedValue.getType() != type) {
			return defValue;
		}
		return (T) encodedValue.getValue();
	}

	@Nullable
	public static Object getValue(IAnnotation ann, String name, EncodedType type) {
		if (ann == null || ann.getValues() == null || ann.getValues().isEmpty()) {
			return null;
		}
		EncodedValue encodedValue = ann.getValues().get(name);
		if (encodedValue == null || encodedValue.getType() != type) {
			return null;
		}
		return encodedValue.getValue();
	}

	@SuppressWarnings("unchecked")
	public static List<EncodedValue> getArray(IAnnotation ann, String name) {
		if (ann == null || ann.getValues() == null || ann.getValues().isEmpty()) {
			return Collections.emptyList();
		}
		EncodedValue encodedValue = ann.getValues().get(name);
		if (encodedValue == null || encodedValue.getType() != EncodedType.ENCODED_ARRAY) {
			return Collections.emptyList();
		}
		return (List<EncodedValue>) encodedValue.getValue();
	}
}
