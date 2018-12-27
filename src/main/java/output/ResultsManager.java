package output;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import gurobi.*;
import lp.Variables;
import manager.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static output.Definitions.*;

public class ResultsManager {

   private static final Logger log = LoggerFactory.getLogger(ResultsManager.class);
   private String resultsFolder;

   public ResultsManager(String folderName) {
      SimpleDateFormat MY_FORMAT = new SimpleDateFormat(
              "dd-MM-yy_HH-mm-ss_", Locale.getDefault());
      Date date = new Date();
      String path = ResultsManager.class.getProtectionDomain().getCodeSource().getLocation().getPath();
      path = path.replaceAll("%20", " ");
      File parentDirectory = new File(path + "/../results");
      if (!parentDirectory.exists())
         parentDirectory.mkdir();
      resultsFolder = path + "/../results/" + MY_FORMAT.format(date) + folderName;
      new File(resultsFolder).mkdir();
   }

   public void exportJsonFile(String fileName, Object object) {
      File jsonFile = new File(resultsFolder + "/" + fileName + ".json");
      ObjectMapper mapper = new ObjectMapper(new JsonFactory());
      DefaultPrettyPrinter.Indenter indenter = new DefaultIndenter("  ", DefaultIndenter.SYS_LF);
      DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
      printer.indentObjectsWith(indenter);
      printer.indentArraysWith(indenter);
      try {
         mapper.writer(printer).writeValue(jsonFile, object);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public static GRBModel importModel(String path, String filename, Parameters pm) {
      path = path.replaceAll("%20", " ");
      GRBModel model;
      try {
         GRBEnv grbEnv = new GRBEnv();
         grbEnv.set(GRB.IntParam.LogToConsole, 0);
         model = new GRBModel(grbEnv);
         new Variables(pm, model);
         model.read(path + filename + "_initial_placement.mst");
         model.optimize();
         Auxiliary.printLog(log, INFO, "initial placement loaded");
         return model;
      } catch (Exception e) {
         Auxiliary.printLog(log, INFO, "no initial placement found");
         return null;
      }
   }

   public void exportModel(GRBModel model, String fileName) {
      try {
         model.write(resultsFolder + "/" + fileName + "_initial_placement.mst");
      } catch (Exception e) {
      }
   }

   public void exportTextFile(String fileName, Results results) {
      File textFile = new File(resultsFolder + "/" + fileName + ".txt");
      FileWriter filewriter;
      try {
         filewriter = new FileWriter(textFile, false);
         PrintWriter printer = new PrintWriter(filewriter);
         String caseName = null;
         if (String.valueOf(results.getScenario().getWeights()).equals("1.0-0.0-0.0"))
            caseName = "LLB";
         if (String.valueOf(results.getScenario().getWeights()).equals("0.0-1.0-0.0"))
            caseName = "SLB";
         if (String.valueOf(results.getScenario().getWeights()).equals("0.0-0.0-1.0"))
            caseName = "SD";
         String partialVarName;
         if (caseName != null)
            partialVarName = fileName + "_" + caseName;
         else partialVarName = fileName;
         // Link utilization
         double[] uLvar = (double[]) results.getRawVariables().get(uL);
         StringBuilder uL = new StringBuilder("lu_" + partialVarName + " = [");
         for (int l = 0; l < results.getPm().getLinks().size(); l++)
            uL.append(Auxiliary.roundDouble(uLvar[l], 3)).append(", ");
         uL = new StringBuilder(uL.substring(0, uL.length() - 2));
         uL.append("]\n");
         // Server utilization
         double[] uXvar = (double[]) results.getRawVariables().get(uX);
         StringBuilder uX = new StringBuilder("xu_" + partialVarName + " = [");
         for (int x = 0; x < results.getPm().getServers().size(); x++)
            uX.append(Auxiliary.roundDouble(uXvar[x], 3)).append(", ");
         uX = new StringBuilder(uX.substring(0, uX.length() - 2));
         uX.append("]\n");
         // Function Placement
         List<Integer> functionsPerServer = results.numOfFunctionsPerServer();
         double max = Auxiliary.max(new ArrayList<>(functionsPerServer));
         StringBuilder placement = new StringBuilder("fp_" + partialVarName + " = [");
         for (Integer integer : functionsPerServer) placement.append(integer / max).append(", ");
         placement = new StringBuilder(placement.substring(0, placement.length() - 2));
         placement.append("]\n");
         // Service Delay
         List<Double> serviceDelayList = results.serviceDelayList();
         max = Auxiliary.max(new ArrayList<>(serviceDelayList));
         StringBuilder serviceDelay = new StringBuilder("sd_" + partialVarName + " = [");
         for (Double aDouble : serviceDelayList) serviceDelay.append(aDouble / max).append(", ");
         serviceDelay = new StringBuilder(serviceDelay.substring(0, serviceDelay.length() - 2));
         serviceDelay.append("]\n");
         printer.write(String.valueOf(uL));
         printer.write(String.valueOf(uX));
         printer.write(String.valueOf(placement));
         printer.write(String.valueOf(serviceDelay));
         printer.close();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}
