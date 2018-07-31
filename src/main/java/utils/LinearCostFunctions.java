package utils;

import java.util.LinkedList;

public class LinearCostFunctions {

    private LinkedList<Double[]> values = new LinkedList<>();

    public LinearCostFunctions() {
    }

    public LinkedList<Double[]> getValues() {
        return values;
    }

    public void setValues(LinkedList<Double[]> values) {
        this.values = values;
    }
}
