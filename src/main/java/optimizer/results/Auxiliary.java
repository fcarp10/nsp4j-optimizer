package optimizer.results;

import static optimizer.Definitions.ERROR;
import static optimizer.Definitions.FUNCTION_MAX_DELAY;
import static optimizer.Definitions.INFO;
import static optimizer.Definitions.LINK_DELAY;
import static optimizer.Definitions.SERVICE_DOWNTIME;
import static optimizer.Definitions.WARNING;

import java.util.List;

import org.decimal4j.util.DoubleRounder;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Path;
import org.slf4j.Logger;

import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBModel;
import gurobi.GRBVar;
import manager.Parameters;
import manager.elements.Function;
import manager.elements.Service;
import optimizer.gui.ResultsGUI;
import static optimizer.Definitions.*;

public class Auxiliary {

   static double avg(List<Double> list) {
      if (list.size() == 0)
         return 0;
      double average = 0;
      for (Object aList : list)
         average += Double.parseDouble(aList.toString());
      average = average / list.size();
      return roundDouble(average, 2);
   }

   static double vrc(List<Object> list, double avg) {
      if (list.size() == 0)
         return 0;
      double variance = 0;
      for (Object aList : list)
         variance += Math.pow(Double.parseDouble(aList.toString()) - avg, 2);
      variance = variance / list.size();
      return roundDouble(variance, 2);
   }

   public static double max(List<Object> list) {
      if (list.size() == 0)
         return 0;
      double max = 0;
      for (Object aList : list)
         if (Double.parseDouble(aList.toString()) > max)
            max = Double.parseDouble(aList.toString());
      return roundDouble(max, 4);
   }

   public static double min(List<Object> list) {
      if (list.size() == 0)
         return 0;
      double min = Double.MAX_VALUE;
      for (Object aList : list)
         if (Double.parseDouble(aList.toString()) < min)
            min = Double.parseDouble(aList.toString());
      return roundDouble(min, 4);
   }

   public static double roundDouble(double value, int decimals) {
      return DoubleRounder.round(value, decimals);
   }

   public static void printLog(Logger log, String status, String message) {
      switch (status) {
         case ERROR:
            log.error(message);
            break;
         case WARNING:
            log.warn(message);
            break;
         case INFO:
            log.info(message);
            break;
      }
      ResultsGUI.log(status + message);
   }

   public static double getMaxServiceDowntime(Service service) {
      double downtime = (double) service.getAttribute(SERVICE_DOWNTIME);
      return downtime * service.getFunctions().size();
   }

   public static double getMaxPathDelay(List<Path> paths) {
      double maxPathDelay = 0;
      for (Path p : paths) {
         double pathDelay = 0;
         for (Edge e : p.getEdgePath())
            pathDelay += (double) e.getAttribute(LINK_DELAY) * 1000; // in ms
         if (pathDelay > maxPathDelay)
            maxPathDelay = pathDelay;
      }
      return maxPathDelay;
   }

   public static double getMaxProcessingDelay(List<Function> functions) {
      double maxProcessingDelay = 0;
      for (Function f : functions)
         if ((double) f.getAttribute(FUNCTION_MAX_DELAY) > maxProcessingDelay)
            maxProcessingDelay = (double) f.getAttribute(FUNCTION_MAX_DELAY);
      return maxProcessingDelay;
   }

   public static boolean[] grbVarsToBooleans(GRBVar[] var) throws GRBException {
      boolean[] convertedVar = new boolean[var.length];
      for (int i = 0; i < var.length; i++) {
         if (var[i] == null)
            continue;
         if (var[i].get(GRB.DoubleAttr.X) == 1.0)
            convertedVar[i] = true;
      }
      return convertedVar;
   }

   public static boolean[][] grbVarsToBooleans(GRBVar[][] var) throws GRBException {
      boolean[][] convertedVar;
      if (var.length > 0)
         convertedVar = new boolean[var.length][var[0].length];
      else
         convertedVar = new boolean[0][0];
      for (int i = 0; i < var.length; i++)
         for (int j = 0; j < var[i].length; j++) {
            if (var[i][j] == null)
               continue;
            if (var[i][j].get(GRB.DoubleAttr.X) == 1.0)
               convertedVar[i][j] = true;
         }
      return convertedVar;
   }

   public static boolean[][][] grbVarsToBooleans(GRBVar[][][] var) throws GRBException {
      boolean[][][] convertedVar;
      if (var.length > 0)
         convertedVar = new boolean[var.length][var[0].length][var[0][0].length];
      else
         convertedVar = new boolean[0][0][0];
      for (int i = 0; i < var.length; i++)
         for (int j = 0; j < var[i].length; j++)
            for (int k = 0; k < var[i][j].length; k++) {
               if (var[i][j][k] == null)
                  continue;
               if (var[i][j][k].get(GRB.DoubleAttr.X) == 1.0)
                  convertedVar[i][j][k] = true;
            }
      return convertedVar;
   }

   public static boolean[][][][] grbVarsToBooleans(GRBVar[][][][] var) throws GRBException {
      boolean[][][][] convertedVar;
      if (var.length > 0)
         convertedVar = new boolean[var.length][var[0].length][var[0][0].length][var[0][0][0].length];
      else
         convertedVar = new boolean[0][0][0][0];
      for (int i = 0; i < var.length; i++)
         for (int j = 0; j < var[i].length; j++)
            for (int k = 0; k < var[i][j].length; k++)
               for (int l = 0; l < var[i][j][k].length; l++) {
                  if (var[i][j][k][l] == null)
                     continue;
                  if (var[i][j][k][l].get(GRB.DoubleAttr.X) == 1.0)
                     convertedVar[i][j][k][l] = true;
               }
      return convertedVar;
   }

   public static double[] grbVarsToDoubles(GRBVar[] var) throws GRBException {
      double[] convertedVar = new double[var.length];
      for (int i = 0; i < var.length; i++)
         if (var[i] != null)
            convertedVar[i] = var[i].get(GRB.DoubleAttr.X);
      return convertedVar;
   }

   public static double[][] grbVarsToDoubles(GRBVar[][] var) throws GRBException {
      double[][] convertedVar = new double[var.length][var[0].length];
      for (int i = 0; i < var.length; i++)
         for (int j = 0; j < var[i].length; j++)
            if (var[i][j] != null)
               convertedVar[i][j] = var[i][j].get(GRB.DoubleAttr.X);
      return convertedVar;
   }

   public static double[][][] grbVarsToDoubles(GRBVar[][][] var) throws GRBException {
      double[][][] convertedVar = new double[var.length][var[0].length][var[0][0].length];
      for (int i = 0; i < var.length; i++)
         for (int j = 0; j < var[i].length; j++)
            for (int k = 0; k < var[i][j].length; k++)
               if (var[i][j][k] != null)
                  convertedVar[i][j][k] = var[i][j][k].get(GRB.DoubleAttr.X);
      return convertedVar;
   }

   public static double[][][][] grbVarsToDoubles(GRBVar[][][][] var) throws GRBException {
      double[][][][] convertedVar = new double[var.length][var[0].length][var[0][0].length][var[0][0][0].length];
      for (int i = 0; i < var.length; i++)
         for (int j = 0; j < var[i].length; j++)
            for (int k = 0; k < var[i][j].length; k++)
               for (int l = 0; l < var[i][j][k].length; l++)
                  if (var[i][j][k][l] != null)
                     convertedVar[i][j][k][l] = var[i][j][k][l].get(GRB.DoubleAttr.X);
      return convertedVar;
   }

   public static boolean[][] zSPvarsFromInitialModel(Parameters pm, GRBModel initialModel) {
      boolean[][] zSPvar = new boolean[pm.getServices().size()][pm.getPathsTrafficFlow()];
      if (initialModel != null)
         try {
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
                  if (initialModel.getVarByName(zSP + "[" + s + "][" + p + "]").get(GRB.DoubleAttr.X) == 1.0)
                     zSPvar[s][p] = true;
         } catch (GRBException e) {
            e.printStackTrace();
         }
      return zSPvar;
   }

   public static boolean[][][] zSPDvarsFromInitialModel(Parameters pm, GRBModel initialModel) {
      boolean[][][] zSPDvar = new boolean[pm.getServices().size()][pm.getPathsTrafficFlow()][pm
            .getDemandsTrafficFlow()];
      if (initialModel != null)
         try {
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
                  for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                     if (initialModel.getVarByName(zSPD + "[" + s + "][" + p + "][" + d + "]")
                           .get(GRB.DoubleAttr.X) == 1.0)
                        zSPDvar[s][p][d] = true;
         } catch (GRBException e) {
            e.printStackTrace();
         }
      return zSPDvar;
   }

   public static boolean[][][] fXSVvarsFromInitialModel(Parameters pm, GRBModel initialModel) {
      boolean[][][] fXSVvar = new boolean[pm.getServers().size()][pm.getServices().size()][pm.getServiceLength()];
      if (initialModel != null)
         try {
            for (int x = 0; x < pm.getServers().size(); x++)
               for (int s = 0; s < pm.getServices().size(); s++)
                  for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                     if (initialModel.getVarByName(fXSV + "[" + x + "][" + s + "][" + v + "]")
                           .get(GRB.DoubleAttr.X) == 1.0)
                        fXSVvar[x][s][v] = true;
         } catch (GRBException e) {
            e.printStackTrace();
         }
      return fXSVvar;
   }

   public static boolean[][][][] fXSVDvarsFromInitialModel(Parameters pm, GRBModel initialModel) {
      boolean[][][][] fXSVDvar = new boolean[pm.getServers().size()][pm.getServices().size()][pm.getServiceLength()][pm
            .getDemandsTrafficFlow()];
      if (initialModel != null)
         try {
            for (int x = 0; x < pm.getServers().size(); x++)
               for (int s = 0; s < pm.getServices().size(); s++)
                  for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                     for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                        if (initialModel.getVarByName(fXSVD + "[" + x + "][" + s + "][" + v + "][" + d + "]")
                              .get(GRB.DoubleAttr.X) == 1.0)
                           fXSVDvar[x][s][v][d] = true;
         } catch (GRBException e) {
            e.printStackTrace();
         }
      return fXSVDvar;
   }
}
