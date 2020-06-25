package de.tu_darmstadt.cbs.covidapp;

import java.io.Serializable;

public class Endpoint implements Serializable {
    public String ipv4Address;
    public int port;
    private static final long serialVersionUID = 4899732617934200945L;

    public Endpoint(String ip, int port) throws IllegalArgumentException {
        if (Endpoint.validatePort(port) && Endpoint.validateIp(ip)) {
            this.ipv4Address = ip;
            this.port = port;
        } else {
            throw new IllegalArgumentException("Invalid IP or Port: " + ip + ":" + port);
        }
    }

    public String getIpv4() {
        return ipv4Address;
    }

    public int getPort() {
        return port;
    }

    public static boolean validatePort(final int port) {
        // TODO: Maybe forbid IANA-Restricted ports?
        return (port > 0) && (port <= 65535);
    }

    // from https://stackoverflow.com/a/30691451
    public static boolean validateIp(final String ip) {
        String PATTERN = "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";
        return ip.matches(PATTERN);
    }

    @Override
    public String toString() {
        return ipv4Address + ":" + port;
    }

    // String form: ip:port, e.g. 127.0.0.1:1337
    public static Endpoint fromString(String address) throws IllegalArgumentException {
        String[] parts = address.split(":");
        if (parts.length != 2)
            throw new IllegalArgumentException("Invalid Endpoint string: " + address);
        return new Endpoint(parts[0], Short.valueOf(parts[1]));
    }

    @Override
    public int hashCode() {
        return 31 * ipv4Address.hashCode() + port;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Endpoint))
            return false;
        Endpoint ep = (Endpoint) o;
        return ep.ipv4Address.equals(ipv4Address) && (ep.port == port);
    }
}
