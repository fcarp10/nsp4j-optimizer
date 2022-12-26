package optimizer.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class WritePlainTextFile {

   private File file;
   private FileWriter filewriter;

   public WritePlainTextFile(String path, String fileName, String extension) {
      file = new File(path + "/" + fileName + extension);
      try {
         filewriter = new FileWriter(file, false);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public void write(String value) {
      try {
         filewriter = new FileWriter(file, true);
         PrintWriter printer = new PrintWriter(filewriter);
         printer.write(value);
         printer.close();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}
