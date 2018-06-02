package results;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

public class ResultsFormat {

    private static WriteFile generalFile;
    private static WriteFile histogramLinkUtilizationFile;

    private static WriteFile linkUtilizationFile;
    private static WriteFile serverUtilizationFile;

    private static WriteFile routingSP;
    private static WriteFile routingSPR;

    private static WriteFile functionXSV;
    private static WriteFile functionXSVR;

    private static WriteFile costs;
    private static WriteFile reliability;

    private static List<String> sortedLinksPerUtilization;
    private static List<String> sortedServersPerUtilization;

    public ResultsFormat(String fileName, String factors) throws IOException {

        sortedLinksPerUtilization = new ArrayList<>();
        sortedServersPerUtilization = new ArrayList<>();

        generalFile = new WriteFile("1_General_" + fileName, factors);
        generalFile.write(String.format("%-7s %-7s %-7s %-7s %-7s %-7s %-7s %-7s %-7s %-7s %-7s %-7s %-4s %-7s %-7s %-7s %-7s %-7s",
                "avg_LU", "max_LU", "min_LU", "std_LU", "avg_SU", "max_SU", "min_SU", "std_SU", "td", "f", "p", "Cost", "Fn", "avgFNV", "maxFNV", "minFNV", "mgr", "rep"));

        histogramLinkUtilizationFile = new WriteFile("2_Histogram_Link_Utilization_" + fileName, factors);
        histogramLinkUtilizationFile.write("### Histogram - Link Utilization ###");
        DecimalFormat df = new DecimalFormat("0.00");
        for (int i = 0; i < 21; i++)
            histogramLinkUtilizationFile.write("\n" + df.format(Math.round(0.05 + i * 0.05 * 100d) / 100d));

        linkUtilizationFile = new WriteFile("3_Link_Utilization_" + fileName, factors);
        serverUtilizationFile = new WriteFile("4_Server_Utilization_" + fileName, factors);
        routingSP = new WriteFile("5_Routing_SP_" + fileName, factors);
        routingSPR = new WriteFile("6_Routing_SPR_" + fileName, factors);
        functionXSV = new WriteFile("7_Functions_XSV_" + fileName, factors);
        functionXSVR = new WriteFile("8_Functions_XSVR_" + fileName, factors);
        costs = new WriteFile("9_Costs_" + fileName, factors);
        costs.write(String.format("%-6s %-6s %-6s %-6s %-6s", "Total", "Server", "Link", "MGR", "REP"));
        reliability = new WriteFile("10_Reliability_" + fileName, factors);
    }


    public static void initializeUtilizationFiles(Map<String, Double> utilizationResults, boolean isForLinks) {

        DecimalFormat df = new DecimalFormat("0.000");
        Map<String, Double> sortedMap = new LinkedHashMap<>();
        utilizationResults.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));

        StringBuilder titleLine = new StringBuilder();
        for (String s : sortedMap.keySet())
            titleLine.append(s).append("  ");

        StringBuilder results = new StringBuilder();
        for (Double d : sortedMap.values())
            results.append(removeZerosConvertToString(d, df)).append(",");

        if (!isForLinks) {
            serverUtilizationFile.write(titleLine + "\n");
            serverUtilizationFile.write(results + "\n");
            sortedServersPerUtilization.addAll(sortedMap.keySet());
        } else {
            linkUtilizationFile.write(titleLine + "\n");
            linkUtilizationFile.write(results + "\n");
            sortedLinksPerUtilization.addAll(sortedMap.keySet());
        }
    }

    public static void printGeneralResults(List<Double> lu, List<Double> su, double td, double tl, double avgPathLength, double cost, int fn, List<Integer> fnv, int numberOfMigrations, int numberOfReplications) {

        DecimalFormat df = new DecimalFormat("0.0000");
        DecimalFormat df2 = new DecimalFormat("0.00");

        double avgLU = utilizationAvg(lu);
        double varianceLU = utilizationVariance(lu, avgLU);

        double avgSU;
        double varianceSU;
        if (su != null) {
            avgSU = utilizationAvg(su);
            varianceSU = utilizationVariance(su, avgSU);
        } else {
            avgSU = 0;
            varianceSU = 0;
            su = new ArrayList<>();
            su.add(0.0);
        }

        generalFile.write(String.format("\n%-7s %-7s %-7s %-7s %-7s %-7s %-7s %-7s %-7s %-7s %-7s %-7s %-7s %-7s %-7s",
                df.format(avgLU), df.format(mostUsed(lu)), df.format(lessUsed(lu)), df.format(Math.sqrt(varianceLU)),
                df.format(avgSU), df.format(mostUsed(su)), df.format(lessUsed(su)), df.format(Math.sqrt(varianceSU)),
                df2.format(td), df2.format(tl), df2.format(avgPathLength), df.format(cost), getVNFresutls(fn, fnv), numberOfMigrations, numberOfReplications));
    }

    public static void printHistogramLinkUtilization(List<Double> utlz) {

        LinkedHashMap<Double, Integer> mapUtilization = new LinkedHashMap<>();
        double gap = 0.05;
        for (int i = 0; i < 21; i++)
            mapUtilization.put(Math.round(gap + i * gap * 100d) / 100d, 0);

        for (Double anUtlz : utlz) {
            for (Object o : mapUtilization.entrySet()) {
                Map.Entry thisEntry = (Map.Entry) o;
                double key = (double) thisEntry.getKey();
                int value = (int) thisEntry.getValue();
                if (anUtlz >= key && anUtlz < Math.round((key + gap) * 10000.0) / 10000.0)
                    thisEntry.setValue(value + 1);
            }
        }
        DecimalFormat df = new DecimalFormat("0.000");
        List<String> stringUtilization = new ArrayList<>();
        for (Object o : mapUtilization.entrySet()) {
            Map.Entry thisEntry = (Map.Entry) o;
            int value = (int) thisEntry.getValue();
            stringUtilization.add(" " + removeZerosConvertToString((double) value / utlz.size(), df));
        }

        histogramLinkUtilizationFile.append(stringUtilization);
    }

    public static void printLinkUtilization(Map<String, Double> utilizationResults) {

        DecimalFormat df = new DecimalFormat("0.000");
        StringBuilder results = new StringBuilder();
        for (String aSortedLinksPerUtilization : sortedLinksPerUtilization)
            results.append(removeZerosConvertToString(utilizationResults.get(aSortedLinksPerUtilization), df)).append(", ");
        linkUtilizationFile.write(results + "\n");
    }

    public static void printServerUtilization(Map<String, Double> utilizationResults) {

        DecimalFormat df = new DecimalFormat("0.000");
        StringBuilder results = new StringBuilder();
        for (String aSortedServersPerUtilization : sortedServersPerUtilization)
            results.append(removeZerosConvertToString(utilizationResults.get(aSortedServersPerUtilization), df)).append(", ");
        serverUtilizationFile.write(results + "\n");
    }

    public static void printRoutingSP(List<String> results) {
        routingSP.write("##############");
        printListToFile(results, routingSP);
    }

    public static void printRoutingSPR(List<String> results) {
        routingSPR.write("##############");
        printListToFile(results, routingSPR);
    }

    public static void printFunctionXSV(List<String> results) {
        functionXSV.write("##############");
        printListToFile(results, functionXSV);
    }

    public static void printFunctionsXSVR(List<String> results) {
        functionXSVR.write("##############");
        printListToFile(results, functionXSVR);
    }

    public static void printCosts(String results) {
        costs.write("\n" + results);
    }

    public static void printReliability(String results) {
        reliability.write(results);
    }


    private static void printListToFile(List<String> list, WriteFile file) {
        for (String s : list)
            file.write("\n" + s);
    }

    private static String removeZerosConvertToString(Double value, DecimalFormat df) {
        String stringValue;
        if ((value + 0.00) == 0.00)
            stringValue = "-.---";
        else
            stringValue = String.valueOf(df.format(value));
        return stringValue;
    }


    private static double utilizationAvg(List<Double> utilizationResults) {
        double tmpU = 0;
        for (Double utilizationResult : utilizationResults) tmpU += utilizationResult;
        tmpU = tmpU / utilizationResults.size();
        return tmpU;
    }

    private static double utilizationVariance(List<Double> utilizationResults, double avg) {
        double variance = 0;
        for (Double utilizationResult : utilizationResults)
            variance += Math.pow(utilizationResult - avg, 2);

        variance = variance / utilizationResults.size();
        return variance;
    }

    private static double mostUsed(List<Double> utilizationResults) {
        double max = 0;
        for (Double utilizationResult : utilizationResults)
            if (utilizationResult > max)
                max = utilizationResult;
        return max;
    }

    private static double lessUsed(List<Double> utilizationResults) {
        double min = Double.MAX_VALUE;
        for (Double utilizationResult : utilizationResults)
            if (utilizationResult < min)
                min = utilizationResult;
        return min;
    }

    private static String getVNFresutls(int fn, List<Integer> fnv) {

        double avgFnv = 0;
        int maxFnv = 0;
        int minFnv = 0;
        if (fnv != null) {
            minFnv = Integer.MAX_VALUE;
            for (int i : fnv) {
                avgFnv += i;
                if (i < minFnv)
                    minFnv = i;
                else if (i > maxFnv)
                    maxFnv = i;
            }
            avgFnv = avgFnv / fnv.size();
        }
        DecimalFormat df = new DecimalFormat("00.000");
        NumberFormat formatter = new DecimalFormat("00");
        return String.format("%-4s %-7s %-7s %-7s", formatter.format(fn), df.format(avgFnv), formatter.format(maxFnv), formatter.format(minFnv));
    }
}
