package core.z3;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Loads Z3 native libraries from the project bundle before Context is created. */
public final class Z3NativeLoader {

    /** Survives Spring DevTools restart: new classloader, same JVM. */
    private static final String JVM_LOADED_FLAG = "tool.z3.native.loaded";

    /** Bật/tắt log debug: -Dz3.native.debug=true hoặc false */
    private static final String DEBUG_PROPERTY = "z3.native.debug";

    private static volatile boolean loaded;

    private Z3NativeLoader() {
    }

    public static void load() {
        debug("===== START Z3 NATIVE LOADER =====");
        debug("os.name = " + System.getProperty("os.name"));
        debug("os.arch = " + System.getProperty("os.arch"));
        debug("user.dir = " + System.getProperty("user.dir"));
        debug("z3.native.dir property = " + System.getProperty("z3.native.dir"));
        debug("Z3_NATIVE_DIR env = " + System.getenv("Z3_NATIVE_DIR"));

        if (loaded) {
            ok("Z3 already loaded in this classloader. Skip.");
            return;
        }

        synchronized (Z3NativeLoader.class) {
            if (loaded) {
                ok("Z3 already loaded after synchronized check. Skip.");
                return;
            }

            if ("true".equals(System.getProperty(JVM_LOADED_FLAG))) {
                loaded = true;
                ok("Z3 already loaded in this JVM. Skip native loading.");
                return;
            }

            try {
                Path binDir = resolveBinDir();
                ok("Resolved Z3 bin directory: " + binDir);

                loadNativeLibraries(binDir);

                System.setProperty("z3.native.dir", binDir.toString());
                System.setProperty(JVM_LOADED_FLAG, "true");

                loaded = true;

                ok("Z3 native libraries loaded successfully from: " + binDir);
                debug("===== END Z3 NATIVE LOADER: SUCCESS =====");

            } catch (UnsatisfiedLinkError e) {
                error("Z3 native loading FAILED.");
                error("Reason: " + e.getMessage());
                e.printStackTrace(System.err);
                debug("===== END Z3 NATIVE LOADER: FAILED =====");
                throw e;
            } catch (RuntimeException e) {
                error("Unexpected runtime error while loading Z3.");
                error("Reason: " + e.getMessage());
                e.printStackTrace(System.err);
                debug("===== END Z3 NATIVE LOADER: FAILED =====");
                throw e;
            }
        }
    }

    private static Path resolveBinDir() {
        debug("Resolving Z3 bin directory...");

        // 1. Ưu tiên -Dz3.native.dir (trỏ thẳng vào /bin)
        String fromProperty = System.getProperty("z3.native.dir");
        if (fromProperty != null && !fromProperty.isBlank()) {
            Path dir = Paths.get(fromProperty).toAbsolutePath().normalize();
            debug("Checking -Dz3.native.dir: " + dir);

            if (isZ3BinDir(dir)) {
                ok("Valid Z3 bin dir from system property: " + dir);
                return dir;
            }

            warn("Invalid Z3 bin dir from system property.");
            warn(describeBinDir(dir));
        } else {
            debug("No -Dz3.native.dir provided.");
        }

        // 2. Fallback: biến môi trường Z3_NATIVE_DIR
        String fromEnv = System.getenv("Z3_NATIVE_DIR");
        if (fromEnv != null && !fromEnv.isBlank()) {
            Path dir = Paths.get(fromEnv).toAbsolutePath().normalize();
            debug("Checking Z3_NATIVE_DIR env: " + dir);

            if (isZ3BinDir(dir)) {
                ok("Valid Z3 bin dir from environment variable: " + dir);
                return dir;
            }

            warn("Invalid Z3 bin dir from environment variable.");
            warn(describeBinDir(dir));
        } else {
            debug("No Z3_NATIVE_DIR env provided.");
        }

        // 3. Fallback: tìm tự động trong project
        Path fromProject = findProjectBundle();
        if (fromProject != null) {
            ok("Valid Z3 bin dir found from project bundle: " + fromProject);
            return fromProject;
        }

        throw new UnsatisfiedLinkError(
                "Could not find Z3 native libraries. "
                        + "Set -Dz3.native.dir or Z3_NATIVE_DIR. "
                        + "Expected files: " + String.join(", ", expectedNativeFileNames())
        );
    }

    private static Path findProjectBundle() {
        Path cwd = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        debug("Searching Z3 bundle from cwd: " + cwd);

        String z3Prefix = expectedZ3FolderPrefix();
        debug("Expected Z3 folder prefix for current OS/arch: " + z3Prefix);

        // Bước 1: Scan XUỐNG từ cwd (recursive, tối đa 6 cấp)
        // Xử lý trường hợp user.dir là workspace root, project nằm ở subfolder sâu hơn
        debug("Phase 1: Descending from cwd...");
        Path found = findZ3BinDescending(cwd, z3Prefix, 6);
        if (found != null) {
            ok("Found Z3 bin by descending from cwd: " + found);
            return found;
        }

        // Bước 2: Scan LÊN từ cwd (tối đa 8 cấp cha)
        // Xử lý trường hợp user.dir là chính project hoặc nằm trong project
        debug("Phase 2: Ascending from cwd...");
        Path scanRoot = cwd.getParent();
        for (int depth = 1; depth < 8 && scanRoot != null; depth++) {
            debug("Scanning parent depth " + depth + ": " + scanRoot);

            // Tìm trong thư mục z3/ con của scanRoot
            Path z3SubDir = scanRoot.resolve("z3");
            if (Files.isDirectory(z3SubDir)) {
                debug("Found z3/ subdirectory: " + z3SubDir);
                Path result = findZ3BinUnder(z3SubDir, z3Prefix);
                if (result != null) {
                    ok("Found Z3 bin inside z3/ subdirectory: " + result);
                    return result;
                }
            }

            // Tìm trực tiếp trong scanRoot
            Path result = findZ3BinUnder(scanRoot, z3Prefix);
            if (result != null) {
                ok("Found Z3 bin under parent scan: " + result);
                return result;
            }

            scanRoot = scanRoot.getParent();
        }

        warn("No Z3 bundle found in project tree.");
        return null;
    }

    /**
     * Scan đệ quy XUỐNG từ {@code dir}, tìm thư mục có tên bắt đầu bằng {@code prefix}.
     * Tìm trong thư mục z3/ trước, sau đó scan trực tiếp, rồi đệ quy vào các subfolder.
     * Giới hạn độ sâu bằng {@code maxDepth} để tránh scan toàn bộ filesystem.
     */
    private static Path findZ3BinDescending(Path dir, String prefix, int maxDepth) {
        if (maxDepth < 0 || !Files.isDirectory(dir)) {
            return null;
        }

        // Tìm trong thư mục z3/ con của dir này
        Path z3SubDir = dir.resolve("z3");
        if (Files.isDirectory(z3SubDir)) {
            debug("Descend: checking z3/ subdirectory: " + z3SubDir);
            Path found = findZ3BinUnder(z3SubDir, prefix);
            if (found != null) return found;
        }

        // Tìm trực tiếp trong dir (có thể chứa folder z3-4.16.0-... ngay tại đây)
        Path found = findZ3BinUnder(dir, prefix);
        if (found != null) return found;

        // Đệ quy vào các subfolder (bỏ qua các thư mục ẩn và thư mục build phổ biến)
        try (DirectoryStream<Path> children = Files.newDirectoryStream(dir)) {
            for (Path child : children) {
                if (!Files.isDirectory(child)) continue;

                String name = child.getFileName().toString();
                // Bỏ qua hidden dirs, target/, node_modules/, .git/, ...
                if (name.startsWith(".") || name.equals("target")
                        || name.equals("node_modules") || name.equals("build")
                        || name.equals("out") || name.equals("bin")) {
                    continue;
                }

                Path result = findZ3BinDescending(child, prefix, maxDepth - 1);
                if (result != null) return result;
            }
        } catch (IOException e) {
            warn("Cannot scan directory: " + dir + ". Reason: " + e.getMessage());
        }

        return null;
    }

    /**
     * Trả về prefix của thư mục Z3 theo OS/arch hiện tại.
     * Dùng prefix thay vì tên đầy đủ để khớp với mọi phiên bản
     * (vd: "z3-4.16.0-x64-glibc-2.39", "z3-4.16.0-x64-glibc-2.35", ...).
     */
    private static String expectedZ3FolderPrefix() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String arch = System.getProperty("os.arch", "").toLowerCase();

        boolean arm64 = arch.equals("aarch64") || arch.equals("arm64");
        boolean x64 = arch.equals("x86_64") || arch.equals("amd64");

        if (os.contains("mac") && arm64) return "z3-4.16.0-arm64-osx";
        if (os.contains("mac") && x64)   return "z3-4.16.0-x64-osx";
        if (os.contains("win") && x64)   return "z3-4.16.0-x64-win";
        if (os.contains("linux") && x64) return "z3-4.16.0-x64-glibc";
        if (os.contains("linux") && arm64) return "z3-4.16.0-arm64-glibc";

        throw new UnsatisfiedLinkError(
                "Unsupported OS/architecture for Z3: os=" + os + ", arch=" + arch
        );
    }

    /**
     * Quét trong {@code root} các thư mục có tên bắt đầu bằng {@code folderPrefix},
     * trả về đường dẫn bin/ hợp lệ đầu tiên tìm được.
     */
    private static Path findZ3BinUnder(Path root, String folderPrefix) {
        try (DirectoryStream<Path> dirs = Files.newDirectoryStream(root, folderPrefix + "*")) {
            for (Path z3Root : dirs) {
                debug("Found z3-like directory: " + z3Root);

                if (!Files.isDirectory(z3Root)) {
                    debug("Skip because not directory: " + z3Root);
                    continue;
                }

                Path bin = z3Root.resolve("bin").toAbsolutePath().normalize();
                debug("Checking scanned bin dir: " + bin);

                if (isZ3BinDir(bin)) {
                    return bin;
                }

                warn("Scanned z3 folder but bin is invalid: " + bin);
                warn(describeBinDir(bin));
            }
        } catch (IOException e) {
            warn("Cannot scan directory: " + root + ". Reason: " + e.getMessage());
        }

        return null;
    }

    private static boolean isZ3BinDir(Path binDir) {
        if (!Files.isDirectory(binDir)) {
            return false;
        }

        for (String fileName : expectedNativeFileNames()) {
            if (!Files.isRegularFile(binDir.resolve(fileName))) {
                return false;
            }
        }

        return true;
    }

    private static void loadNativeLibraries(Path binDir) {
        String os = System.getProperty("os.name", "").toLowerCase();
        debug("Loading native libraries from: " + binDir);

        try {
            if (os.contains("mac")) {
                loadLibrary(binDir.resolve("libz3.dylib"));
                loadLibrary(binDir.resolve("libz3java.dylib"));
                return;
            }

            if (os.contains("linux")) {
                loadLibrary(binDir.resolve("libz3.so"));
                loadLibrary(binDir.resolve("libz3java.so"));
                return;
            }

            if (os.contains("win")) {
                loadLibrary(binDir.resolve("libz3.dll"));
                loadLibrary(binDir.resolve("libz3java.dll"));
                return;
            }
        } catch (UnsatisfiedLinkError e) {
            if (isAlreadyLoadedInJvm(e)) {
                warn("Native library already loaded in JVM. Treat as success.");
                return;
            }

            throw e;
        }

        throw new UnsatisfiedLinkError("Unsupported OS for Z3 native loading: " + os);
    }

    private static void loadLibrary(Path library) {
        Path absoluteLibrary = library.toAbsolutePath().normalize();
        debug("Trying System.load: " + absoluteLibrary);

        if (!Files.isRegularFile(absoluteLibrary)) {
            throw new UnsatisfiedLinkError(
                    "Native library file does not exist: " + absoluteLibrary
            );
        }

        try {
            System.load(absoluteLibrary.toString());
            ok("Loaded native library: " + absoluteLibrary.getFileName());

        } catch (UnsatisfiedLinkError e) {
            if (isAlreadyLoadedInJvm(e)) {
                warn("Already loaded in JVM: " + absoluteLibrary.getFileName());
                return;
            }

            error("Failed to load native library: " + absoluteLibrary);
            error("Original reason: " + e.getMessage());

            UnsatisfiedLinkError wrapped = new UnsatisfiedLinkError(
                    "Failed to load native library: " + absoluteLibrary
                            + System.lineSeparator()
                            + "Reason: " + e.getMessage()
            );
            wrapped.initCause(e);
            throw wrapped;
        }
    }

    private static String[] expectedNativeFileNames() {
        String os = System.getProperty("os.name", "").toLowerCase();

        if (os.contains("mac"))   return new String[]{"libz3.dylib", "libz3java.dylib"};
        if (os.contains("linux")) return new String[]{"libz3.so",    "libz3java.so"};
        if (os.contains("win"))   return new String[]{"libz3.dll",   "libz3java.dll"};

        return new String[0];
    }

    private static String describeBinDir(Path binDir) {
        StringBuilder sb = new StringBuilder();

        sb.append("Directory check: ").append(binDir).append(System.lineSeparator());
        sb.append("exists = ").append(Files.exists(binDir)).append(System.lineSeparator());
        sb.append("isDirectory = ").append(Files.isDirectory(binDir)).append(System.lineSeparator());

        for (String fileName : expectedNativeFileNames()) {
            Path file = binDir.resolve(fileName);
            sb.append("required file: ")
                    .append(file)
                    .append(" | exists = ")
                    .append(Files.isRegularFile(file))
                    .append(System.lineSeparator());
        }

        return sb.toString();
    }

    private static boolean isAlreadyLoadedInJvm(UnsatisfiedLinkError error) {
        String message = error.getMessage();
        return message != null && message.toLowerCase().contains("already loaded");
    }

    private static boolean debugEnabled() {
        return Boolean.parseBoolean(System.getProperty(DEBUG_PROPERTY, "true"));
    }

    private static void debug(String message) {
        if (debugEnabled()) {
            System.out.println("[Z3][DEBUG] " + message);
        }
    }

    private static void ok(String message) {
        System.out.println("[Z3][OK] " + message);
    }

    private static void warn(String message) {
        if (debugEnabled()) {
            System.err.println("[Z3][WARN] " + message);
        }
    }

    private static void error(String message) {
        System.err.println("[Z3][ERROR] " + message);
    }
}
