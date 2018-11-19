package output;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBModel;
import lp.Variables;
import manager.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static output.Auxiliary.INFO;

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
      GRBModel model = null;
      try {
         GRBEnv grbEnv = new GRBEnv();
         grbEnv.set(GRB.IntParam.LogToConsole, 0);
         model = new GRBModel(grbEnv);
         new Variables(pm, model);
         model.read(path + filename + "_initial_placement.mst");
         model.optimize();
         Auxiliary.printLog(log, INFO, "initial placement loaded");
      } catch (Exception e) {
         Auxiliary.printLog(log, INFO, "no initial placement found");
      }
      return model;
   }

   public void exportModel(GRBModel model, String fileName) {
      try {
         model.write(resultsFolder + "/" + fileName + "_initial_placement.mst");
      } catch (Exception e) {
      }
   }
}