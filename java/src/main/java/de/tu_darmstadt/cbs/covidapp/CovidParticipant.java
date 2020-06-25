package de.tu_darmstadt.cbs.covidapp;

import de.tu_darmstadt.cbs.emailsmpc.Participant;
import java.io.Serializable;

public class CovidParticipant extends Participant implements Serializable {
    public final Endpoint endpoint;
    private static final long serialVersionUID = 2526398315542120875L;

    public CovidParticipant(String name, String address, String endpoint) {
        super(name, address);
        this.endpoint = Endpoint.fromString(endpoint);
    }

    @Override
    public String toString() {
        String result = super.toString();
        result += ", " + endpoint;
        return result;
    }
}
