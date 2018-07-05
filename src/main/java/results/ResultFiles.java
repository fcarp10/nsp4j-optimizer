package results;

public class ResultFiles {

    public WriteFile writeFile;
    private String fileName;

    public ResultFiles(String fileName, String factors) {
        this.fileName = fileName;
        try {
            writeFile = new WriteFile(factors);
            writeFile.initializeTextPlainFile("_summary_" + fileName);
            writeFile.writeTextPlain(String.format("%-7s %-7s %-7s %-7s %-7s %-7s %-7s %-7s " +
                            "%-7s %-7s %-7s %-7s %-7s %-7s %-7s %-7s"
                    , "avg-lu", "max-lu", "min-lu", "std-lu", "avg-su", "max-su", "min-su", "std-su"
                    , "avg-f", "max-f", "min-f", "std-f", "avg-p", "mgr", "rep", "cost"));
        } catch (Exception ignored) {
        }
    }

    public void printSummary(Results r) {
        writeFile.writeTextPlain(String.format("\n%-7s %-7s %-7s %-7s %-7s %-7s %-7s %-7s " +
                        "%-7s %-7s %-7s %-7s %-7s %-7s %-7s %-7s",
                r.getAvgLu(), r.getMinLu(), r.getMaxLu(), r.getVrcLu(),
                r.getAvgXu(), r.getMinXu(), r.getMaxXu(), r.getVrcXu(),
                r.getAvgFu(), r.getMinFu(), r.getMaxFu(), r.getVrcFu(),
                r.getAvgPathLength(), r.getNumOfMigrations(), r.getNumOfReplicas(), r.getCost()));
    }

    public void print(Results results, String useCase) {
        writeFile.createJsonForResults(fileName + "_" + useCase, results);
    }
}
