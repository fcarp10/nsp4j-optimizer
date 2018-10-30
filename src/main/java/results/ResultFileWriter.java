package results;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ResultFileWriter {

    private String folder;

    public ResultFileWriter(String folderName) {
        SimpleDateFormat MY_FORMAT = new SimpleDateFormat(
                "dd-MM-yy_HH-mm-ss_", Locale.getDefault());
        Date date = new Date();
        String path = ResultFileWriter.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        path = path.replaceAll("%20", " ");
        File parentDirectory = new File(path + "/../results");
        if (!parentDirectory.exists())
            parentDirectory.mkdir();
        folder = path + "/../results/" + MY_FORMAT.format(date) + folderName;
        new File(folder).mkdir();
    }

    public void createJsonForResults(String fileName, Object object) {
        File jsonFile = new File(folder + "/" + fileName + ".json");
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
}
