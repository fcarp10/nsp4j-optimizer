package results;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WriteFile {

    private File file;
    private ObjectMapper mapper;

    public WriteFile(String fileName, String factors) {
        SimpleDateFormat MY_FORMAT = new SimpleDateFormat(
                "dd-MM-yy__HH-mm", Locale.getDefault());
        Date date = new Date();
        String path = WriteFile.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        path = path.replaceAll("%20", " ");
        File parentDirectory = new File(path + "/../results");
        if (!parentDirectory.exists())
            parentDirectory.mkdir();
        String folder = path + "/../results/" + MY_FORMAT.format(date) + "__" + factors;
        new File(folder).mkdir();
        file = new File(folder + "/" + fileName);
        mapper = new ObjectMapper(new YAMLFactory());
    }

    private void write(String value) {
        try {
            mapper.writeValue(file, value);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
