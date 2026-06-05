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
        fullyClonedClassName = fullyClonedClassName.contains(".") ? fullyClonedClassName.substring(0, fullyClonedClassName.lastIndexOf('.')) : fullyClonedClassName;
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

        // Ghép toàn bộ vào chung 1 Classpath
        String fullCp = FilePath.newTestDriverPath + java.io.File.pathSeparator + currentCp;

        // CHẠY TRỰC TIẾP BẰNG PROCESS BUILDER
        try {
            // Lệnh Biên dịch (javac)
            ProcessBuilder pbCompile = new ProcessBuilder("javac", "-cp", fullCp, path + "/TestDriver.java");
            pbCompile.redirectErrorStream(true);
            Process pCompile = pbCompile.start();

            // Đọc log compile
            try (BufferedReader r = new BufferedReader(new java.io.InputStreamReader(pCompile.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    log.debug("[COMPILE LOG] {}", line);
                }
            }

            pCompile.waitFor();

            // Lệnh Chạy (java)
            ProcessBuilder pbRun = new ProcessBuilder("java", "-cp", fullCp, fullyClonedClassName + ".TestDriver");
            pbRun.redirectErrorStream(true);
            Process pRun = pbRun.start();

            // Đọc log khi chạy test (
            try (BufferedReader r = new BufferedReader(new java.io.InputStreamReader(pRun.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    log.debug("[TEST DRIVER LOG] {}", line);
                }
            }
            pRun.waitFor();

        } catch (Exception e) {
            e.printStackTrace();
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
            String[] markedStatementData = markedStatements[i].split("===");
            // Bỏ qua dòng trống
            if (markedStatementData.length == 0 || markedStatementData[0].isBlank()) {
                continue;
            }

            if (i == markedStatements.length - 1) {
                try {
                    // Nếu đây là rác, ta sẽ ném lỗi
                    runtime = Double.parseDouble(markedStatementData[0].trim());
                    if (markedStatementData.length > 1) {
                        output = markedStatementData[1];
                    }
                } catch (NumberFormatException e) {
                    log.info(" Bỏ qua rác ở khối runtime: {}", markedStatementData[0]);
                    runtime = 0.0;
                }
          }
            else {
                // Cờ chuẩn ta phải có ít nhất 3 phần. Ít hơn là rác, ta bỏ qua.
                if (markedStatementData.length < 3) continue;

                String statement = markedStatementData[0].trim();
                String trueStr = markedStatementData[1].trim().toLowerCase();
                String falseStr = markedStatementData[2].trim().toLowerCase();

                if (!trueStr.equals("true") && !trueStr.equals("false")) continue;
                if (!falseStr.equals("true") && !falseStr.equals("false")) continue;

                boolean isTrueConditionalStatement = Boolean.parseBoolean(trueStr);
                boolean isFalseConditionalStatement = Boolean.parseBoolean(falseStr);

                int id = -1;
                if (markedStatementData.length > 3) {
                    try {
                        id = Integer.parseInt(markedStatementData[3].trim());
                    } catch (NumberFormatException e) {
                        id = -1;
                    }
                }

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
