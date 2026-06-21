package com.example.fullTesting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Runs on profile {@code full} (port 8007). Calls unit-testing on {@link #unitTestingBaseUrl} (default 8006).
 * Between each heavy {@code /unit} call, optionally triggers {@code /restartService} and waits until {@code /health} is UP.
 */
@Service
public class FullTestingService {

    private final RestTemplate restTemplate;

    @Value("${unittesting.base-url:http://localhost:8006}")
    private String unitTestingBaseUrl;

    @Value("${fulltesting.tool-root:${user.dir}}")
    private String toolRoot;

    @Value("${fulltesting.restart-before-each-request:true}")
    private boolean restartBeforeEachRequest;

    @Value("${fulltesting.fallback-shell-restart:false}")
    private boolean fallbackShellRestart;

    @Value("${fulltesting.unit-testing-module-dir:processing-service/unitTesting}")
    private String unitTestingModuleRelativeDir;

    public FullTestingService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /** Runs once after FullTesting (port 8007) is up. Restart the app to run the suite again. */
    @EventListener(ApplicationReadyEvent.class)
    public void mainService() {
        String nameProject = "stringMethods";

        if (!waitForUnitTestingReady(20, 1000)) {
            System.err.println("⚠ Unit testing is not reachable at " + unitTestingBaseUrl);
            if (fallbackShellRestart) {
                try {
                    shellRestartUnitTesting();
                    if (!waitForUnitTestingReady(40, 2000)) {
                        System.err.println("❌ Unit testing still not ready after shell restart.");
                        return;
                    }
                } catch (Exception e) {
                    System.err.println("❌ Shell restart failed: " + e.getMessage());
                    return;
                }
            } else {
                System.err.println("   Start it: cd processing-service/unitTesting && mvn spring-boot:run");
                System.err.println("   Or set fulltesting.fallback-shell-restart=true (not recommended).");
                return;
            }
        }

        try {
            Path jsonFile = Paths.get(toolRoot, "project", "anonymous", "tmp-prj",
                    nameProject + ".zip.project", "tmp-prjt.json");
            if (!Files.isRegularFile(jsonFile)) {
                System.err.println("❌ JSON not found: " + jsonFile.toAbsolutePath());
                return;
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode data = objectMapper.readTree(jsonFile.toFile());

            List<Integer> methodIds = new ArrayList<>();
            collectMethodIds(data.get("rootNode"), methodIds, null);

            Path logPath = resolveUnderToolRoot(unitTestingModuleRelativeDir)
                    .resolve("data")
                    .resolve("all.txt");

            String startupLog =
                            "\n========================================\n" +
                            "Start Time             : " + java.time.LocalDateTime.now() + "\n" +
                            "========================================\n";

            Files.createDirectories(logPath.getParent());

            Files.write(
                    logPath,
                    startupLog.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

            for (int id : methodIds) {
                if (restartBeforeEachRequest) {
                    requestUnitTestingRestart();
                    if (!waitForUnitTestingReady(120, 500)) {
                        System.err.println("❌ Unit testing not ready after restart, skip id=" + id);
                        continue;
                    }
                }

                unitTesting(nameProject, "branch", id);

                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

        } catch (Exception e) {
            System.err.println("❌ mainService: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String unitApiPrefix() {
        return trimTrailingSlash(unitTestingBaseUrl) + "/api/unit-testing-service";
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private boolean waitForUnitTestingReady(int maxRetries, long delayMillis) {
        String healthUrl = unitApiPrefix() + "/health";
        for (int i = 0; i < maxRetries; i++) {
            try {
                ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    // health() returns 503 when restarting
                    return true;
                }
            } catch (Exception ignored) {
                // still starting or restarting
            }
            sleepQuietly(delayMillis);
        }
        return false;
    }


    private void requestUnitTestingRestart() {
        try {
            restTemplate.getForEntity(unitApiPrefix() + "/restartService", Void.class);
        } catch (Exception e) {
            System.err.println("⚠ restartService request failed: " + e.getMessage());
        }
        // Restart is async; avoid a false-positive health on the dying context.
        sleepQuietly(1500);
    }

    private void shellRestartUnitTesting() throws IOException, InterruptedException {
        int port = unitTestingPort();
        stopServiceOnPort(port);
        Path moduleDir = resolveUnderToolRoot(unitTestingModuleRelativeDir);
        ProcessBuilder pb = new ProcessBuilder("mvn", "spring-boot:run");
        pb.directory(moduleDir.toFile());
        pb.inheritIO();
        pb.start();
        TimeUnit.SECONDS.sleep(15);
        System.out.println("♻ Shell restart of unit testing launched from " + moduleDir.toAbsolutePath());
    }

    private void stopServiceOnPort(int port) throws IOException, InterruptedException {
        ProcessBuilder findPid = new ProcessBuilder("lsof", "-ti", "tcp:" + port);
        Process findProcess = findPid.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(findProcess.getInputStream()))) {
            String pid;
            while ((pid = reader.readLine()) != null) {
                System.out.println("⚠ Killing PID " + pid + " on port " + port);
                new ProcessBuilder("kill", "-9", pid).start().waitFor();
            }
        }
        findProcess.waitFor();
    }

    private void unitTesting(String name, String criterion, int targetId) {
        String url = buildUrl(name, criterion, targetId);
        System.out.println("🚀 Testing URL: " + url);

        Path path = resolveUnderToolRoot(unitTestingModuleRelativeDir).resolve("data").resolve("all.txt");
        String logLine;

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && responseBody.containsKey("body")) {
                Map<String, Object> data = (Map<String, Object>) responseBody.get("body");
                String methodName = (String) data.getOrDefault("methodName", "N/A");
                double coverage = data.get("fullCoverage") != null ? ((Number) data.get("fullCoverage")).doubleValue() : 0.0;
                double executionTime = data.get("executionTime") != null ? ((Number) data.get("executionTime")).doubleValue() : 0.0;
                double memoryUsage = data.get("memoryUsage") != null ? ((Number) data.get("memoryUsage")).doubleValue() : 0.0;
                List<?> fullTestDataSet = data.get("fullTestDataSet") instanceof List ? (List<?>) data.get("fullTestDataSet") : List.of();
                int testDataSize = fullTestDataSet.size();

                logLine = String.format("%s | %s | %.2f%% | %.2fms | %.2fMB | %d cases%n",
                        methodName, criterion, coverage, executionTime, memoryUsage, testDataSize);

                StringBuilder testDataLog = new StringBuilder("TestData: ");
                for (Object testCase : fullTestDataSet) {
                    testDataLog.append(testCase.toString()).append(" ; ");
                }
                testDataLog.append("\n");
                logLine += testDataLog.toString();

            } else {
                logLine = String.format("%s | ERROR | 0 | 0 | 0 | No body data%n", criterion);
            }

        } catch (Exception e) {
            logLine = String.format("%s | TIMEOUT/ERROR | 0 | 0 | 0 | %s%n", criterion, e.getMessage());
            System.err.println("❌ Lỗi: " + e.getMessage());
        }

        try {
            Files.createDirectories(path.getParent());
            Files.write(path, logLine.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            System.out.print("✅ Logged: " + logLine);
        } catch (IOException ioException) {
            System.err.println("❌ Lỗi ghi file: " + ioException.getMessage());
        }
    }

    private void collectMethodIds(JsonNode node, List<Integer> methodIds, String currentFileName) {
        if (node == null) {
            return;
        }

        if (node.has("entityClass") &&
                "JavaNode".equals(node.get("entityClass").asText())) {

            currentFileName = node.get("simpleName").asText();
        }

        if (node.has("entityClass") &&
                "JavaMethodNode".equals(node.get("entityClass").asText())) {

            if (currentFileName == null || !currentFileName.matches(".*\\d+\\.java$")) {
                int id = node.get("id").asInt();
                methodIds.add(id);
            }
        }

        if (node.has("children")) {
            for (JsonNode child : node.get("children")) {
                collectMethodIds(child, methodIds, currentFileName);
            }
        }
    }

    private String buildUrl(String nameProject, String coverageType, int targetId) {
        return unitApiPrefix() + "/unit"
                + "?nameProject=" + nameProject + ".zip.project"
                + "&coverageType=" + coverageType
                + "&targetId=" + targetId;
    }

    private Path resolveUnderToolRoot(String relativePath) {
        Path base = Paths.get(toolRoot).toAbsolutePath().normalize();
        for (String seg : relativePath.split("/")) {
            if (!seg.isEmpty()) {
                base = base.resolve(seg);
            }
        }
        return base;
    }

    private int unitTestingPort() {
        try {
            String u = trimTrailingSlash(unitTestingBaseUrl);
            if (u.contains(":")) {
                int lastColon = u.lastIndexOf(':');
                String after = u.substring(lastColon + 1);
                int slash = after.indexOf('/');
                String portPart = slash >= 0 ? after.substring(0, slash) : after;
                return Integer.parseInt(portPart);
            }
        } catch (Exception ignored) {
            // fall through
        }
        return 8006;
    }

    private static void sleepQuietly(long delayMillis) {
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

