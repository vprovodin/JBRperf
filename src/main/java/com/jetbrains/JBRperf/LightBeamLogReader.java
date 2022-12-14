package com.jetbrains.JBRperf;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class LightBeamLogReader extends OneScoreLogReader {

    LightBeamLogReader() {
        super("LightBeamLogReader");
        metricWidthMax = 10;
    }

    public void readLog(String logFilePath) throws FileNotFoundException {
        File file = new File(logFilePath);
        Scanner input = new Scanner(file);

        int i = 0;
        while (input.hasNextLine()) {
            String line = input.nextLine();
            if (line.trim().length() == 0) continue;
            if (!line.trim().startsWith("##")) continue;

            String[] arrLaf = line.replaceAll("'","").replace("##teamcity[buildStatisticValue key=","")
                    .replace("value=","").replace("]","").split(" ");
            String[] arrCtrl = arrLaf[0].split(":");
            String scoreName = arrCtrl[1] + ':' + arrCtrl[2];
            String score = arrLaf[1];

            metrics.add(i++, scoreName);
            values.put(scoreName, Float.valueOf(score.replace(",", ".")));
        }
    }

    public static final String SCORES_FILE_NAME = "LightBeam_scores.txt";
    public static final String REGRESSION_FILE_NAME = "regression.txt";

    String getScoreFile(String path, String score) {
        return path + File.separator + SCORES_FILE_NAME;
    }

    String getRegressionFile(String path) {
        return path + File.separator +  REGRESSION_FILE_NAME;
    }
}
