package com.jetbrains.JBRperf;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

enum ResultInterpreter {
    higher_better,
    lower_better
}

abstract public class DataLogReader {

    abstract public void readLog(String logFilePath) throws FileNotFoundException;

    abstract public List<String> getScorenNamesList();

    abstract public List<String> storeScores(String outDir) throws IOException;
    abstract public Map<String, Float> getScores(String scoreName);
    abstract String getScoreFile(String path, String score);
}
