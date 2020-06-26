package de.tu_darmstadt.cbs.covidapp;

import net.freeutils.httpserver.HTTPServer;
import net.freeutils.httpserver.HTTPServer.VirtualHost;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class RestServer {
    private int port;
    private File keyStore;
    private String keyStorePwd;
    private HTTPServer server;
    private SSLContext context;
    private RestHistogramEndpoint endpoints;

    public RestServer(CovidApp app, Configuration config)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException,
            IOException, UnrecoverableKeyException, KeyManagementException {
        port = config.restPort;
        server = new HTTPServer(port);

        VirtualHost host = server.getVirtualHost(null); // default virtual host
        host.addContext(config.restAddressPrefix + "histogram", new RestHistogramEndpoint(app), "GET");
        if (config.host && config.periodicRebuild == false)
        	host.addContext(config.restAddressPrefix + "recalculate", new RestRecalculateEndpoint(app), "GET");
        //KeyStore ks = KeyStore.getInstance("jks");
        //ks.load(new FileInputStream(config.keyStore), config.keyStorePwd.toCharArray());
        //KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        //kmf.init(ks, config.keyStorePwd.toCharArray());
        //SSLContext sslContext = SSLContext.getInstance("TLS");
        //sslContext.init(kmf.getKeyManagers(), null, null);
        server.setServerSocketFactory(ServerSocketFactory.getDefault());
    }

    public void run() throws IOException {
        server.start();
    }

    public void stop() {
        server.stop();
    }

}
