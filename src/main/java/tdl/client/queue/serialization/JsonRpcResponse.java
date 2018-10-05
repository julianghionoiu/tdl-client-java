package tdl.client.queue.serialization;

import tdl.client.queue.abstractions.response.Response;
import tdl.client.queue.actions.ClientAction;

/**
 * Created by julianghionoiu on 10/01/2016.
 */
@SuppressWarnings("FieldCanBeLocal")
public final class JsonRpcResponse {
    private final Object result;
    private final String error;
    private final String id;

    @SuppressWarnings("SameParameterValue")
    private JsonRpcResponse(Object result, String error, String id) {
        this.result = result;
        this.error = error;
        this.id = id;
    }

    static JsonRpcResponse from(Response response) {
        return new JsonRpcResponse(response.getResult(), null, response.getId());
    }

    public Response toResponse() {
        return new Response() {
            @Override
            public String getId() {
                return id;
            }

            @Override
            public Object getResult() {
                return result;
            }

            @Override
            public ClientAction getClientAction() {
                return null;
            }

            @Override
            public String getAuditText() {
                return null;
            }
        };
    }
}