package utils;


import org.decimal4j.util.DoubleRounder;

import java.util.ArrayList;
import java.util.List;

public class Auxiliary {

    public static double avg(List<Double> utilizationResults) {
        double tmpU = 0;
        for (Double utilizationResult : utilizationResults) tmpU += utilizationResult;
        tmpU = tmpU / utilizationResults.size();
        return roundDouble(tmpU, 2);
    }

    public static double vrc(List<Double> utilizationResults, double avg) {
        double variance = 0;
        for (Double utilizationResult : utilizationResults)
            variance += Math.pow(utilizationResult - avg, 2);
        variance = variance / utilizationResults.size();
        return roundDouble(variance, 2);
    }

    public static double max(List<Double> utilizationResults) {
        double max = 0;
        for (Double utilizationResult : utilizationResults)
            if (utilizationResult > max)
                max = utilizationResult;
        return roundDouble(max, 2);
    }

    public static double min(List<Double> utilizationResults) {
        double min = Double.MAX_VALUE;
        for (Double utilizationResult : utilizationResults)
            if (utilizationResult < min)
                min = utilizationResult;
        return roundDouble(min, 2);
    }

    public static List<Integer> listsSizes(List<List<Integer>> list) {
        List<Integer> listsSizes = new ArrayList<>();
        for (List<Integer> aList : list)
            listsSizes.add(aList.size());
        return listsSizes;
    }

    public static double avgF(List<Integer> results) {
        double tmp = 0;
        for (Integer i : results) tmp += i;
        tmp = tmp / results.size();
        return roundDouble(tmp, 2);
    }

    public static double vrcF(List<Integer> results, double avg) {
        double variance = 0;
        for (Integer i : results)
            variance += Math.pow(i - avg, 2);
        variance = variance / results.size();
        return roundDouble(variance, 2);
    }

    public static int maxF(List<Integer> results) {
        int max = 0;
        for (Integer i : results)
            if (i > max)
                max = i;
        return max;
    }

    public static int minF(List<Integer> results) {
        int min = Integer.MAX_VALUE;
        for (Integer i : results)
            if (i < min)
                min = i;
        return min;
    }

    public static double roundDouble(double value, int decimals) {
        return DoubleRounder.round(value, decimals);
    }
}
