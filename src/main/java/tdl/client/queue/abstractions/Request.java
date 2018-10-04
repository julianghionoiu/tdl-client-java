package tdl.client.queue.abstractions;

import com.amazonaws.services.sqs.model.Message;
import tdl.client.audit.Auditable;
import tdl.client.audit.PresentationUtils;
import tdl.client.queue.serialization.JsonRpcRequest;

import java.io.File;

/**
 * Created by julianghionoiu on 20/06/2015.
 */
public class Request implements Auditable {
    private final Message  originalMessage;
    private final JsonRpcRequest requestData;

    public Request(Message originalMessage, JsonRpcRequest requestData) {
        logToConsole("     Request creation");
        this.originalMessage = originalMessage;
        this.requestData = requestData;
    }

    public void logToConsole(String s) {
        if (new File("DEBUG").exists()) {
            System.out.println(s);
        }
    }

    public Message  getOriginalMessage() {
        logToConsole("     Request getOriginalMessage");
        return originalMessage;
    }

    public String getId() {
        logToConsole("     Request getId");
        return requestData.getId();
    }

    public String getMethodName() {
        logToConsole("     Request getMethodName");
        return requestData.getMethod();
    }

    public String[] getParams() {
        logToConsole("     Request getOriginalMessage");
        return requestData.getParams();
    }


    //~~~ Pretty print

    @Override
    public String getAuditText() {
        logToConsole("     Request getAuditText");
        return String.format("id = %s, req = %s(%s)",
                getId(), getMethodName(), PresentationUtils.toDisplayableString(getParams()));
    }
}
