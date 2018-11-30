package Server.Common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.stream.Stream;

public class Log implements Serializable {
  private static final long serialVersionUID = 992150778513571831L;
  public static final String OUTPUT_FOLDER = "../data";
  public static final String FILE_EXTENSION = "data";
  private static final File file = new File(OUTPUT_FOLDER + "/" + "log" + "." + FILE_EXTENSION);

  public Log() {
  }

  public synchronized void write(String data) {
    try {
      BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
      bw.write(data + "\n");
      bw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public int getLatestTransactionId() {
      try {
        Stream<String> stream = Files.lines(file.toPath());
        return stream.filter(line -> line.startsWith("TM")).map(line -> Integer.valueOf(line.split("\t")[1])).max(Integer::compare).get();
      } catch (IOException e) {
        e.printStackTrace();
      }
      return 0;
  }
}
