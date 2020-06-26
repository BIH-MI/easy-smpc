/**
 * 
 */
package de.tu_darmstadt.cbs.covidapp;

import de.tu_darmstadt.cbs.emailsmpc.Participant;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Base64;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import de.tu_darmstadt.cbs.emailsmpc.Participant;

/**
 * @author kussel
 *
 */
public class NetworkSender {
    private CovidApp app;
    private CovidParticipant self;
    private String senderString;
    private SSLContext sslContext;

    public NetworkSender(CovidApp app, Configuration config, CovidParticipant self)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException,
            IOException, UnrecoverableKeyException, KeyManagementException {
        this.app = app;
        this.self = self;
        System.out.println("Self: " + self);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        Participant tempParticipant = new Participant(self.name, self.emailAddress);
        oos.writeObject(tempParticipant);
        oos.close();
        senderString = Base64.getEncoder().encodeToString(bos.toByteArray());
        bos.close();
        File keyStore = config.keyStore;
        String keyStorePwd = config.keyStorePwd;
        File trustStore = config.trustStore;
        String trustStorePwd = config.trustStorePwd;
        KeyStore ks = KeyStore.getInstance("jks");
        ks.load(new FileInputStream(keyStore), keyStorePwd.toCharArray());
        KeyStore ts = KeyStore.getInstance("jks");
        ts.load(new FileInputStream(trustStore), trustStorePwd.toCharArray());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, keyStorePwd.toCharArray());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ts);
        this.sslContext = SSLContext.getInstance("TLS");
        this.sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
    }

    private void send(CovidParticipant recipient, String message) throws UnknownHostException, IOException {
        SSLSocketFactory ssf = sslContext.getSocketFactory();
        SSLSocket sslSocket = (SSLSocket) ssf.createSocket(recipient.endpoint.getIpv4(), recipient.endpoint.getPort());
        DataOutputStream os = new DataOutputStream(sslSocket.getOutputStream());
        os.writeBytes(message);
        os.flush();
        sslSocket.close();
    }

    public void sendInitialMessage(CovidParticipant recipient, String message)
            throws UnknownHostException, IOException {
        String networkMessage = senderString + "&Init&" + message;
        send(recipient, networkMessage);
    }

    public void sendShareMessage(CovidParticipant recipient, String message) throws UnknownHostException, IOException {
        String networkMessage = senderString + "&Share&" + message;
        send(recipient, networkMessage);
    }

    public void sendResultMessage(CovidParticipant recipient, String message) throws UnknownHostException, IOException {
        String networkMessage = senderString + "&Result&" + message;
        send(recipient, networkMessage);
    }

    public void sendDoneInitMessage(CovidParticipant recipient) throws UnknownHostException, IOException {
        String networkMessage = senderString + "&DoneInit";
        send(recipient, networkMessage);
    }

    public void sendRecalculateMessage(CovidParticipant recipient)
            throws UnknownHostException, IOException {
        String networkMessage = senderString + "&Recalculate";
        send(recipient, networkMessage);
    }

    public void sendAckInitMessage(CovidParticipant recipient) throws UnknownHostException, IOException {
        String networkMessage = senderString + "&AckInit";
        send(recipient, networkMessage);
    }

}
