package com.example.unittesting.Sevice;


import com.example.unittesting.model.MethodTest;
import com.fasterxml.jackson.databind.node.ArrayNode;
import core.testGeneration.ConcolicTestGeneration.ConcolicTesting;
import core.testGeneration.ConcolicTestGeneration.ConcolicTestingWithStub.AS4UT;
import core.testGeneration.ConcolicTestGeneration.ConcolicTestingWithStub.ConcolicTestingWithStub4Libs;
import core.testGeneration.TestGeneration;
import core.entity.ParameterInput;
import core.testResult.result.autoTestResult.TestResult;

import com.example.unittesting.utils.findNode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import core.utils.Utils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


import java.io.File;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Service

public class UTestServiceImpl implements UTestService {
    private MethodTest methodTest;
    private List<ParameterInput> parameterInputs;

    @Override
    public ResponseEntity<Object> runAutomationTest(int targetId, String nameProject, TestGeneration.Coverage coverage) throws IOException {
        try {
            File jsonFile = new File("project/anonymous/tmp-prj/" + nameProject + "/tmp-prjt.json");
            if (!jsonFile.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Đường dẫn không tồn tại!");
            } else {
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    JsonNode data = objectMapper.readTree(jsonFile);
                    JsonNode nodeWithId = findNode.getNodeById(targetId, data.get("rootNode"));
                    if (nodeWithId.get("entityClass").asText().equals("JavaMethodNode")) {
                        System.out.println(nodeWithId + "\n");
                        String simpleName = nodeWithId.get("simpleName").asText();
                        String pathMethod = nodeWithId.get("path").asText();
                        System.out.println(pathMethod);
                        int openingParenthesisIndex = simpleName.indexOf("(");
                        String name = simpleName.substring(0, openingParenthesisIndex).trim();
                        File file = new File(pathMethod);
                        String className = file.getName();

                        // TEST TEMPLATE
//                        createMethodTest("project/anonymous/tmp-prj/" + nameProject + "/tmp-prjt.json", targetId);

                        TestResult result = ConcolicTestingWithStub4Libs.runFullConcolic(targetId, pathMethod, name, className, coverage);

                        return ResponseEntity.ok(result);
                    } else {
                        System.out.println("Node with id not JavaMethodNode.\n");
                        return ResponseEntity.ok(nodeWithId);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Error reading JSON file.\n");
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error reading JSON file.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error processing request. Exception: " + e.getMessage()+"\n");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request. Exception: " + e.getMessage());
        }
    }

    private final String JAVA_METHOD_NODE = "JavaMethodNode";

    @Override
    public ResponseEntity<Object> runRegressionTest(String nameProject, TestGeneration.Coverage coverage) throws IOException {
        try {
            String dirPath = "project/anonymous/tmp-prj/" + nameProject;
            File jsonFile = new File(dirPath + "/tmp-prj.json");
            if (!jsonFile.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Non exist file "+ jsonFile.getPath());
            } else {
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    JsonNode data = objectMapper.readTree(jsonFile);
                    List<TestResult> finalResult = new ArrayList<>();

                    ArrayNode changedNodes = (ArrayNode) data.get("changedNodes");

                    for (int i = 0; i < changedNodes.size(); i++) {
                        JsonNode changeNode = changedNodes.get(i);

                        if (changeNode.get("entityClass").asText().equals(JAVA_METHOD_NODE)) {

                            String path = changeNode.get("path").asText();
                            String name = changeNode.get("simpleName").asText();
                            String className = (new File(path)).getName();

                            TestResult result = ConcolicTesting.runFullConcolic(0, path, name, className, coverage);
                            finalResult.add(result);
                        }

                        System.out.println("abc");
                    }

                    return ResponseEntity.ok(finalResult);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Error reading JSON file.\n");
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error reading JSON file.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error processing request. Exception: " + e.getMessage()+"\n");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request. Exception: " + e.getMessage());
        }
    }@Override
    public ResponseEntity<Object> runAutomationTestFile(int targetId, String nameProject, TestGeneration.Coverage coverage) throws IOException {
        try {
            File jsonFile = new File("project/anonymous/tmp-prj/" + nameProject + "/tmp-prjt.json");
            if (!jsonFile.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("The path does not exist!");
            } else {
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    List<Object> results = new ArrayList<>();
                    JsonNode data = objectMapper.readTree(jsonFile);
                    JsonNode nodeWithId = findNode.getNodeById(targetId, data.get("rootNode"));
                    if (nodeWithId.get("entityClass").asText().equals("JavaNode")) {
                        JsonNode children = nodeWithId.get("children");
                        List<String> methodNames  = new ArrayList<>();
                        for (JsonNode child : children) {
                            if (child.get("entityClass").asText().equals("JavaMethodNode")) {
                                String methodName = child.get("simpleName").asText();
                                methodNames.add(methodName);
                                int openingParenthesisIndex = methodName.indexOf("(");
                                String pathMethod = nodeWithId.get("path").asText();
                                File file = new File(pathMethod);
                                String className = file.getName();
                                results.add(AS4UT.runFullConcolic(child.get("id").asInt(), pathMethod, methodName.substring(0, openingParenthesisIndex).trim(),className , coverage));
                            }
                        }

                        return ResponseEntity.ok(results);
                    } else {
                        System.out.println("Node with id not JavaNode.\n");
                        return ResponseEntity.ok(nodeWithId);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Error reading JSON file.\n");
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error reading JSON file.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error processing request. Exception: " + e.getMessage()+"\n");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request. Exception: " + e.getMessage());
        }
    }
    @Override
    public ResponseEntity<Object> runAutomationTestProject(String nameProject, TestGeneration.Coverage coverage) throws IOException {
        try {
            // Load file JSON
            File jsonFile = new File("project/anonymous/tmp-prj/" + nameProject + "/tmp-prjt.json");
            if (!jsonFile.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("The path does not exist!");
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode data = objectMapper.readTree(jsonFile);

            // Lấy nút gốc
            JsonNode rootNode = findNode.getNodeById(1, data.get("rootNode"));
            if (rootNode == null || !rootNode.get("entityClass").asText().equals("JavaPackageNode")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("The root node is not a JavaPackageNode!");
            }

            // Kết quả kiểm thử
            List<Object> results = new ArrayList<>();
            processJavaPackageNode(rootNode, nameProject, coverage, results);

            return ResponseEntity.ok(results);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error reading JSON file.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request. Exception: " + e.getMessage());
        }
    }

    // Đệ quy xử lý JavaPackageNode
    private void processJavaPackageNode(JsonNode node, String nameProject, TestGeneration.Coverage coverage, List<Object> results) {
        if (!node.has("children")) return;

        for (JsonNode child : node.get("children")) {
            String entityClass = child.get("entityClass").asText();

            try {
                if ("JavaMethodNode".equals(entityClass)) {
                    results.add(processJavaMethodNode(child, coverage));
                } else if ("JavaNode".equals(entityClass)) {
                    results.add(processJavaNode(child, coverage));
                } else if ("JavaPackageNode".equals(entityClass)) {
                    processJavaPackageNode(child, nameProject, coverage, results);
                }
            } catch (Exception e) {
                results.add("Error processing node ID " + child.get("id").asInt() + ": " + e.getMessage());
            }
        }
    }
    private List<Object> processJavaNode(JsonNode javaNode, TestGeneration.Coverage coverage) {
        List<Object> results = new ArrayList<>();
        if (!javaNode.has("children")) {
            results.add("JavaNode ID " + javaNode.get("id").asInt() + " does not contain any methods.");
            return results;
        }

        for (JsonNode child : javaNode.get("children")) {
            String entityClass = child.get("entityClass").asText();

            if ("JavaMethodNode".equals(entityClass)) {
                try {
                    Object testResult = processJavaMethodNode(child, coverage);
                    results.add(testResult);
                } catch (Exception e) {
                    results.add("Error testing method ID " + child.get("id").asInt() + ": " + e.getMessage());
                }
            } else {
                results.add("Skipped non-method node with ID " + child.get("id").asInt());
            }
        }
        return results;
    }
    private TestResult processJavaMethodNode(JsonNode javaMethodNode, TestGeneration.Coverage coverage) throws IOException {
        try {
            if (!javaMethodNode.get("entityClass").asText().equals("JavaMethodNode")) {
                throw new IllegalArgumentException("Provided node is not a JavaMethodNode.");
            }

            String simpleName = javaMethodNode.get("simpleName").asText();
            String pathMethod = javaMethodNode.get("path").asText();
            System.out.println("Path: " + pathMethod);

            int openingParenthesisIndex = simpleName.indexOf("(");
            String methodName = simpleName.substring(0, openingParenthesisIndex).trim();

            File file = new File(pathMethod);
            String className = file.getName();

            return AS4UT.runFullConcolic(javaMethodNode.get("id").asInt(),pathMethod, methodName, className, coverage);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Error processing JavaMethodNode: " + e.getMessage(), e);
        }
    }

    @Override
    public ResponseEntity<Object> runAutomationTestAll(List<Integer> targetIds, String nameProject, TestGeneration.Coverage coverage) throws IOException {
        try {
            File jsonFile = new File("project/anonymous/tmp-prj/" + nameProject + "/tmp-prjt.json");
            if (!jsonFile.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("The path does not exist!");
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode data = objectMapper.readTree(jsonFile);

            List<Object> results = new ArrayList<>();
            for (Integer targetId : targetIds) {
                JsonNode nodeWithId = findNode.getNodeById(targetId, data.get("rootNode"));

                if (nodeWithId == null) {
                    results.add("Node with ID " + targetId + " not found.");
                    continue;
                }

                String entityClass = nodeWithId.get("entityClass").asText();
                if ("JavaPackageNode".equals(entityClass)) {
                    processJavaPackageNode(nodeWithId, nameProject, coverage, results);
                }
                else  if ("JavaNode".equals(entityClass)) {
                    results.add( processJavaNode(nodeWithId, coverage));
                }
                else if ("JavaMethodNode".equals(entityClass)) {
                    results.add(processJavaMethodNode(nodeWithId, coverage));
                } else {
                    results.add("Unsupported entity class for node ID " + targetId + ": " + entityClass);
                }
            }
            return ResponseEntity.ok(results);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error reading JSON file.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request. Exception: " + e.getMessage());
        }
    }

    public void createMethodTest(String path, Integer targerId) throws IOException {
        File file = new File(path);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode nodes = objectMapper.readTree(file);
        JsonNode rootNode = nodes.get("rootNode");
        NodeServiceImpl nodeService = new NodeServiceImpl(rootNode);
        JsonNode node = nodeService.findByIdNode(targerId);
        Integer idMethod = node.get("id").intValue();
        String nameMethod = node.get("simpleName").textValue();

        JsonNode parentNode = nodeService.findParentById(targerId);
//        String className = parentNode.get("simpleName").textValue() + "Test";
        String methodPath = node.get("path").textValue();
//        String qualifiedClassName = parentNode.get("qualifiedName").textValue();

//        methodTest = new MethodTest(idMethod, nameMethod, qualifiedClassName, methodPath);
        methodTest = new MethodTest(idMethod, nameMethod, "ABC", methodPath);
        nodeService.getParameterDependency(targerId);
        parameterInputs = nodeService.getParameterInputs();
    }
}