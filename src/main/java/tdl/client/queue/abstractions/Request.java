package tdl.client.queue.abstractions;

import com.google.gson.JsonElement;
import tdl.client.audit.Auditable;
import tdl.client.audit.PresentationUtils;
import tdl.client.queue.serialization.JsonRpcRequest;
import tdl.client.queue.transport.StringMessage;

import java.util.List;

/**
 * Created by julianghionoiu on 20/06/2015.
 */
public class Request implements Auditable {
    private final StringMessage originalMessage;
    private final JsonRpcRequest requestData;

    public Request(StringMessage originalMessage, JsonRpcRequest requestData) {
        this.originalMessage = originalMessage;
        this.requestData = requestData;
    }

    public StringMessage getOriginalMessage() {
        return originalMessage;
    }

    public String getId() {
        return requestData.getId();
    }

    public String getMethodName() {
        return requestData.getMethod();
    }

    public List<JsonElement> getParams() {
        return requestData.getParams();
    }


    //~~~ Pretty print

    @Override
    public String getAuditText() {
        return String.format("id = %s, req = %s(%s)",
                getId(), getMethodName(), PresentationUtils.toDisplayableRequest(getParams()));
    }


}
