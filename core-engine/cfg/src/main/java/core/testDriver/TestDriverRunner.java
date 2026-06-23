package core.testDriver;

import core.FilePath;
import core.cmd.CommandLine;
import core.path.MarkedStatement;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public final class TestDriverRunner {
    private static double runtime;
    private static String output;

    private TestDriverRunner() {
    }

    public static List<MarkedStatement> newRunTestDriver(String testDriver, String fullyClonedClassName) throws IOException, InterruptedException {
        fullyClonedClassName = fullyClonedClassName.contains(".")
                ? fullyClonedClassName.substring(0, fullyClonedClassName.lastIndexOf('.'))
                : fullyClonedClassName;

        String path = FilePath.newTestDriverPath + "/" + fullyClonedClassName.replace(".", "/");
        writeDataToFile(testDriver, path + "/TestDriver.java");

        // Lấy Mockito ghép với TestDriver path
        String currentCp = System.getProperty("java.class.path");
        try {
            // Định vị chính xác file Mockito
            String mockitoPath = new java.io.File(org.mockito.Mockito.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();
            currentCp = currentCp + java.io.File.pathSeparator + mockitoPath;
        } catch (Exception e) {
            // Bỏ qua nếu không tìm thấy
        }

        String fullCp = FilePath.newTestDriverPath + java.io.File.pathSeparator + currentCp;
        try {
            ProcessBuilder pbCompile = new ProcessBuilder("javac", "-cp", fullCp, path + "/TestDriver.java");
            pbCompile.redirectErrorStream(true);
            Process pCompile = pbCompile.start();

            try (BufferedReader r = new BufferedReader(new java.io.InputStreamReader(pCompile.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    System.out.println("[COMPILE LOG] " + line);
                }
            }

            boolean compileFinished = pCompile.waitFor(1, java.util.concurrent.TimeUnit.MINUTES);

            if (!compileFinished) {
                pCompile.destroyForcibly();
                throw new RuntimeException("Compile timeout after 1 minute");
            }

            if (pCompile.exitValue() != 0) {
                throw new RuntimeException("Compilation failed");
            }

            //chatgpt
            ProcessBuilder pbRun = new ProcessBuilder("java", "-cp", fullCp, fullyClonedClassName + ".TestDriver");
            pbRun.redirectErrorStream(true);
            Process pRun = pbRun.start();

            StringBuilder runLog = new StringBuilder();
            Thread outputThread = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(new java.io.InputStreamReader(pRun.getInputStream()))) {
                            String line;
                            while ((line = r.readLine()) != null) {
                                synchronized (runLog) {
                                    runLog.append(line).append('\n');
                                }

                                System.out.println("[TEST DRIVER LOG] " + line);
                            }

                        } catch (IOException ignored) {
                        }
                    });
            outputThread.start();

            //chatgpt
            boolean runFinished = pRun.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);

            if (!runFinished) {
                pRun.destroyForcibly();
                outputThread.join(2000);
                throw new RuntimeException("Execution timeout after 1 minute");
            }

            outputThread.join();
            if (pRun.exitValue() != 0) {
                throw new RuntimeException("Execution failed");
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

        return getMarkedStatement();
    }

    public static List<MarkedStatement> runTestDriver(String testDriver) throws IOException, InterruptedException {
        writeDataToFile(testDriver, FilePath.testDriverPath);

        CommandLine.executeCommand("javac " + FilePath.testDriverPath);
        CommandLine.executeCommand("java " + FilePath.testDriverPath);

        return getMarkedStatement();
    }

    private static List<MarkedStatement> getMarkedStatement() {
        List<MarkedStatement> result = new ArrayList<>();

        String markedData = getDataFromFile(FilePath.concreteExecuteResultPath);
        if (markedData == null || markedData.isBlank()) return result;
        String[] markedStatements = markedData.split("---end---");
        for (int i = 0; i < markedStatements.length; i++) {
            String[] markedStatementData = markedStatements[i].split("===", -1);
            if (i == markedStatements.length - 1) {
                if (markedStatementData.length == 0 || markedStatementData[0].isBlank()) {
                    continue; // bỏ qua dòng trống
                }
                runtime = Double.parseDouble(markedStatementData[0]);
                output = markedStatementData[1];
            } else {
                String statement = markedStatementData[0];
                boolean isTrueConditionalStatement = Boolean.parseBoolean(markedStatementData[1]);
                boolean isFalseConditionalStatement = Boolean.parseBoolean(markedStatementData[2]);
                int id = Integer.parseInt(markedStatementData[3]);
                MarkedStatement uncheckedMarkedStatement = new MarkedStatement(statement, isTrueConditionalStatement, isFalseConditionalStatement, id);
                if (!result.contains(uncheckedMarkedStatement)) {
                    result.add(uncheckedMarkedStatement);
                }
            }
        }
        return result;
    }

    public static double getRuntime() {
        return runtime;
    }

    public static String getOutput() {
        return output;
    }

    private static String getDataFromFile(String path) {
        StringBuilder result = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            if ((line = br.readLine()) != null) {
                result.append(line);
            }
            while ((line = br.readLine()) != null) {
                result.append("\n").append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    private static void writeDataToFile(String data, String path) {
        try {
            FileWriter writer = new FileWriter(path);
            writer.write(data);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
