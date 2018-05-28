package net;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class WriteToFile {

    private static final String FILENAME = "data.txt";

    public static void write(String data) {

        BufferedWriter bw = null;
        FileWriter fw = null;
        try {

            fw = new FileWriter(FILENAME, true);
            bw = new BufferedWriter(fw);
            bw.write(data);
            System.out.println("Writing Done");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null) {
                    bw.close();
                }
                if (fw != null) {
                    fw.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
