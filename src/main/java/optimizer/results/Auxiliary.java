package optimizer.results;


import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBVar;
import manager.elements.Function;
import manager.elements.Service;
import optimizer.gui.ResultsGUI;
import org.decimal4j.util.DoubleRounder;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Path;
import org.slf4j.Logger;

import java.util.List;

import static optimizer.Definitions.*;

public class Auxiliary {

   static double avg(List<Object> list) {
      if (list.size() == 0) return 0;
      double average = 0;
      for (Object aList : list) average += Double.parseDouble(aList.toString());
      average = average / list.size();
      return roundDouble(average, 2);
   }

   static double vrc(List<Object> list, double avg) {
      if (list.size() == 0) return 0;
      double variance = 0;
      for (Object aList : list) variance += Math.pow(Double.parseDouble(aList.toString()) - avg, 2);
      variance = variance / list.size();
      return roundDouble(variance, 2);
   }

   public static double max(List<Object> list) {
      if (list.size() == 0) return 0;
      double max = 0;
      for (Object aList : list)
         if (Double.parseDouble(aList.toString()) > max)
            max = Double.parseDouble(aList.toString());
      return roundDouble(max, 4);
   }

   public static double min(List<Object> list) {
      if (list.size() == 0) return 0;
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
         if (var[i] == null) continue;
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
            if (var[i][j] == null) continue;
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
               if (var[i][j][k] == null) continue;
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
                  if (var[i][j][k][l] == null) continue;
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
}
