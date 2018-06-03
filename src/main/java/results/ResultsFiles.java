package results;

import java.text.DecimalFormat;

public class ResultsFiles {

    private static WriteFile generalFile;

    public ResultsFiles(String fileName, String factors) {
        try {
            generalFile = new WriteFile("1-general-" + fileName, factors);
             } catch (Exception e) {
        }
    }

    public static void printGeneralResults(Results results) {

        DecimalFormat df = new DecimalFormat("0.0000");
        DecimalFormat df2 = new DecimalFormat("0.00");


    }
}
