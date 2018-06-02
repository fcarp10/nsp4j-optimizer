package results;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WriteFile {

    protected File file;
    protected FileWriter filewriter;
    protected String filePath;

    public WriteFile(String fileName, String factors)
            throws IOException {

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
        filewriter = new FileWriter(file, false);

        this.filePath = folder + "/" + fileName;
    }

    public void write(String content) {

        try {
            filewriter = new FileWriter(file, true);
            PrintWriter printer = new PrintWriter(filewriter);
            printer.write(content);
            printer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void append(List<String> content) {

        try {
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            List<String> lines = new ArrayList<>();
            String line;

            while ((line = bufferedReader.readLine()) != null)
                lines.add(line);

            bufferedReader.close();
            line = "";
            int initialPosition = 0;
            while (!line.startsWith("#")) {
                line = lines.get(initialPosition);
                initialPosition++;
            }

            int j = 0;
            FileOutputStream fileOut = new FileOutputStream(this.filePath);
            for (int i = 0; i < lines.size(); i++) {
                String lineBytes;
                if (i >= initialPosition) {
                    lineBytes = lines.get(i) + content.get(j) + "\n";
                    j++;
                } else
                    lineBytes = lines.get(i) + "\n";
                fileOut.write(lineBytes.getBytes());
            }

            fileOut.close();

        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
