package com.jetbrains.JBRperf;

import java.io.*;
import java.util.*;

abstract public class OneScoreLogReader extends DataLogReader {

    ArrayList<String> metrics = new ArrayList<String>();
    HashMap<String, Float> values = new HashMap<String, Float>();

    protected int metricWidthMax = 50;

    protected OneScoreLogReader(String readerName) {
        super(readerName);
    }

    public List<String> getScoreNamesList() {return Collections.emptyList();}

    public Map<String, Float> getScores(String scoreName) {
        return values;
    }

    String getScoreFile(String path) {
        return getScoreFile(path, "");
    }

    abstract String getRegressionFile(String path);
    String getRegressionFile(String path, String score) {
        return getRegressionFile(path);
    }

    public List<String> storeScores(String outDir) throws IOException {

        ArrayList<String> scoreMetrics = new ArrayList<>();

        String fileName = getScoreFile(outDir);
        PrintWriter printWriter = new PrintWriter(new FileWriter(fileName));
        String measure = (ScoresComparator.RESULT_INTERPRETER == ResultInterpreter.higher_better) ? "FPS" : "ms";

        for (int i = 0; i < metrics.size(); i++) {
            float value = values.get(metrics.get(i));
            printWriter.println(metrics.get(i) +"\t" + value + "\t" + measure);
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
        PrintWriter printWriter = new PrintWriter(new FileWriter(getRegressionFile(currentDir)));

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
            printWriter.printf(Locale.UK, "%-" + metricWidthMax + "s\t%7.2f\t%7.2f\t%6.2f\t%5.2f%n",
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
