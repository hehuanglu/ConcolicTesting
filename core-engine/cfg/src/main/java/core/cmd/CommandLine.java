package core.cmd;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandLine {

    public static void executeCommand(String command) throws IOException, InterruptedException {
        String os = System.getProperty("os.name").toLowerCase();
        String javaHome = System.getProperty("java.home");
        String binDir = javaHome + File.separator + "bin";

        // Tách command ra thành mảng để ProcessBuilder hiểu từng tham số
        List<String> parts = new ArrayList<>(Arrays.asList(command.split(" ")));
        String cmdType = parts.get(0);

        ProcessBuilder builder;

        if (cmdType.equals("javac") || cmdType.equals("java")) {
            // 1. Chỉ định chính xác file exe (ProcessBuilder không cần ngoặc kép)
            String exeName = os.contains("win") ? cmdType + ".exe" : cmdType;
            parts.set(0, binDir + File.separator + exeName);

            String cp = System.getProperty("java.class.path");

            try {
                String mockitoPath = new File(org.mockito.Mockito.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();
                cp = cp + File.pathSeparator + mockitoPath;
            } catch (Throwable t) {
                System.out.println("Cảnh báo: Không thể lấy đường dẫn thực của Mockito.");
            }

            // 4. Nhét tham số -cp vào
            parts.add(1, "-cp");
            parts.add(2, cp + File.pathSeparator + ".");

            // 5. Truyền trực tiếp List<String> vào ProcessBuilder để phá vỡ mọi giới hạn độ dài
            builder = new ProcessBuilder(parts);
        } else {
            // Các lệnh khác (dir, mkdir...) thì chạy qua cmd
            if (os.contains("win")) {
                builder = new ProcessBuilder("cmd.exe", "/c", String.join(" ", parts));
            } else {
                builder = new ProcessBuilder("sh", "-c", String.join(" ", parts));
            }
        }

        builder.redirectErrorStream(true);
        Process process = builder.start();

        try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) {
                System.out.println(line);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.out.println("Process exited with code = " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (process.isAlive()) {
                process.destroyForcibly();
            }
            throw e;
        }
    }

    public static void executeCommand(String command, String cdFolder) throws IOException, InterruptedException {
        Process p = Runtime.getRuntime().exec(command, null, new File(cdFolder));
        p.waitFor();
    }

    public static void main(String[] args) throws Exception {
        String command = "javac D:\\Haivt\\gen-test\\JGT-workspace\\instrument\\findGCD\\Solution.java";
        Process p = Runtime.getRuntime().exec(command);
        p.waitFor();
    }

}