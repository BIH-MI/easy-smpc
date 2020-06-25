package de.tu_darmstadt.cbs.covidapp;

public class ArrayPrinter<T> {
    public String toString(T[] array) {
        String result = "[";
        for (T e : array) {
            result += e + ", ";
        }
        return result.substring(0, result.length() - 2) + "]";
    }
}
