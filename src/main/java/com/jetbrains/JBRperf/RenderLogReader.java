package com.jetbrains.JBRperf;

import java.io.*;
import java.util.*;

public class RenderLogReader extends DataLogReader {

    ArrayList<String> metrics = new ArrayList<String>();
    HashMap<String, Float> values = new HashMap<String, Float>();

    RenderLogReader() {
        super("RenderLogReader");
    }

    public void readLog(String logFilePath) throws FileNotFoundException {
        File file = new File(logFilePath);
        Scanner input = new Scanner(file);
        boolean isReadStarted = false;

        int i = 0;
        while (input.hasNextLine()) {
            String line = input.nextLine();
            String[] scores = line.split(":");
            if (scores.length == 0) break;

            String scoreName = scores[0].trim();
            metrics.add(i, scoreName);
            values.put(scoreName, Float.valueOf(scores[1].trim().split(" ")[0].replace(",",".")));
        }
    }

    public List<String> getScoreNamesList() {return Collections.emptyList();}

    public Map<String, Float> getScores(String scoreName) {
        return values;
    }

    public static final String SCORES_FILE_NAME = "render_perf.txt";
    public static final String REGRESSION_FILE_NAME = "render_perf_comparison.txt";

    String getScoreFile(String path, String score) {
        return path + File.separator + SCORES_FILE_NAME;
    }
    String getScoreFile(String path) {
        return getScoreFile(path, "");
    }

    String getRegressionFile(String path) {
        return path + File.separator +  REGRESSION_FILE_NAME;
    }
    String getRegressionFile(String path, String score) {
        return getRegressionFile(path);
    }

    public List<String> storeScores(String outDir) throws IOException {

        ArrayList<String> scoreMetrics = new ArrayList<>();

        String fileName = getScoreFile(outDir);
        PrintWriter printWriter = new PrintWriter(new FileWriter(fileName));
        for (int i = 0; i < metrics.size() - 1; i++) {
            float value = values.get(metrics.get(i));
            //printWriter.println(metrics.get(i) + " : " + value);
            printWriter.printf(Locale.UK, "%-30s : %7.2f%n", metrics.get(i), value);
            scoreMetrics.add(fileName);
        }
        printWriter.close();
        return scoreMetrics;
    }

    void compareResults(String currentDir, String referenceDir, String cmpresDir,
                        Float deviation) throws IOException {

        String scoreFileName = getScoreFile(referenceDir);
        File file = new File(scoreFileName);
        if ( !file.exists()) {
            ScoresComparator.logger.log(scoreFileName + " does not exist, skipping scores comnparison");
            return;
        }
        Scanner input = new Scanner(file);

        boolean isReadingStarted = false;
        PrintWriter printWriter = new PrintWriter(new FileWriter(getRegressionFile(referenceDir)));

        while (input.hasNextLine()) {
            if (!isReadingStarted) {
                isReadingStarted = true;
                if (ScoresComparator.IS_HEADER_INCLUDED) {
                    printWriter.println(input.nextLine());
                    continue;
                }
            }

            String line = input.nextLine();
            String[] scoreNameValue = line.split("\t");

            String fullTestName = scoreNameValue[0].trim();
            ScoresComparator.logger.logTC("##teamcity[testStarted name=\'" + fullTestName + "\']");

            float currentValue = values.get(fullTestName);
            float referenceValue;
            try {
                referenceValue = Float.valueOf(scoreNameValue[1]);
            } catch (NumberFormatException e) {
                referenceValue = Float.valueOf(scoreNameValue[1].replace(',', '.'));
            }

            float diff;
            boolean failed = false;
            if (ScoresComparator.RESULT_INTERPRETER == ResultInterpreter.higher_better) {
                diff = (referenceValue != 0) ? (currentValue / referenceValue * 100 - 100) : Float.NaN;
                failed = (currentValue < referenceValue * (1 - deviation));
            } else {
                diff = (referenceValue != 0) ? (100 - currentValue / referenceValue * 100) : Float.NaN;
                failed = (currentValue > referenceValue * (1 + deviation));
            }

            ScoresComparator.logger.logTCf("##teamcity[buildStatisticValue key=\'%s\' value=\'%f\']\n", fullTestName, currentValue);
            ScoresComparator.logger.logf("buildStatisticValue key=\'%s\' value=\'%7.2f'\n", fullTestName, currentValue);
            if (failed) {
                printWriter.print(ScoresComparator.FAILED_SIGN);
                ScoresComparator.logger.logTCf("##teamcity[testFailed name=\'%s\' message=\'currentValue=%7.2f referenceValue=%7.2f diff=%6.2f\']\n", fullTestName, currentValue, referenceValue, diff);
                ScoresComparator.logger.logf("***testFailed name=\'%s\' currentValue=%7.2f referenceValue=%7.2f diff=%6.2f\n",fullTestName, currentValue, referenceValue, diff);
            } else {
                printWriter.print(ScoresComparator.PASSED_SIGN);
            }
            printWriter.printf(Locale.UK, "%-30s\t%7.2f\t%7.2f\t%6.2f\t%5.2f%n",
                    scoreNameValue[0], referenceValue, currentValue, diff, deviation);
            ScoresComparator.logger.logTC("##teamcity[testFinished name=\'" + fullTestName + "\' duration=\'" + diff + "\']");
        }
        printWriter.close();
    }

    void compareResults(String scoreName, String currentDir, String referenceDir, String cmpresDir,
                        Float deviation) throws NotImplementedException {
        throw new NotImplementedException();
    }

}
