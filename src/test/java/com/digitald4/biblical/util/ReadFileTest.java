package com.digitald4.biblical.util;

import java.io.BufferedReader;
import java.io.FileReader;

public class ReadFileTest {

  protected static String getContent(String filename) throws Exception {
    BufferedReader br = new BufferedReader(new FileReader(filename));

    StringBuilder content = new StringBuilder();
    String line;
    while ((line = br.readLine()) != null) {
      content.append(line).append("\n");
    }

    return content.toString();
  }

}
