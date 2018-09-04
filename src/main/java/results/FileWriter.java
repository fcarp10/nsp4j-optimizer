package results;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class FileWriter {

    private File textPlainFile;
    private java.io.FileWriter fileWriter;
    private String folder;

    FileWriter(String folderName) {
        SimpleDateFormat MY_FORMAT = new SimpleDateFormat(
                "dd-MM-yy_HH-mm_", Locale.getDefault());
        Date date = new Date();
        String path = FileWriter.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        path = path.replaceAll("%20", " ");
        File parentDirectory = new File(path + "/../results");
        if (!parentDirectory.exists())
            parentDirectory.mkdir();
        folder = path + "/../results/" + MY_FORMAT.format(date) + "W" + folderName;
        new File(folder).mkdir();
    }

    void initializeTextPlainFile(String fileName) throws IOException {
        textPlainFile = new File(folder + "/" + fileName + ".txt");
        fileWriter = new java.io.FileWriter(textPlainFile, false);
    }

    void writeTextPlain(String content) {
        try {
            fileWriter = new java.io.FileWriter(textPlainFile, true);
            PrintWriter printer = new PrintWriter(fileWriter);
            printer.write(content);
            printer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void createJsonForResults(String fileName, Object object) {
        File jsonFile = new File(folder + "/" + fileName + ".json");
        ObjectMapper mapper = new ObjectMapper(new JsonFactory());
        DefaultPrettyPrinter.Indenter indenter =
                new DefaultIndenter("    ", DefaultIndenter.SYS_LF);
        DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
        printer.indentObjectsWith(indenter);
        printer.indentArraysWith(indenter);
        try {
            mapper.writer(printer).writeValue(jsonFile, object);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
