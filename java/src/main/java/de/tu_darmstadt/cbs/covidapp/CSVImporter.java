package de.tu_darmstadt.cbs.covidapp;

import java.math.BigInteger;
import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import com.opencsv.CSVReader;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import java.util.List;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

public class CSVImporter implements HistogramImporter {
    public BigInteger[] getCounts() {
        Map<Integer, BigInteger> count = new HashMap<>();
        List<CovidCSVRecord> data = getRecords();
        for (CovidCSVRecord record : data) {
            int id = record.getBloodID();
            count.put(id, count.getOrDefault(id, BigInteger.ZERO).add(BigInteger.ONE));
        }
        BigInteger[] result = new BigInteger[this.binNum];
        for (int i = 0; i < this.binNum; ++i) {
            result[i] = count.getOrDefault(i + 1, BigInteger.ZERO);
        }
        return result;
    }

    public CSVImporter(File dataFile, int binNum) throws IOException {
        this.binNum = binNum;
        this.dataFile = dataFile;
        try {
            FileReader fr = new FileReader(this.dataFile);
            HeaderColumnNameMappingStrategy<CovidCSVRecord> strategy = new HeaderColumnNameMappingStrategy<>();
            strategy.setType(CovidCSVRecord.class);
            this.reader = new CsvToBeanBuilder(fr).withType(CovidCSVRecord.class).withMappingStrategy(strategy)
                    .withIgnoreLeadingWhiteSpace(true).build();
        } catch (IOException e) {
            System.err.println("Can not open file: " + e);
        }
    }

    private List<CovidCSVRecord> getRecords() {
        return reader.parse();
    }

    private int binNum;
    private CsvToBean reader;
    private File dataFile;
}
