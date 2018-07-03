package results;

import com.google.gson.Gson;

public class ResultFiles {

    public WriteFile summaryResultsFile;
    public WriteFile resultsFile;

    public ResultFiles(String fileName, String factors) {
        try {
            summaryResultsFile = new WriteFile("summary_results_" + fileName, factors);
            summaryResultsFile.initializeTextPlainFile();
            summaryResultsFile.writeTextPlain(String.format("%-7s %-7s %-7s %-7s %-7s %-7s %-7s %-7s " +
                            "%-7s %-7s %-7s %-7s %-7s %-7s %-7s %-7s"
                    , "avg-lu", "max-lu", "min-lu", "std-lu", "avg-su", "max-su", "min-su", "std-su"
                    , "avg-f", "max-f", "min-f", "std-f", "avg-p", "mgr", "rep", "cost"));
            resultsFile = new WriteFile("results_" + fileName, factors);
            resultsFile.initializeJsonFile();
        } catch (Exception ignored) {
        }
    }

    public void printSummary(Results r) {
        summaryResultsFile.writeTextPlain(String.format("\n%-7s %-7s %-7s %-7s %-7s %-7s %-7s %-7s " +
                        "%-7s %-7s %-7s %-7s %-7s %-7s %-7s %-7s",
                r.getAvgLu(), r.getMinLu(), r.getMaxLu(), r.getVrcLu(),
                r.getAvgXu(), r.getMinXu(), r.getMaxXu(), r.getVrcXu(),
                r.getAvgFu(), r.getMinFu(), r.getMaxFu(), r.getVrcFu(),
                r.getAvgPathLength(), r.getNumOfMigrations(), r.getNumOfReplicas(), r.getCost()));
    }

    public void print(Results results) {
        Gson g = new Gson();
        String jsonResults = g.toJson(results);
        resultsFile.writeJson(jsonResults);
    }
}
