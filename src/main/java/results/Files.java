package results;

public class Files {

    public Writer writer;
    private String fileName;

    public Files(String fileName, String factors) {
        this.fileName = fileName;
        try {
            writer = new Writer(factors);
            writer.initializeTextPlainFile("_summary_" + fileName);
            writer.writeTextPlain(String.format("%-7s %-7s %-7s %-7s %-7s %-7s %-7s %-7s " +
                            "%-7s %-7s %-7s %-7s %-7s %-7s %-7s %-7s"
                    , "avg-lu", "max-lu", "min-lu", "std-lu", "avg-su", "max-su", "min-su", "std-su"
                    , "avg-f", "max-f", "min-f", "std-f", "avg-p", "mgr", "rep", "cost"));
        } catch (Exception ignored) {
        }
    }

    public void printSummary(Results r) {
        writer.writeTextPlain(String.format("\n%-7s %-7s %-7s %-7s %-7s %-7s %-7s %-7s " +
                        "%-7s %-7s %-7s %-7s %-7s %-7s %-7s %-7s",
                r.getAvgLu(), r.getMinLu(), r.getMaxLu(), r.getVrcLu(),
                r.getAvgXu(), r.getMinXu(), r.getMaxXu(), r.getVrcXu(),
                r.getAvgFu(), r.getMinFu(), r.getMaxFu(), r.getVrcFu(),
                r.getAvgPathLength(), r.getNumOfMigrations(), r.getNumOfReplicas(), r.getCost()));
    }

    public void print(Results results, String useCase) {
        writer.createJsonForResults(fileName + "_" + useCase, results);
    }
}
