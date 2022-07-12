package optimizer.results;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBModel;
import manager.Parameters;
import optimizer.gui.Scenario;
import optimizer.lp.VariablesLP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static optimizer.Definitions.*;
import static optimizer.results.Auxiliary.printLog;

public class ResultsManager {

   private static final Logger log = LoggerFactory.getLogger(ResultsManager.class);
   private String resultsFolder;

   public ResultsManager(String folderName) {
      SimpleDateFormat MY_FORMAT = new SimpleDateFormat("dd-MM-yy_HH-mm-ss_", Locale.getDefault());
      Date date = new Date();
      String path = ResultsManager.class.getProtectionDomain().getCodeSource().getLocation().getPath();
      File f = new File(path);
      if (!f.isDirectory())
         path = f.getParent()+ "/results/";
      else 
         path = path + "../results/";
      path = path.replaceAll("%20", " ");
      File parentDirectory = new File(path);
      if (!parentDirectory.exists())
         parentDirectory.mkdir();
      resultsFolder = path + MY_FORMAT.format(date) + folderName;
      new File(resultsFolder).mkdir();
   }

   public void exportJsonObject(String fileName, Object object) {
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

   public String importConfDrlFile(String fileName) {
      String path = Auxiliary.getResourcesPath(fileName + ".json");
      ObjectMapper objectMapper = new ObjectMapper();
      String conf = null;
      try {
         File file = new File(path + fileName + ".json");
         conf = objectMapper.readValue(file, String.class);
      } catch (Exception e) {
         Auxiliary.printLog(log, WARNING, "no drl conf. file found");
      }
      return conf;
   }

   public GRBModel loadInitialPlacement(String filename, Parameters pm, Scenario sce) throws GRBException {
      GRBModel model = loadModel(filename, pm, sce, true);
      if (model != null) {
         Auxiliary.printLog(log, INFO, "initial placement loaded");
         return model;
      } else {
         Auxiliary.printLog(log, WARNING, "no initial placement");
         return null;
      }
   }

   public GRBModel loadModel(String pathFile, Parameters pm, Scenario sce, boolean isInitialPlacement) {
      GRBModel model;
      try {
         File file = new File(pathFile + ".mst");
         if (!file.exists()) {
            if (!isInitialPlacement)
               printLog(log, WARNING, "no initial solution found");
            return null;
         }
         GRBEnv grbEnv = new GRBEnv();
         if (isInitialPlacement)
            grbEnv.set(GRB.IntParam.LogToConsole, 0);
         model = new GRBModel(grbEnv);
         new VariablesLP(pm, model, sce, null);
         model.read(pathFile + ".mst");
         model.optimize();
         if (!isInitialPlacement)
            Auxiliary.printLog(log, INFO, "initial solution loaded");
         return model;
      } catch (Exception e) {
         printLog(log, ERROR, e.getMessage());
         return null;
      }
   }

   public void exportModel(GRBModel model, String fileName) {
      try {
         model.write(resultsFolder + "/" + fileName + ".mst");
      } catch (Exception e) {
         Auxiliary.printLog(log, ERROR, "error while exporting model");
      }
   }

   public PrintWriter getPrinterFromPlainTextFile(String fileName, String extension) {
      try {
         FileWriter fw = new FileWriter(resultsFolder + "/" + fileName + extension, true);
         BufferedWriter bw = new BufferedWriter(fw);
         return new PrintWriter(bw);
      } catch (IOException e) {
         e.printStackTrace();
      }
      return null;
   }

   public String getResultsFolder() {
      return resultsFolder;
   }

   public void setResultsFolder(String resultsFolder) {
      this.resultsFolder = resultsFolder;
   }
}
