package de.tu_darmstadt.cbs.covidapp;

import de.tu_darmstadt.cbs.emailsmpc.Participant;
import java.util.List;
import java.io.File;

public class Configuration {
    public String studyName;
    public boolean host;
    public boolean periodicRebuild;
    public long periodicRebuildSeconds;
    public int restPort;
    public int internalPort;
    public String restAddressPrefix;
    public File dataFile;
    public File trustStore;
    public String trustStorePwd;
    public File keyStore;
    public String keyStorePwd;
    public CovidParticipant[] participants;
    public String[] binNames;
    public char restDelimiter;

    @Override
    public String toString() {
        ArrayPrinter<String> binPrinter = new ArrayPrinter<>();
        ArrayPrinter<CovidParticipant> partPrinter = new ArrayPrinter<>();
        return "studyName: " + studyName + "\n" + "host: " + host + "\n" + "periodicRebuild: " + periodicRebuild + "\n"
                + "periodicRebuildSeconds: " + periodicRebuildSeconds + "\n" + "dataFile: " + dataFile.toString() + "\n"
                + "trustStore: " + trustStore.toString() + "\n" + "participants: " + partPrinter.toString(participants)
                + "\n" + "binNames: " + binPrinter.toString(binNames) + "\n" + "restDelimiter: " + restDelimiter;
    }
}
