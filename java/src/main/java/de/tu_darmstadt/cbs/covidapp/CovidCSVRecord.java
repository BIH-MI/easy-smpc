package de.tu_darmstadt.cbs.covidapp;

import static java.util.Map.entry;
import com.opencsv.CSVReader;
import com.opencsv.bean.CsvBindByName;
import java.util.HashMap;
import java.util.Map;

public class CovidCSVRecord {

    @CsvBindByName(column = "Blutgruppe_ID", required = true)
    private int bloodID;

    public void setBloodID(int id) throws IllegalArgumentException {
        if (id < 1 || id > 8)
            throw new IllegalArgumentException("Invalid Blood Type ID (1-8)");
        this.bloodID = id;
    }

    public int getBloodID() {
        return this.bloodID;
    }

    public String getBloodType() {
        return CovidCSVRecord.bloodIDToGroupName.get(this.bloodID);
    }

    private static final Map<Integer, String> bloodIDToGroupName = Map.ofEntries(entry(1, "0+"), entry(2, "0-"),
            entry(3, "A+"), entry(4, "A-"), entry(5, "B+"), entry(6, "B-"), entry(7, "AB+"), entry(8, "AB-"));
}
