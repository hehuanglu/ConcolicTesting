package com.example.uploadprojectservice.Service;

import com.example.uploadprojectservice.Service.project.ProjectService;
import com.example.uploadprojectservice.ast.Node.MethodNode;
import com.example.uploadprojectservice.ast.Node.Node;
import com.example.uploadprojectservice.ast.Node.Parameter;
import com.example.uploadprojectservice.ast.data.DataTest;
import com.example.uploadprojectservice.ast.data.InfoMethod;
import com.example.uploadprojectservice.model.Response;
import com.example.uploadprojectservice.model.VersionCompareResponse;
import com.example.uploadprojectservice.utils.JwtUtils;
import com.example.uploadprojectservice.utils.worker.Getter;
import com.example.uploadprojectservice.utils.worker.Writer;
import com.example.uploadprojectservice.utils.worker.findNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.netflix.discovery.EurekaClient;
import core.FilePath;
import core.cfg.utils.ASTHelper;
import core.uploadProjectUtils.cloneProjectUtils.CloneProject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@Service
public class UploadServiceImpl implements UploadService {

    @Autowired
    private EurekaClient eurekaClient;

    private RestTemplate restTemplate = new RestTemplate();

    @Override
    public VersionCompareResponse compareTwoVersions(List<MultipartFile> files, String project, String user) {
        String versionCompareURL = "http://" +
                eurekaClient.getApplication("VERSION-COMPARE-SERVICE").getInstances().get(0).getIPAddr() +
                ":6002/api/version-compare-service/byFile";

        String fullUrl = UriComponentsBuilder.fromHttpUrl(versionCompareURL)
                .queryParam("project", project)
                .queryParam("user", user)
                .toUriString();

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        for (MultipartFile file : files) {
            try {
                ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                    @Override
                    public String getFilename() {
                        return file.getOriginalFilename();
                    }
                };
                body.add("files", fileResource);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<VersionCompareResponse> response = restTemplate.postForEntity(fullUrl, requestEntity, VersionCompareResponse.class);
        return response.getBody();
    }

    private static int testIdCounter = 1;
    final ProjectService projectService;

    final JwtUtils jwtUtils;

    public UploadServiceImpl(ProjectService projectService, JwtUtils jwtUtils) {
        this.projectService = projectService;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public void applyVersionCompareInfo(Node node, VersionCompareResponse versionCompareResponse) {
        if (node instanceof MethodNode) {
            MethodNode methodNode = (MethodNode) node;
            String uniqueName = methodNode.getUniqueName().substring(1).replace(" ", "");

            List<LinkedHashMap> changedNodes = versionCompareResponse.getChangedNodes();
            for (LinkedHashMap value : changedNodes) {
                if (value.get("uniqueName").equals(uniqueName)) {
                    methodNode.setChanged(true);
                    break;
                }
            }

            List<LinkedHashMap> addedNodes = versionCompareResponse.getAddedNodes();
            for (LinkedHashMap value : addedNodes) {
                if (value.get("uniqueName").equals(uniqueName)) {
                    methodNode.setAdded(true);
                    break;
                }
            }

            Set<LinkedHashMap> impactedNodes = versionCompareResponse.getImpactedNodes();
            for (LinkedHashMap value : impactedNodes) {
                if(value.get("uniqueName").equals(uniqueName)) {
                    methodNode.setImpacted(true);
                    break;
                }
            }
        } else {
            List<Node> nodes = node.getChildren();
            for (Node n : nodes) {
                applyVersionCompareInfo(n, versionCompareResponse);
            }
        }
    }

    @Override
    public void preprocessSourceCode(String path, ASTHelper.Coverage coverage) throws IOException, InterruptedException {
        Path rootPackage = CloneProject.findRootPackage(Paths.get(path));
        if (rootPackage == null) throw new RuntimeException("Invalid project");

        long startRunTestTime = System.nanoTime();

        // Clone Project (NTD)
//        CloneProject.cloneProject(rootPackage.toString(), FilePath.clonedProjectPath, coverage);

        // Concolic Preprocess
//            ConcolicUploadUtil.ConcolicPreprocessSourceCode(rootPackage.toString());

        long endRunTestTime = System.nanoTime();

        double runTestDuration = (endRunTestTime - startRunTestTime) / 1000000.0;

        System.out.println("clone time " + runTestDuration);
    }

    @Override
    public ResponseEntity<Object> build(String path) throws IOException {
        Response response = Getter.getResponse(path);
        Writer.write(path, response, "tmp-prjt");
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public String buildProject(List<String> parser, MultipartFile file, String user, String project) {
        String userPath = user;
        if (!userPath.equals("anonymous")) {
            userPath = jwtUtils.extractUsername(user);
        }
        String fileName = projectService.storeFile(file, userPath, project);
        return fileName;
    }

    @Override
    public ResponseEntity<Object> saveDataTest(InfoMethod requestMethod) {
        try {
            int idmethod = requestMethod.getId();
            // Lưu chuỗi JSON vào tệp tin
            String path = requestMethod.getPath();
            int zipProjectIndex = path.indexOf(".zip.project");
            String targetDirectoryName = path.substring(0, zipProjectIndex + ".zip.project".length());
            String outputFolderPath = targetDirectoryName + File.separator;
            File jsonFile = new File(outputFolderPath + "/tmp-prjt.json");
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode data = objectMapper.readTree(jsonFile);
            JsonNode nodeWithId = findNode.getNodeById(idmethod, data.get("rootNode"));
            if (data == null || nodeWithId == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request. Invalid JSON data.");
            }
            String simpleName = nodeWithId.get("simpleName").asText();
            //System.out.println(simpleName);
            String entityClass = nodeWithId.get("entityClass").asText();
            String qualifiedName = nodeWithId.get("qualifiedName").asText();
            String uniqueName = nodeWithId.get("uniqueName").asText();
            int openingParenthesisIndex = simpleName.indexOf("(");
            String name = simpleName.substring(0, openingParenthesisIndex).trim();

            StringBuilder content = new StringBuilder();
            CompilationUnit compilationUnit = StaticJavaParser.parse(new File(path));
            List<com.github.javaparser.ast.body.MethodDeclaration> methodDeclarations = compilationUnit.findAll(MethodDeclaration.class);
            for (MethodDeclaration methodDeclaration : methodDeclarations) {
                String methodname = methodDeclaration.getSignature().asString();
                //System.out.println(methodName);
                if (methodname.equals(simpleName)) {
                    content = new StringBuilder(methodDeclaration.toString());
                }
            }
            List<Parameter> parameters = requestMethod.getParameters();
            StringBuilder parameterTypes = new StringBuilder();
            for (Parameter parameter : parameters) {
                parameterTypes.append(parameter.getDescribe()).append("_");
                System.out.println(parameter + "\n");
            }
            parameterTypes.deleteCharAt(parameterTypes.length() - 1); // Xóa dấu "_" cuối cùng

            int testId = testIdCounter++;
            String nameTest = name + idmethod + "_" + parameterTypes.toString() + "_" + testId;
            LocalDateTime localDateTime = LocalDateTime.now();
            String currentTime = localDateTime.toString();

            requestMethod.setSimpleName(name);
            requestMethod.setQualifiedName(qualifiedName);
            requestMethod.setUniqueName(uniqueName);
            requestMethod.setEntityClass(entityClass);
            requestMethod.setContent(content);
            DataTest dataTest = new DataTest(nameTest, testId, "not executed", requestMethod, currentTime, "");
            String json = objectMapper.writeValueAsString(dataTest);

            // Lưu chuỗi JSON vào tệp tin


            File outputFolder = new File(outputFolderPath);
            if (!outputFolder.exists()) {
                outputFolder.mkdirs(); // Tạo thư mục nếu chưa tồn tại
            }

            String outputFilePath = outputFolderPath + nameTest + ".json";
            File outputFile = new File(outputFilePath);
            FileWriter writer = new FileWriter(outputFile);
            writer.write(json);
            writer.close();

            // Đối tượng ResponseEntity cho kết quả
            return ResponseEntity.ok("Expect value has been set and saved successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            // Xử lý lỗi nếu cần
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request. Exception: " + e.getMessage());
        }
    }
}
