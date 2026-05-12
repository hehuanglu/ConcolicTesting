package com.jcia.xml.utils.Helper;

import java.util.Arrays;
import java.util.List;

public class StringHelper {

    public static List<String> SUPPORTED_EXTENSIONS = Arrays.asList("xml", "xhtml", "jsp");

    public static String strip(String input) {
        return input.trim();
    }

    public static String normalizeJavaClassPath(String javaClassPath) {
        String className = javaClassPath.substring(javaClassPath.lastIndexOf(".") + 1, javaClassPath.length());
        return javaClassPath.replace(".", "/") + ".java/" + className;
    }


}
