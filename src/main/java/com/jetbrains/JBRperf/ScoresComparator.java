package com.jetbrains.JBRperf;

import java.io.IOException;
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

    static final private String [] delimiters = {"_", "\\."};

    static DataLogReader identifyLogReader() throws UnknownLogReaderException {
        DataLogReader reader = null;

        String readerName = "";
        for (String delimiter : delimiters) {
            String[] arr = CURRENT_TESTRESULTS_FILE.split(delimiter);
            if (arr.length > 0) {
                readerName = arr[0];
                switch (readerName) {
                    case "mapbench":
                        reader = new MapbenchLogReader();
                        break;
                    case "render":
                        reader = new RenderLogReader();
                        break;
                    case "J2DBench":
                        reader = new J2DBenchLogReader();
                        break;
                    case "dacapo":
                        reader = new DacapoLogReader();
                        break;
                    case "JavaDraw":
                        reader = new JavaDrawLogReader();
                        break;
                    case "LightBeam":
                        reader = new LightBeamLogReader();
                        break;
                }
            }
            if (reader != null)
                return reader;
        }
        throw new UnknownLogReaderException(readerName);
    }

    static void readArgs(String[] args) throws UnknownLogReaderException {
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

        dataReader = identifyLogReader();
    }

    static int  scoreNumber = 0;

    public static void main(String[] args) throws IOException, UnknownLogReaderException {

        readArgs(args);

        dataReader.readLog(CURRENT_TESTRESULTS_FILE);
        List<String> scoreNames = dataReader.getScoreNamesList();
        dataReader.storeScores("./");
        if (scoreNames.isEmpty())
            dataReader.compareResults("./", "prev", "./", Float.valueOf(DEVIATIONS[0]));
        else
            scoreNames.forEach((scoreName) -> {
                try {
                    float deviation = Float.valueOf(DEVIATIONS[scoreNumber++]);
                    dataReader.compareResults(scoreName, "./", "prev", "./", deviation);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
    }
}