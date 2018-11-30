package Server.Common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class ShadowPage<T> implements Serializable {
  private static final long serialVersionUID = 992150778513571821L;
  public static final String OUTPUT_FOLDER = "../data";
  public static final String FILE_EXTENSION = "data";
  private File file;

  public ShadowPage(String rm, String name) {
    file = new File(OUTPUT_FOLDER + "/" + rm + "/" + name + "." + FILE_EXTENSION);
  }

  public void save(T data) {
    file.getParentFile().mkdirs();

    try {
      file.createNewFile();
      ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
      oos.writeObject(data);
      oos.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public T load() {
    try {
      ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
      @SuppressWarnings("unchecked")
      T data = (T) ois.readObject();
      ois.close();
      return data;
    } catch (IOException | ClassNotFoundException e) {
      return null;
    }
  }
}
