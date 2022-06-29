package jadx.api.plugins.input.data;

import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.insns.custom.ICustomPayload;

import java.util.List;

public interface ICallSite extends ICustomPayload {

	List<EncodedValue> getValues();

	void load();
}
