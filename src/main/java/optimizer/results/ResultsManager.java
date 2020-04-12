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
import optimizer.Manager;
import optimizer.gui.Scenario;
import optimizer.lp.Variables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static optimizer.Definitions.*;


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

   public static boolean[][][] loadInitialPlacement(String filename, Parameters pm, Scenario sce) throws GRBException {
      GRBModel model = loadModel(filename, pm, sce);
      if (model != null) {
         Auxiliary.printLog(log, INFO, "initial placement loaded");
         boolean[][][] fXSVvar = new boolean[pm.getServers().size()][pm.getServices().size()][pm.getServiceLength()];
         for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                  if (model.getVarByName(fXSV + "[" + x + "][" + s + "][" + v + "]").get(GRB.DoubleAttr.X) == 1.0)
                     fXSVvar[x][s][v] = true;
         return fXSVvar;
      } else {
         Auxiliary.printLog(log, WARNING, "no initial placement");
         return null;
      }
   }

   public static GRBModel loadModel(String filename, Parameters pm, Scenario sce) {
      String path = getResourcePath(filename + ".mst");
      GRBModel model;
      try {
         GRBEnv grbEnv = new GRBEnv();
         grbEnv.set(GRB.IntParam.LogToConsole, 0);
         model = new GRBModel(grbEnv);
         new Variables(pm, model, sce, null);
         model.read(path + filename + ".mst");
         model.optimize();
         Auxiliary.printLog(log, INFO, "initial model loaded");
         return model;
      } catch (Exception e) {
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

   public static String getResourcePath(String fileName) {
      try {
         File file = new File(Manager.class.getClassLoader().getResource("scenarios/" + fileName).getFile());
         String absolutePath = file.getAbsolutePath();
         String path = absolutePath.substring(0, absolutePath.lastIndexOf(File.separator));
         if (System.getProperty("os.name").equals("Mac OS X") || System.getProperty("os.name").equals("Linux"))
            path = path + "/";
         else
            path = path + "\\";
         path = path.replaceAll("%20", " ");
         return path;
      } catch (Exception e) {
         return null;
      }
   }

   public File createPlainTextFile(String fileName, String extension) {
      File file = new File(resultsFolder + "/" + fileName + extension);
      try {
         new FileWriter(file, false);
         return file;
      } catch (IOException e) {
         e.printStackTrace();
      }
      return null;
   }

   public void appendToPlainText(File file, String data) {
      try {
         FileWriter fileWriter = new FileWriter(file, true);
         PrintWriter printer = new PrintWriter(fileWriter);
         printer.write(data + "\n");
         printer.close();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}
