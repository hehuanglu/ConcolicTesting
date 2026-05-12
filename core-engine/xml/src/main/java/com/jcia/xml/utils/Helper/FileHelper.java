package com.jcia.xml.utils.Helper;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileHelper {

    // list all files from this path
    public static List<Path> listFiles(Path path) throws IOException {

        List<Path> result;
        try (Stream<Path> walk = Files.walk(path)) {
            result = walk.filter(Files::isRegularFile)
                    .collect(Collectors.toList());
        }
        return result;

    }

    public static List<String> listDirectories(Path path) {
        File file = new File(path.toString());
        String[] directories = file.list(new FilenameFilter() {
            @Override
            public boolean accept(File current, String name) {
                return new File(current, name).isDirectory();
            }
        });
        return List.of(directories);
    }

    public static Set<File> listAllSubDirs(File d) throws IOException {
        TreeSet<File> closed = new TreeSet<File>(new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return f1.toString().compareTo(f2.toString());
            }
        });
        Deque<File> open = new ArrayDeque<File>();
        open.push(d);
        closed.add(d);
        while ( ! open.isEmpty()) {
            d = open.pop();
            for (File f : d.listFiles()) {
                if (f.isDirectory() && ! closed.contains(f)) {
                    open.push(f);
                    closed.add(f);
                }
            }
        }
        return closed;
    }

    public static String getFileExtension(String fileName) {
        if (fileName == null) {
            throw new IllegalArgumentException("fileName must not be null!");
        }

        String extension = "";

        int index = fileName.lastIndexOf('.');
        if (index > 0) {
            extension = fileName.substring(index + 1);
        }

        return extension;

    }

}
