package com.jetbrains.JBRperf;

import java.io.*;
import java.util.*;

public class J2DBenchLogReader extends OneScoreLogReader {

    J2DBenchLogReader() {
        super("J2DBenchLogReader");
    }

    public void readLog(String logFilePath) throws FileNotFoundException {
        File file = new File(logFilePath);
        Scanner input = new Scanner(file);

        int i = 0;
        while (input.hasNextLine()) {
            String line = input.nextLine();
            String[] scores = line.split(",");
            if (scores.length == 0) break;

            String scoreName = scores[0].trim();
            metrics.add(i, scoreName);
            values.put(scoreName, Float.valueOf(scores[1].trim()));
        }
    }

    public static final String SCORES_FILE_NAME = "J2DBench.txt";
    public static final String REGRESSION_FILE_NAME = "J2DBench_comparison.txt";

    String getScoreFile(String path, String score) {
        return path + File.separator + SCORES_FILE_NAME;
    }
    String getRegressionFile(String path) {
        return path + File.separator +  REGRESSION_FILE_NAME;
    }
}
