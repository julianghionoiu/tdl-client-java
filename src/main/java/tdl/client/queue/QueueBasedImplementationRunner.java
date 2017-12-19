package tdl.client.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tdl.client.queue.abstractions.Request;
import tdl.client.queue.abstractions.UserImplementation;
import tdl.client.queue.abstractions.response.Response;
import tdl.client.queue.actions.ClientAction;
import tdl.client.audit.AuditStream;
import tdl.client.audit.Auditable;
import tdl.client.audit.StdoutAuditStream;
import tdl.client.queue.transport.BrokerCommunicationException;
import tdl.client.queue.transport.RemoteBroker;
import tdl.client.runner.IConsoleOut;
import tdl.client.runner.RoundManagement;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static tdl.client.queue.actions.ClientActions.publish;

public class QueueBasedImplementationRunner implements ImplementationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueueBasedImplementationRunner.class);
    private final String hostname;
    private final int port;
    private final String uniqueId;
    private final Audit audit;
    private int requestTimeoutMillis;
    private final Map<String, UserImplementation> solutions;
    private IConsoleOut consoleOut;

    QueueBasedImplementationRunner(String hostname, int port, String uniqueId, int requestTimeoutMillis,
                                   AuditStream auditStream, Map<String, UserImplementation> solutions, IConsoleOut consoleOut) {
        this.hostname = hostname;
        this.port = port;
        this.uniqueId = uniqueId;
        this.requestTimeoutMillis = requestTimeoutMillis;
        this.audit = new Audit(auditStream);
        this.solutions = solutions;
        this.consoleOut = consoleOut;
    }

    public static class Builder {
        private String hostname;
        private String username;
        private int port;
        private String uniqueId;
        private int requestTimeoutMillis;
        private AuditStream auditStream = new StdoutAuditStream();
        private final Map<String, UserImplementation> solutions;
        private IConsoleOut consoleOut;

        public Builder() {
            port = 61616;
            requestTimeoutMillis = 500;
            this.solutions = new HashMap<>();
        }

        public Builder setHostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        public Builder setUniqueId(String uniqueId) {
            this.uniqueId = uniqueId;
            return this;
        }

        @SuppressWarnings("WeakerAccess")
        public Builder setRequestTimeoutMillis(@SuppressWarnings("SameParameterValue") int requestTimeoutMillis) {
            this.requestTimeoutMillis = requestTimeoutMillis;
            return this;
        }

        @SuppressWarnings("WeakerAccess")
        public Builder setAuditStream(AuditStream auditStream) {
            this.auditStream = auditStream;
            return this;
        }

        public Builder withSolutionFor(String methodName, UserImplementation solution) {
            solutions.put(methodName, solution);
            return this;
        }

        public Builder withConsoleOut(IConsoleOut consoleOut) {
            this.consoleOut = consoleOut;
            return this;
        }

        public QueueBasedImplementationRunner create() {
            return new QueueBasedImplementationRunner(hostname, port, uniqueId, requestTimeoutMillis, auditStream, solutions, consoleOut);
        }
    }

    public void run() {
        ProcessingRules processingRules = createDeployProcessingRules();
        goLiveWith(processingRules);
    }

    private ProcessingRules createDeployProcessingRules() {
        ProcessingRules deployProcessingRules = new ProcessingRules();

        // Debt - do we need this anymore?
        deployProcessingRules
                .on("display_description")
                .call(p -> RoundManagement.saveDescription(p[0], p[1], consoleOut))
                .then(publish());

        solutions.forEach((methodName, userImplementation) -> deployProcessingRules
                .on(methodName)
                .call(userImplementation)
                .then(publish()));

        return deployProcessingRules;
    }

    public int getRequestTimeoutMillis() {
        return requestTimeoutMillis;
    }

    public void goLiveWith(ProcessingRules processingRules) {
        audit.logLine("Starting client");
        try (RemoteBroker remoteBroker = new RemoteBroker(hostname, port, uniqueId, requestTimeoutMillis)){
            //Design: We use a while loop instead of an ActiveMQ MessageListener to process the messages in order
            audit.logLine("Waiting for requests");
            Optional<Request> request = remoteBroker.receive();
            while (request.isPresent()) {
                request = applyProcessingRules(request.get(), processingRules, remoteBroker);
            }
        } catch (Exception e) {
            String message = "There was a problem processing messages";
            LOGGER.error(message, e);
            audit.logException(message, e);
        }
        audit.logLine("Stopping client");
    }

    private Optional<Request> applyProcessingRules(
            Request request, ProcessingRules processingRules, RemoteBroker remoteBroker)
            throws BrokerCommunicationException {
        audit.startLine();
        audit.log(request);

        //Obtain response from user
        Response response = processingRules.getResponseFor(request);
        audit.log(response);

        //Obtain action
        ClientAction clientAction = response.getClientAction();

        //Act
        clientAction.afterResponse(remoteBroker, request, response);
        audit.log(clientAction);
        audit.endLine();
        return clientAction.getNextRequest(remoteBroker);
    }

    //~~~~ Utils

    private static class Audit {
        private final AuditStream auditStream;
        private StringBuilder line;

        Audit(AuditStream auditStream) {
            this.auditStream = auditStream;
            startLine();
        }

        //~~~ Normal output

        void startLine() {
            line = new StringBuilder();
        }

        void log(Auditable auditable) {
            String text = auditable.getAuditText();
            if (!text.isEmpty() && line.length() > 0) {
                line.append(", ");
            }
            line.append(text);
        }

        void endLine() {
            auditStream.println(line.toString());
        }

        //~~~ Exception

        void logException(String message, Exception e) {
            startLine();
            line.append(message).append(": ").append(e.getMessage());
            endLine();
        }

        void logLine(String text) {
            startLine();
            this.line.append(text);
            endLine();
        }
    }

}
