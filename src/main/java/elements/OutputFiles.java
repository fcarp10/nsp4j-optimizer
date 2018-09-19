package elements;

import results.Results;

public class OutputFiles {

    private FileWriter fileWriter;
    private String fileName;

    public OutputFiles(String fileName, String factors) {
        this.fileName = fileName;
        try {
            fileWriter = new FileWriter(factors);
            fileWriter.initializeTextPlainFile("_summary_" + fileName);
            fileWriter.writeTextPlain(String.format("%-7s %-7s %-7s %-7s %-7s %-7s %-7s %-7s " +
                            "%-7s %-7s %-7s %-7s %-7s %-7s %-7s %-7s"
                    , "avg-lu", "max-lu", "min-lu", "std-lu", "avg-su", "max-su", "min-su", "std-su"
                    , "avg-f", "max-f", "min-f", "std-f", "avg-p", "mgr", "rep", "cost"));
        } catch (Exception ignored) {
        }
    }

    public void printSummary(Results r) {
        fileWriter.writeTextPlain(String.format("\n%-7s %-7s %-7s %-7s %-7s %-7s %-7s %-7s " +
                        "%-7s %-7s %-7s %-7s %-7s %-7s %-7s %-7s",
                r.getAvgLu(), r.getMinLu(), r.getMaxLu(), r.getVrcLu(),
                r.getAvgXu(), r.getMinXu(), r.getMaxXu(), r.getVrcXu(),
                r.getAvgFu(), r.getMinFu(), r.getMaxFu(), r.getVrcFu(),
                r.getAvgPathLength(), r.getNumOfMigrations(), r.getNumOfReplicas(), r.getCost()));
    }

    public void print(Results results, String useCase) {
        fileWriter.createJsonForResults(fileName + "_" + useCase, results);
    }
}
