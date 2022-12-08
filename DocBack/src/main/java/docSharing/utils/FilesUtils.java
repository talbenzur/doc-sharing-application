package docSharing.utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FilesUtils {
    public static String getFileName(String filePath){
        Path path = Paths.get(filePath);
        return path.getFileName().toString();
    }

    public static String readFromFile(String path){
        String data = "";
        try {
            data = new String(Files.readAllBytes(Paths.get(path)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    public static void writeToFile(String content, String path){
        FileWriter fw;
        try {
            fw = new FileWriter(path);
            fw.write(content);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
