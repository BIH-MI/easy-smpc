package de.tu_darmstadt.cbs.covidapp;

import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;
import org.tomlj.TomlArray;
import org.tomlj.Toml;
import java.nio.file.Path;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import de.tu_darmstadt.cbs.emailsmpc.Participant;

public class TOMLConfigReader {
    public static Configuration getConfig(File configFile) {
        Configuration config = new Configuration();
        try {
            TomlParseResult toml = Toml.parse(configFile.toPath());
            if (toml.hasErrors())
                toml.errors().forEach(error -> System.err.println(error.toString()));
            config.studyName = toml.getString("study_name");
            config.host = toml.getBoolean("host");
            config.periodicRebuild = toml.getBoolean("periodic_rebuild");
            if (config.periodicRebuild == true) {
                config.periodicRebuildSeconds = toml.getLong("periodic_rebuild_seconds");
            } else {
                config.periodicRebuildSeconds = 0;
            }
            config.dataFile = new File(toml.getString("data_file"));
            config.trustStore = new File(toml.getString("trust_store"));
            config.keyStore = new File(toml.getString("key_store"));
            config.trustStorePwd = toml.getString("trust_store_pwd");
            config.keyStorePwd = toml.getString("key_store_pwd");
            config.restPort = toml.getLong("rest_port").intValue();
            config.internalPort = toml.getLong("internal_port").intValue();
            if (Endpoint.validatePort(config.restPort) == false)
                throw new IllegalStateException("Invalid rest port");
            config.restAddressPrefix = toml.getString("rest_address_prefix");
            config.participants = getParticipants(toml);
            config.binNames = getBinNames(toml);
            String delim = toml.getString("rest_delimiter");
            if (delim != null) {
                config.restDelimiter = delim.charAt(0);
            } else {
                config.restDelimiter = '&';
            }

        } catch (IllegalStateException e) {
            System.err.println("An Error occured parsing the config: " + e);
        } catch (IOException e) {
            System.err.println("An Error occured parsing the config: " + e);
        }
        return config;
    }

    private static CovidParticipant[] getParticipants(TomlParseResult toml) throws IllegalStateException {
        TomlArray participants = toml.getArray("participants");
        CovidParticipant[] result = new CovidParticipant[participants.size()];
        for (int i = 0; i < participants.size(); i++) {
            TomlTable p = participants.getTable(i);
            String name = p.getString("name");
            String email = p.getString("email_address");
            String endpoint = p.getString("endpoint");
            if (name == null || email == null || endpoint == null)
                throw new IllegalStateException("Invalid Participant in TOML");
            result[i] = new CovidParticipant(name, email, endpoint);
        }
        return result;
    }

    private static String[] getBinNames(TomlParseResult toml) {
        TomlArray binNames = toml.getArray("bin_names");
        String[] result = new String[binNames.size()];
        for (int i = 0; i < binNames.size(); i++) {
            result[i] = binNames.getString(i);
        }
        return result;
    }

}
