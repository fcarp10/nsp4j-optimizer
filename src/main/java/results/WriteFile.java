package results;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WriteFile {

    private File file;
    private ObjectMapper mapper;
    private FileWriter filewriter;

    public WriteFile(String fileName, String factors) {
        SimpleDateFormat MY_FORMAT = new SimpleDateFormat(
                "dd-MM-yy--HH-mm", Locale.getDefault());
        Date date = new Date();
        String path = WriteFile.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        path = path.replaceAll("%20", " ");
        File parentDirectory = new File(path + "/../results");
        if (!parentDirectory.exists())
            parentDirectory.mkdir();
        String folder = path + "/../results/" + MY_FORMAT.format(date) + "-" + factors;
        new File(folder).mkdir();
        file = new File(folder + "/" + fileName);

    }

    public void initializeJsonFile() {
        mapper = new ObjectMapper(new JsonFactory());
    }

    public void initializeTextPlainFile() throws IOException {
        filewriter = new FileWriter(file, false);
    }

    public void writeJson(String content) {
        try {
            mapper.writeValue(file, content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeTextPlain(String content) {
        try {
            filewriter = new FileWriter(file, true);
            PrintWriter printer = new PrintWriter(filewriter);
            printer.write(content);
            printer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
