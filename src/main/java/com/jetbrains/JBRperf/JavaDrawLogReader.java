package com.jetbrains.JBRperf;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class JavaDrawLogReader extends OneScoreLogReader {

    JavaDrawLogReader() {
        super("JavaDrawLogReader");
        metricWidthMax = 10;
    }

    public void readLog(String logFilePath) throws FileNotFoundException {
        File file = new File(logFilePath);
        Scanner input = new Scanner(file);

        int i = 0;
        while (input.hasNextLine()) {
            String line = input.nextLine();
            if (line.trim().length() == 0) continue;
            if (line.contains("Total")) break;

            String scoreName = line.substring(0,39).trim().replace(" ", "_");
            String frames = line.substring(38,47).trim();
            String fps = line.substring(47,56).trim();

            metrics.add(i++, scoreName);
            values.put(scoreName, Float.valueOf(fps));
        }
    }

    public static final String SCORES_FILE_NAME = "JavaDraw.txt";
    public static final String REGRESSION_FILE_NAME = "regression.txt";

    String getScoreFile(String path, String score) {
        return path + File.separator + SCORES_FILE_NAME;
    }

    String getRegressionFile(String path) {
        return path + File.separator +  REGRESSION_FILE_NAME;
    }
}
