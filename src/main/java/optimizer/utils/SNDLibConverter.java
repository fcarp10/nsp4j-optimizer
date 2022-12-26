package optimizer.utils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SNDLibConverter {

   public static void run(String path, String filename, boolean notBidirectional) {
      Scanner scanner = ConfigFiles.scanPlainTextFileInResources("/" + filename + ".txt");
      WritePlainTextFile writePlainTextFile = new WritePlainTextFile(path, filename, ".dgs");
      String temp;
      Map<String, String> nodesMap = new HashMap<>();
      writePlainTextFile.write("DGS004" + System.getProperty("line.separator"));
      writePlainTextFile.write("test 0 0" + System.getProperty("line.separator"));
      writePlainTextFile.write(System.getProperty("line.separator"));
      while (scanner.nextLine().trim().compareTo("NODES (") != 0) {
      }
      int nodeCounter = 1;
      while ((temp = scanner.nextLine()) != null) {
         temp = temp.trim();
         if (temp.trim().compareTo(")") == 0) {
            break;
         }
         Pattern p;
         Matcher m;
         String sourceID = "";
         p = Pattern.compile("[a-zA-Z0-9\\.-]+");
         m = p.matcher(temp);
         if (m.find()) {
            sourceID = m.group(0);
         }
         double[] temp1 = new double[2];
         int count = 0;
         while (m.find()) {
            temp1[count] = Double.parseDouble(m.group(0));
            count++;
            if (count == 2)
               break;
         }
         String nodeName = "n" + nodeCounter;
         nodesMap.put(sourceID, nodeName);
         nodeCounter++;
         writePlainTextFile.write("an " + nodeName + " x:" + temp1[0] + " y:"
                 + temp1[1] + System.getProperty("line.separator"));
      }
      writePlainTextFile.write(System.getProperty("line.separator"));
      while (scanner.nextLine().trim().compareTo("LINKS (") != 0) {
      }
      while ((temp = scanner.nextLine()) != null) {
         temp = temp.trim();
         if (temp.length() == 1) {
            break;
         }
         Pattern p;
         Matcher m;
         p = Pattern.compile("[a-zA-Z0-9\\.]+");
         m = p.matcher(temp);
         String[] temp1 = new String[7];
         int count = 0;
         while (m.find()) {
            temp1[count] = m.group(0);
            count++;
            if (count == 7)
               break;
         }
         writePlainTextFile.write("ae " + nodesMap.get(temp1[1]) + nodesMap.get(temp1[2]) + " "
                 + nodesMap.get(temp1[1]) + " > " + nodesMap.get(temp1[2]) + System.getProperty("line.separator"));
         if(notBidirectional)
            writePlainTextFile.write("ae " + nodesMap.get(temp1[2]) + nodesMap.get(temp1[1]) + " "
                    + nodesMap.get(temp1[2]) + " > " + nodesMap.get(temp1[1]) + System.getProperty("line.separator"));

      }
   }
}
