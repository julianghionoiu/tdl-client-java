package tdl.client.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.function.Consumer;


public class ChallengeSession {
    private final Logger LOG = LoggerFactory.getLogger(ChallengeSession.class);
    private String hostname;
    private int port;
    private String journeyId;
    private boolean useColours;
    private final String username;
    private BufferedReader reader;
    private PrintStream writer;
    private ImplementationRunner implementationRunner;

    public static ChallengeSession forUsername(@SuppressWarnings("SameParameterValue") String username) {
        return new ChallengeSession(username);
    }

    private ChallengeSession(String username) {
        this.username = username;
    }

    public ChallengeSession withServerHostname(@SuppressWarnings("SameParameterValue") String hostname) {
        this.hostname = hostname;
        return this;
    }

    public ChallengeSession withPort(int port) {
        this.port = port;
        return this;
    }

    public ChallengeSession withJourneyId(String journeyId) {
        this.journeyId = journeyId;
        return this;
    }

    public ChallengeSession withColours(boolean useColours) {
        this.useColours = useColours;
        return this;
    }

    public ChallengeSession withBufferedReader(BufferedReader reader) {
        this.reader = reader;
        return this;
    }

    public ChallengeSession withOutputStream(PrintStream out) {
        this.writer = out;
        return this;
    }

    public ChallengeSession withImplementationRunner(ImplementationRunner implementationRunner) {
        this.implementationRunner = implementationRunner;
        return this;
    }

    //~~~~~~~~ The entry point ~~~~~~~~~

    public void start(String[] args) {
        implementationRunner.withWriter(writer);
        if (!RecordingSystem.isRecordingSystemOk()) {
            writer.println("Please run `record_screen_and_upload` before continuing.");
            return;
        }
        writer.println("Connecting to " + hostname);
        runApp(args);
    }

    private void runApp(String[] args) {
        CombinedClient combinedClient = new CombinedClient(journeyId, useColours, hostname, port, writer, implementationRunner);

        try {
            boolean shouldContinue = combinedClient.checkStatusOfChallenge();
            if (shouldContinue) {
                String userInput = getUserInput(args);
                String roundDescription = combinedClient.executeUserAction(userInput);
                RoundManagement.saveDescription(roundDescription, writer);
            }
        }  catch (HttpClient.ServerErrorException e) {
            LOG.error("Server experienced an error. Try again.", e);
        } catch (HttpClient.OtherCommunicationException e) {
            LOG.error("Client threw an unexpected error.", e);
        } catch (HttpClient.ClientErrorException e) {
            LOG.error("The client sent something the server didn't expect.");
            writer.println(e.getResponseMessage());
        }
    }

    private String getUserInput(String[] args) {
        return args.length > 0 ? args[0] : readInputFromConsole();
    }

    private String readInputFromConsole() {
        try {
            return reader.readLine();
        } catch (IOException e) {
            LOG.error("Could not read user input.", e);
            return "error";
        }
    }
}
