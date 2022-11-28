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

    DataLogReader(String readerName) {
        this.readerName = readerName;
    }

    abstract public void readLog(String logFilePath) throws FileNotFoundException;

    abstract public List<String> getScoreNamesList();

    abstract public List<String> storeScores(String outDir) throws IOException;
    abstract public Map<String, Float> getScores(String scoreName);
    abstract String getScoreFile(String path, String score);
    abstract String getRegressionFile(String path, String score);

    abstract void compareResults(String scoreName, String currentDir, String referenceDir, String cmpresDir,
                                Float deviation) throws IOException;
    abstract void compareResults(String currentDir, String referenceDir, String cmpresDir,
                                 Float deviation) throws IOException;

    private String readerName;
    public String getLogReaderName() {return readerName;}
}
