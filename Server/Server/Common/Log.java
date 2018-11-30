package Server.Common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;

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
}
