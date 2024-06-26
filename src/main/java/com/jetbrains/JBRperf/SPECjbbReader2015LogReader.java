package com.jetbrains.JBRperf;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class SPECjbbReader2015LogReader extends OneScoreLogReader {

    SPECjbbReader2015LogReader() {
        super("SPECjbbReader2015LogReader");
        measureName = "jOPS";
    }

    @Override
    public void readLog(String logFilePath) throws FileNotFoundException {
        File file = new File(logFilePath);
        Scanner input = new Scanner(file);

        int i = 0;
        while (input.hasNextLine()) {
            String line = input.nextLine();
            if (line.trim().startsWith("#")) continue;
            if (!line.startsWith("jbb2015.result.metric.")) continue;
            if (!line.contains(" = ")) continue;

            String[] scores = line.split(" = ");
            if (scores.length == 0) break;

            String scoreName = scores[0].trim();
            metrics.add(i++, scoreName);
            values.put(scoreName, Float.valueOf(scores[1].trim()));
        }
    }

    public static final String SCORES_FILE_NAME = "specjbb2015_perf.txt";
    public static final String REGRESSION_FILE_NAME = "specjbb2015_perf_comparison.txt";

    @Override
    String getScoreFile(String path, String score) {
        return path + File.separator + SCORES_FILE_NAME;
    }

    @Override
    String getRegressionFile(String path) {
        return path + File.separator +  REGRESSION_FILE_NAME;
    }
}
