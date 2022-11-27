package com.jetbrains.JBRperf;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class ScoresComparator {

    private static void printUsage() {
        logger.log("Usage: perfcmp.sh [options] <test_results_cur> <test_results_ref> <results> <test_prefix> <noHeaders>");
        logger.log("Options:");
        logger.log(" -h, --help\tdisplay this help");
        logger.log(" -tc\tprint teacmity statistic");
        logger.log("test_results_cur - the file with  metrics values for the current measuring");
        logger.log("test_results_ref - the file with metrics values for the reference measuring");
        logger.log("results - results of comaprison");
        logger.log("test_prefix - specifys measuring type, makes sense for enabled -tc, by default no prefixes");
        logger.log("noHeaders - by default 1-st line contains headers");
        logger.log("deviation - permissible deviation by default deviation=0.1");
        logger.log("");
        logger.log("test_results_* files content should be in csv format with header and tab separator:");
        logger.log("The 1-st column is the test name");
        logger.log("The 2-st column is the test value");
        logger.log("");
        logger.log("Example:");
        logger.log("Test           Value");
        logger.log("Testname       51.54");
    }

    static boolean DO_TC_STATISTIC = false;
    static String CURRENT_TESTRESULTS_FILE;
    static String REFERENCE_TESTRESULT_FILES;
    static String COMPARISON_RESULT_FILE;
    static String TEST_PREFIX = "";
    static boolean IS_HEADER_INCLUDED = true;
    static String[] DEVIATIONS;
    static ResultInterpreter RESULT_INTERPRETER = ResultInterpreter.lower_better;

    static DataLogReader dataReader;
    static Logger logger = new Logger();
    static final String FAILED_SIGN = " * ";
    static final String PASSED_SIGN = "   ";


    static void readArgs(String[] args) {
        if (args.length < 3) {
            printUsage();
            System.exit(1);
        }
        String[] options = new String[args.length - 1];
        if (args[0].compareToIgnoreCase("-tc") == 0) {
            DO_TC_STATISTIC = true;
            System.arraycopy(args, 1, options, 0, options.length);
        }

        CURRENT_TESTRESULTS_FILE = options[0];
        REFERENCE_TESTRESULT_FILES = options[1];
        if (options.length > 2)
            COMPARISON_RESULT_FILE = options[2];
        if (options.length > 3)
            TEST_PREFIX = options[3];
        if (options.length > 4)
            if (!options[4].isEmpty())
                IS_HEADER_INCLUDED = options[4].compareToIgnoreCase("true") == 0;
        if (options.length > 5)
            DEVIATIONS = options[5].split(":");
        if (options.length > 6)
            if (!options[6].isEmpty())
                RESULT_INTERPRETER = ResultInterpreter.valueOf(options[6]);

        dataReader = new MapbenchLogReader();
    }

    private static void compareResults(String scoreName, String currentDir, String referenceDir, String cmpresDir,
                                       Float deviation) throws IOException {
        logger.log("processign logs for :" + scoreName);

        Map<String, Float> curData = dataReader.getScores(scoreName);

        String scoreFileName = dataReader.getScoreFile(referenceDir, scoreName);
        File file = new File(scoreFileName);
        if ( !file.exists()) {
            logger.log(scoreFileName + " does not exist, skipping scores comnparison for " + scoreName);
            return;
        }
        Scanner input = new Scanner(file);

        boolean isReadingStarted = false;
        PrintWriter printWriter = new PrintWriter(new FileWriter("regression_" + scoreName + ".txt"));

        while (input.hasNextLine()) {
            if (!isReadingStarted) {
                isReadingStarted = true;
                if (IS_HEADER_INCLUDED) {
                    printWriter.println(input.nextLine());
                    continue;
                }
            }

            String line = input.nextLine();
            String[] scoreNameValue = line.split("\t");

            String fullTestName = TEST_PREFIX + scoreNameValue[0].trim();
            logger.logTC("##teamcity[testStarted name=\'" + fullTestName + "\']");

            if (curData == null || curData.get(fullTestName)==null) {
                logger.log("Cannot get value for " + fullTestName);
                continue;
            }
            float currentValue = curData.get(fullTestName);
            float referenceValue;
            try {
                referenceValue = Float.valueOf(scoreNameValue[1]);
            } catch (NumberFormatException e) {
                referenceValue = Float.valueOf(scoreNameValue[1].replace(',','.'));
            }
            float diff = (referenceValue != 0) ? (100 - currentValue / referenceValue * 100) : Float.NaN;

            boolean failed = false;
            if (RESULT_INTERPRETER == ResultInterpreter.higher_better)
                failed = (currentValue < referenceValue * (1 + deviation));
            else
                failed = (currentValue > referenceValue * (1 + deviation));

            logger.logTCf("##teamcity[buildStatisticValue key=\'%s\' value=\'%f\']\n", fullTestName, currentValue);
            logger.logf("buildStatisticValue key=\'%s\' value=\'%7.2f'\n", fullTestName, currentValue);
            if (failed) {
                printWriter.print(FAILED_SIGN);
                logger.logTCf("##teamcity[testFailed name=\'%s\' message=\'currentValue=%7.2f referenceValue=%7.2f diff=%6.2f\']\n", fullTestName, currentValue, referenceValue, diff);
                logger.logf("***testFailed name=\'%s\' currentValue=%7.2f referenceValue=%7.2f diff=%6.2f\n",fullTestName, currentValue, referenceValue, diff);
            } else {
                printWriter.print(PASSED_SIGN);
            }
            printWriter.printf(Locale.UK, "%-50s\t%7.2f\t%7.2f\t%6.2f\t%5.2f%n",
                    scoreNameValue[0], referenceValue, currentValue, diff, deviation);
            logger.logTC("##teamcity[testFinished name=\'" + fullTestName + "\' duration=\'" + diff + "\']");
        }
        printWriter.close();
    }

    static int  scoreNumber = 0;

    public static void main(String[] args) throws IOException {

        readArgs(args);

        dataReader.readLog(CURRENT_TESTRESULTS_FILE);
        List<String> scoreNames = dataReader.getScorenNamesList();

        dataReader.storeScores("./");

        scoreNames.forEach((scoreName) -> {
            try {
                float deviation = Float.valueOf(DEVIATIONS[scoreNumber++]);

                compareResults(scoreName, "./", "prev", "./", deviation);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}