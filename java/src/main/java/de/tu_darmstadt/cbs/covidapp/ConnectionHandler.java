package de.tu_darmstadt.cbs.covidapp;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.UnknownHostException;
import java.util.Base64;
import java.io.DataOutputStream;

import javax.net.ssl.SSLSocket;

import de.tu_darmstadt.cbs.emailsmpc.Bin;
import de.tu_darmstadt.cbs.emailsmpc.Message;
import de.tu_darmstadt.cbs.emailsmpc.Participant;

public class ConnectionHandler implements Runnable {
    public final SSLSocket clientSocket;
    private final CovidApp app;

    public ConnectionHandler(SSLSocket client, CovidApp app) {
        this.clientSocket = client;
        this.app = app;
    }

    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String nextLine;
            String message = "";
            while ((nextLine = in.readLine()) != null) {
                message += nextLine;
            }
            in.close();
            handleMessage(message);
            clientSocket.close();
        } catch (Exception e) {
            System.err.println("An Error occured handling the connection: " + e);
            e.printStackTrace();
        }
    }

    private Participant getParticipant(String message) throws IOException, ClassNotFoundException {
        byte[] data = Base64.getDecoder().decode(message);
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
        Participant participant = (Participant) ois.readObject();
        ois.close();
        return participant;
    }

    private int getParticipantId(Participant participant) {
        try {
            app.readLockModel();
            int id = app.getModel().getParticipantId(participant);
            return id;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            app.readUnlockModel();
        }
        // Is never reached
        return -1;
    }

    private void setModel(String message) {
        try {
            Message modelMsg = Message.deserializeMessage(message);
            app.writeLockModel();
            app.getModel().toContinuousFromParticipating(modelMsg.data);
            app.ownId = app.getModel().ownId;
            app.initialized.countDown();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            app.writeUnlockModel();
        }
    }

    private void setShare(String message, Participant sender) {
        try {
            Message shareMsg = Message.deserializeMessage(message);
            app.writeLockModel();
            app.getModel().setShareFromContinuousMessage(shareMsg, sender, false);
            if (app.getModel().isResultComputable() == true)
                app.sumComputable.countDown();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            app.writeUnlockModel();
        }
    }

    private void bufferResultShare(String message, Participant sender) {
        try {
            Message shareMsg = Message.deserializeMessage(message);
            app.readLockModel();
            int senderId = app.getModel().getParticipantId(sender);
            app.resultMessageBuffer.add(shareMsg, sender, senderId);
            if (app.resultMessageBuffer.isComputable())
                app.resultComputable.countDown();

        } catch (ClassNotFoundException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            app.readUnlockModel();
        }
        System.out.println("Got ResultShare from " + sender);
    }

    private void handleMessage(String message) throws ClassNotFoundException, IOException {
        if (!message.contains("&"))
            throw new IllegalArgumentException("Invalid Message: " + message);
        String[] messageParts = message.split("&");
        Participant sender = getParticipant(messageParts[0]);
        switch (messageParts[1]) {
        case "Init":
            setModel(messageParts[2]);
            break;
        case "Share":
            setShare(messageParts[2], sender);
            break;
        case "Result":
            bufferResultShare(messageParts[2], sender);
            break;
        case "DoneInit":
            app.allClientsInitialized.countDown();
            break;
        case "NewCalcShares":

            break;
        case "AckInit":
            app.allClientsInitialized.countDown();
            break;
        }
        System.out.println("Got Message from " + sender + " saying: " + messageParts[1]);
    }
}