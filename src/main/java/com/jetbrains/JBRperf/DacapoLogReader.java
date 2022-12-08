package com.jetbrains.JBRperf;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class DacapoLogReader extends OneScoreLogReader {

    DacapoLogReader() {
        super("DacapoLogReader");
        metricWidthMax = 10;
    }

    public void readLog(String logFilePath) throws FileNotFoundException {
        File file = new File(logFilePath);
        Scanner input = new Scanner(file);

        int i = 0;
/*
===== DaCapo 9.12 avrora starting =====
===== DaCapo 9.12 avrora PASSED in 1973 msec =====
===== DaCapo 9.12 fop starting =====
===== DaCapo 9.12 fop PASSED in 624 msec =====
Using scaled threading model. 8 processors detected, 8 threads used to drive the workload, in a possible range of [1,4000]
===== DaCapo 9.12 h2 starting =====
. . .
 */
        while (input.hasNextLine()) {
            String line = input.nextLine();
            if (!line.startsWith("===== DaCapo") || !line.contains("msec")) continue;

            String[] arr = line.split(" ");

            String scoreName = arr[3].trim();
            metrics.add(i++, scoreName);
            values.put(scoreName, Float.valueOf(arr[6].trim()));
        }
    }

    public static final String SCORES_FILE_NAME = "dacapo_tests.txt";
    public static final String REGRESSION_FILE_NAME = "regression.txt";

    String getScoreFile(String path, String score) {
        return path + File.separator + SCORES_FILE_NAME;
    }

    String getRegressionFile(String path) {
        return path + File.separator +  REGRESSION_FILE_NAME;
    }
}
