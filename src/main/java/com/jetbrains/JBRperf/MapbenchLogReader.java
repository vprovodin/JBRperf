package com.jetbrains.JBRperf;

import jdk.jshell.spi.ExecutionControl;

import java.io.*;
import java.util.*;

public class MapbenchLogReader extends DataLogReader {

    private ArrayList<String> valuableLines = new ArrayList<String>(); // rows
    private Float[][] values;

    private String[] scores; //header
    private String[] metrics; // rows

    private static final String[] SCORES_TO_PRINT = {
            "Pct95",
            "Avg",
            "StdDev",
            "Min",
            "Max"
    };

    MapbenchLogReader() {
        super("MapbenchLogReader");
    }

    public void readLog(String logFilePath) throws FileNotFoundException {
        File file = new File(logFilePath);
        Scanner input = new Scanner(file);
        boolean isReadStarted = false;
        boolean isReadFinished = false;

        while (input.hasNextLine()) {

            if (!isReadStarted) {
                isReadStarted = (input.nextLine().compareTo("TEST results:") == 0);

                if (isReadStarted) {
                    String line = input.nextLine();
                    scores = line.split("\t");
                }
                continue;
            } else {
                String line = input.nextLine();

                if (line.isEmpty())
                    break;
                valuableLines.add(line);
            }
        }

        values = new Float[scores.length + 1][valuableLines.size()];
        metrics = new String[valuableLines.size()];
        String[] line;
        for (int i = 0; i < valuableLines.size(); i++) {
            line = valuableLines.get(i).split("\t");
            metrics[i] = line[0].trim();
            for (int j = 1; j < line.length; j++) {
                values[i][j] = Float.valueOf(line[j]);
            }
        }
    }

    public static final String OUT_FILE_PREFIX = "mapbench_tests_";

    String getScoreFile(String path, String score) {
        return path + File.separator + OUT_FILE_PREFIX + score + ".txt";
    }

    public static final String REGRESSION_FILE_PREFIX = "regression_";

    String getRegressionFile(String path, String score) {
        return path + File.separator + REGRESSION_FILE_PREFIX + score + ".txt";
    }

    /**
     * The method creates a list of text files for each score in <code>SCORES_TO_PRINT</code>. The files contain
     * metric name and its value related to the corresponding score.
     *
     * @param outDir - output directory
     * @return A list of text files for each score in <code>SCORES_TO_PRINT</code>. The files contain metric name and
     * its value related to the corresponding score.
     * @throws IOException
     */
    public List<String> storeScores(String outDir) throws IOException {

        ArrayList<String> scoreMetrics = new ArrayList<>();

        List<String> scores_to_print = Arrays.asList(SCORES_TO_PRINT);
        String measure = (ScoresComparator.RESULT_INTERPRETER == ResultInterpreter.higher_better) ? "FPS" : "ms";

        for (int i = 0; i < scores.length - 1; i++) {

            if (!scores_to_print.contains(scores[i + 1])) continue;

            String fileName = getScoreFile(outDir, scores[i + 1]);

            PrintWriter printWriter = new PrintWriter(new FileWriter(fileName));
            printWriter.println(scores[0] + "\t" + scores[i + 1]);
            for (int j = 0; j < metrics.length; j++) {
                printWriter.printf(Locale.UK, "%-50s\t%7.2f\t%s%n",
                        metrics[j].trim() + "." + scores[i + 1], values[j][i + 1], measure);
                System.out.printf(Locale.UK, "%-50s\t%7.2f\t%s%n",
                        metrics[j].trim() + "." + scores[i + 1], values[j][i + 1], measure);
            }

            printWriter.close();
            scoreMetrics.add(fileName);
        }
        return scoreMetrics;
    }

    void compareResults(String currentDir, String referenceDir, String cmpresDir,
                        Float deviation) throws NotImplementedException {
        throw new NotImplementedException();
    }

    void compareResults(String scoreName, String currentDir, String referenceDir, String cmpresDir,
                                       Float deviation) throws IOException {
        ScoresComparator.logger.log("processign logs for :" + scoreName);

        Map<String, Float> curData = getScores(scoreName);

        String scoreFileName = getScoreFile(referenceDir, scoreName);
        File file = new File(scoreFileName);
        if ( !file.exists()) {
            ScoresComparator.logger.log(scoreFileName + " does not exist, skipping scores comnparison for " + scoreName);
            return;
        }
        Scanner input = new Scanner(file);

        boolean isReadingStarted = false;
        PrintWriter printWriter = new PrintWriter(new FileWriter(getRegressionFile(currentDir, scoreName)));

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

            String fullTestName = ScoresComparator.TEST_PREFIX + scoreNameValue[0].trim();
            ScoresComparator.logger.logTC("##teamcity[testStarted name=\'" + fullTestName + "\']");

            if (curData == null || curData.get(fullTestName)==null) {
                ScoresComparator.logger.log("Cannot get value for " + fullTestName);
                continue;
            }
            float currentValue = curData.get(fullTestName);
            float referenceValue;
            try {
                referenceValue = Float.valueOf(scoreNameValue[1]);
            } catch (NumberFormatException e) {
                referenceValue = Float.valueOf(scoreNameValue[1].replace(',','.'));
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
            printWriter.printf(Locale.UK, "%-50s\t%7.2f\t%7.2f\t%6.2f\t%5.2f%n",
                    scoreNameValue[0], referenceValue, currentValue, diff, deviation);
            ScoresComparator.logger.logTC("##teamcity[testFinished name=\'" + fullTestName + "\' duration=\'" + diff + "\']");
        }
        printWriter.close();
    }

    public List<String> getScoreNamesList() {
        return Arrays.asList(SCORES_TO_PRINT);
    }

    public Map<String, Float> getScores(String scoreName) {
        Map<String, Float> map = new HashMap<>();

        for (int i = 0; i < scores.length - 1; i++) {

            if (!(scores[i + 1].compareToIgnoreCase(scoreName) == 0)) continue;

            for (int j = 0; j < metrics.length; j++)
                map.putIfAbsent(ScoresComparator.TEST_PREFIX + metrics[j].trim()
                        + "." + scores[i + 1], values[j][i+1]);
            break;
        }

        return map;
    }
}
