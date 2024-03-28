package com.jetbrains.JBRperf;

import java.io.*;
import java.util.*;

public class RenderLogReader extends OneScoreLogReader {

    RenderLogReader() {
        super("RenderLogReader");
    }

    public void readLog(String logFilePath) throws FileNotFoundException {
        File file = new File(logFilePath);
        Scanner input = new Scanner(file);

        int i = 0;
        while (input.hasNextLine()) {
            String line = input.nextLine();

             // System.out.println("Line: " + line);

            if (line.startsWith("#") || line.startsWith("[")
                    || !line.contains(":")) continue;

            // Check legend (24.02):
            if (line.startsWith("Test Name")) {
                // System.out.println("LINE:" + line);
                if (line.contains("Median(TimeMs)")) {
                    System.out.println("Unsupported Time unit (expected FPS results) !");
                    return;
                }
                continue;
            }

            // only supports single FPS score:
            if (!line.contains("FPS")) continue;
            String[] scores = line.split(":");
            if (scores.length == 0) break;

            String scoreName = scores[0].trim();
            // Avoid repeated metrics:
            if (!metrics.contains(scoreName)) {
                metrics.add(i++, scoreName);
            }
            values.put(scoreName, Float.valueOf(scores[1].trim().split(" ")[0].replace(",",".")));
        }
    }

    public static final String SCORES_FILE_NAME = "render_perf.txt";
    public static final String REGRESSION_FILE_NAME = "render_perf_comparison.txt";

    String getScoreFile(String path, String score) {
        return path + File.separator + SCORES_FILE_NAME;
    }

    String getRegressionFile(String path) {
        return path + File.separator +  REGRESSION_FILE_NAME;
    }
}
