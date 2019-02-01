package output;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import gui.WebClient;
import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBVar;
import lp.CostFunctions;
import org.decimal4j.util.DoubleRounder;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static output.Definitions.ERROR;
import static output.Definitions.INFO;

public class Auxiliary {

   public static CostFunctions costFunctions;

   public Auxiliary() {
      TypeReference<CostFunctions> typeReference = new TypeReference<>() {
      };
      InputStream inputStream = TypeReference.class.getResourceAsStream("/aux_files/linear-cost-functions.yml");
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      try {
         costFunctions = mapper.readValue(inputStream, typeReference);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   static double avg(List<Object> list) {
      if (list.size() == 0) return 0;
      double average = 0;
      for (Object aList : list) average += Double.valueOf(aList.toString());
      average = average / list.size();
      return roundDouble(average, 2);
   }

   static double vrc(List<Object> list, double avg) {
      if (list.size() == 0) return 0;
      double variance = 0;
      for (Object aList : list) variance += Math.pow(Double.valueOf(aList.toString()) - avg, 2);
      variance = variance / list.size();
      return roundDouble(variance, 2);
   }

   public static double max(List<Object> list) {
      if (list.size() == 0) return 0;
      double max = 0;
      for (Object aList : list)
         if (Double.valueOf(aList.toString()) > max)
            max = Double.valueOf(aList.toString());
      return roundDouble(max, 2);
   }

   public static double min(List<Object> list) {
      if (list.size() == 0) return 0;
      double min = Double.MAX_VALUE;
      for (Object aList : list)
         if (Double.valueOf(aList.toString()) < min)
            min = Double.valueOf(aList.toString());
      return roundDouble(min, 2);
   }

   public static double roundDouble(double value, int decimals) {
      return DoubleRounder.round(value, decimals);
   }

   public static void printLog(Logger log, String status, String message) {
      switch (status) {
         case ERROR:
            log.error(message);
            break;
         case INFO:
            log.info(message);
            break;
      }
      WebClient.postMessage(status + message);
   }

   public static boolean[] convertVariablesToBooleans(GRBVar[] var) throws GRBException {
      boolean[] convertedVar = new boolean[var.length];
      for (int i = 0; i < var.length; i++) {
         if (var[i] == null) continue;
         if (var[i].get(GRB.DoubleAttr.X) == 1.0)
            convertedVar[i] = true;
      }
      return convertedVar;
   }

   public static boolean[][] convertVariablesToBooleans(GRBVar[][] var) throws GRBException {
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

   public static boolean[][][] convertVariablesToBooleans(GRBVar[][][] var) throws GRBException {
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

   public static boolean[][][][] convertVariablesToBooleans(GRBVar[][][][] var) throws GRBException {
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

   public static double[] convertVariablesToDoubles(GRBVar[] var) throws GRBException {
      double[] convertedVar = new double[var.length];
      for (int i = 0; i < var.length; i++)
         if (var[i] != null)
            convertedVar[i] = var[i].get(GRB.DoubleAttr.X);
      return convertedVar;
   }

   public static double[][] convertVariablesToDoubles(GRBVar[][] var) throws GRBException {
      double[][] convertedVar = new double[var.length][var[0].length];
      for (int i = 0; i < var.length; i++)
         for (int j = 0; j < var[i].length; j++)
            if (var[i][j] != null)
               convertedVar[i][j] = var[i][j].get(GRB.DoubleAttr.X);
      return convertedVar;
   }

   public static double[][][] convertVariablesToDoubles(GRBVar[][][] var) throws GRBException {
      double[][][] convertedVar = new double[var.length][var[0].length][var[0][0].length];
      for (int i = 0; i < var.length; i++)
         for (int j = 0; j < var[i].length; j++)
            for (int k = 0; k < var[i][j].length; k++)
               if (var[i][j][k] != null)
                  convertedVar[i][j][k] = var[i][j][k].get(GRB.DoubleAttr.X);
      return convertedVar;
   }

   public static double[][][][] convertVariablesToDoubles(GRBVar[][][][] var) throws GRBException {
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
