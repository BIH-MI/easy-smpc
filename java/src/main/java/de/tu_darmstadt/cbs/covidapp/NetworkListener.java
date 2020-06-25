package de.tu_darmstadt.cbs.covidapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;

public class NetworkListener implements Runnable {
    private final int port;
    private SSLServerSocket serverSocket;
    private final CovidApp app;

    public NetworkListener(CovidApp app, Configuration config)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException,
            IOException, UnrecoverableKeyException, KeyManagementException {
        port = config.internalPort;
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
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        SSLContext.setDefault(sslContext);
        SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();
        this.serverSocket = null;
        this.serverSocket = (SSLServerSocket) ssf.createServerSocket(port);
        this.serverSocket.setNeedClientAuth(true);
        this.app = app;
        System.out.println("Listening on port " + port + " for connection");
    }

    public void run() {
        for (;;) {
            SSLSocket clientSocket = null;
            try {
                clientSocket = (SSLSocket) serverSocket.accept();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            new Thread(new ConnectionHandler(clientSocket, app)).start();
        }
    }
}
