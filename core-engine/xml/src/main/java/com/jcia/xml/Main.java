package com.jcia.xml;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.jcia.xml.dom.Node;
import com.jcia.xml.parser.XmlFileParser;
import com.jcia.xml.utils.Helper.FileHelper;
import com.jcia.xml.utils.Helper.StringHelper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Main {

    private static final ExecutorService THREADPOOL_FIXED_SIZE = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        String folderPath = "C:/Users/thanh/Documents/jcia/project-test/web-budget/web-budget";
        List<Node> xmlNodes = new ArrayList<>();
        Path path = Paths.get(folderPath);
        List<Path> paths = FileHelper.listFiles(path);

        /**
         * Submit callable task to ThreadPool
         */
        List<Future<Node>> xmlNodeFutures = new ArrayList<>();
        paths.forEach(x -> {
            if(StringHelper.SUPPORTED_EXTENSIONS.contains(FileHelper.getFileExtension(x.toString()))){
                Future<Node> future = null;
                future = THREADPOOL_FIXED_SIZE.submit(new XmlFileParser(x.toString()));
                xmlNodeFutures.add(future);
            }
        });

        for(Future<Node> future : xmlNodeFutures) {
            Node parsedNode = future.get();
            xmlNodes.add(parsedNode);
        }

        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = ow.writeValueAsString(xmlNodes);

        //Parse project (submit each node into future - multithreading)
        System.out.println(json);

    }

}
