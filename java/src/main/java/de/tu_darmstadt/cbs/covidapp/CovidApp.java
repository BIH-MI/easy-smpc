package de.tu_darmstadt.cbs.covidapp;
public class CovidApp {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java -jar covidApp.jar <configfile.toml>");
            System.exit(-1);
        }
    }
}
