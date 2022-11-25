package com.jetbrains.JBRperf;

import java.io.*;
import java.util.*;

import static com.jetbrains.JBRperf.ScoresComparator.TEST_PREFIX;

public class MapbenchLogReader extends DataLogReader {

    private ArrayList<String> valuableLines = new ArrayList<String>(); // rows
    private Float[][] values;

    private String[] scores; //header
    private String[] metrics; // rows

    public static final String OUT_FILE_PREFIX = "mapbench_tests_";
    private static final String[] SCORES_TO_PRINT = {
            "Pct95",
            "Avg",
            "StdDev",
            "Min",
            "Max"
    };

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

    String getScoreFile(String path, String score) {
        return path + File.separator + OUT_FILE_PREFIX + score + ".txt";
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

        for (int i = 0; i < scores.length - 1; i++) {

            if (!scores_to_print.contains(scores[i + 1])) continue;

            String fileName = getScoreFile(outDir, scores[i + 1]);
            PrintWriter printWriter = new PrintWriter(new FileWriter(fileName));
            printWriter.println(scores[0] + "\t" + scores[i + 1]);
            for (int j = 0; j < metrics.length; j++)
                printWriter.printf("%-50s\t%7.2f%n", metrics[j].trim() + "." + scores[i + 1], values[j][i+1]);

            printWriter.close();
            scoreMetrics.add(fileName);
        }
        return scoreMetrics;
    }

    public List<String> getScorenNamesList() {
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
