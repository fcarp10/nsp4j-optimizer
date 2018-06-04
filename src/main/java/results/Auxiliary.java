package results;


import org.decimal4j.util.DoubleRounder;

import java.util.List;

public class Auxiliary {

    public static final int DECIMALS = 2;

    public static double avg(List<Double> utilizationResults) {
        double tmpU = 0;
        for (Double utilizationResult : utilizationResults) tmpU += utilizationResult;
        tmpU = tmpU / utilizationResults.size();
        return roundDouble(tmpU);
    }

    public static double vrc(List<Double> utilizationResults, double avg) {
        double variance = 0;
        for (Double utilizationResult : utilizationResults)
            variance += Math.pow(utilizationResult - avg, 2);
        variance = variance / utilizationResults.size();
        return roundDouble(variance);
    }

    public static double max(List<Double> utilizationResults) {
        double max = 0;
        for (Double utilizationResult : utilizationResults)
            if (utilizationResult > max)
                max = utilizationResult;
        return roundDouble(max);
    }

    public static double min(List<Double> utilizationResults) {
        double min = Double.MAX_VALUE;
        for (Double utilizationResult : utilizationResults)
            if (utilizationResult < min)
                min = utilizationResult;
        return roundDouble(min);
    }

    public static double roundDouble(double value){
        return DoubleRounder.round(value, DECIMALS);
    }
}
