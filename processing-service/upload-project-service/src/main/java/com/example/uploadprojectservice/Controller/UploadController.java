package com.example.uploadprojectservice.Controller;

import com.example.uploadprojectservice.Service.UploadService;
import com.example.uploadprojectservice.ast.Node.Parameter;
import com.example.uploadprojectservice.ast.data.InfoMethod;
import com.example.uploadprojectservice.model.Response;
import com.example.uploadprojectservice.model.VersionCompareResponse;
import com.example.uploadprojectservice.utils.worker.Getter;
import com.example.uploadprojectservice.utils.worker.Writer;
import core.cfg.utils.ASTHelper;
import core.uploadProjectUtils.cloneProjectUtils.CloneProject;
import com.example.uploadprojectservice.utils.worker.findNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import core.FilePath;
import io.swagger.v3.oas.annotations.Operation;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
@RestController
@RequestMapping("/api/upload-project-service")
public class UploadController {
    /**
     *
     */
    @Autowired
    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping("/regression-testing-upload")
    public JsonNode regressionUpload(@RequestBody List<MultipartFile> files,
                                     @RequestParam(name = "user", required = false, defaultValue = "anonymous") String user,
                                     @RequestParam(name = "project", required = false, defaultValue = "tmp-prj") String project) throws IOException, InterruptedException {
        if (files != null) {
            String path = uploadService.buildProject(null, files.get(1), user, project);

            Response utUploadResponse = Getter.getResponse(path);

            uploadService.preprocessSourceCode(path, ASTHelper.Coverage.BRANCH);

            File jsonFile = new File(path + "/tmp-prjt.json");

            VersionCompareResponse versionCompareResponse = uploadService.compareTwoVersions(files, project, user);

            uploadService.applyVersionCompareInfo(utUploadResponse.getRootNode(), versionCompareResponse);

            Writer.write(path, utUploadResponse, "tmp-prjt");
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode data = objectMapper.readTree(jsonFile);
            return data;
        } else {
            throw new IllegalArgumentException("File is null");
        }
    }

    @PostMapping("/process")
    @Operation(
            summary = "This is API upload",
            description = "The API upload project.zip to store the source code of the program you want to perform unit-testing",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "This is the request body description :"),
            parameters = {@io.swagger.v3.oas.annotations.Parameter(name = "project", description = "The name save Project.", example = "abc-project"),
                    @io.swagger.v3.oas.annotations.Parameter(name = "user", description = "The name user.", example = "abc-user"),
                    @io.swagger.v3.oas.annotations.Parameter(name = "parser", description = "The list parser.")}
    )
    public JsonNode process(@RequestParam(name = "parser") List<String> parserList,
                            @RequestBody MultipartFile file,
                            @RequestParam(name = "user", required = false, defaultValue = "anonymous") String user,
                            @RequestParam(name = "project", required = false, defaultValue = "tmp-prj") String project) throws IOException, InterruptedException {
        if (file != null) {
            String path = uploadService.buildProject(parserList, file, user, project);
            uploadService.build(path);

            uploadService.preprocessSourceCode(path, ASTHelper.Coverage.MCDC);

            path += "/tmp-prjt.json";
            File jsonFile = new File(path);

            ObjectMapper objectMapper = new ObjectMapper();
            try {
                JsonNode data = objectMapper.readTree(jsonFile);
                return data;
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Error reading JSON file: " + e.getMessage());
            }
        } else {
            throw new IllegalArgumentException("File is null");
        }
    }

    @GetMapping(value = "/view-tree")
    public ResponseEntity<Object> NodeTree(@RequestParam String nameProject) {

        try {
            File file = new File("project/anonymous/tmp-prj/" + nameProject + "/tmp-prjt.json");
            if (!file.exists()) {
                // Xử lý khi tệp không tồn tại
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Đường dẫn không tồn tại!");
            } else {
                JSONParser jsonParser = new JSONParser();
                FileReader fileReader = new FileReader(file.getAbsolutePath());
                Object analysisFile;
                try {
                    Object obj = jsonParser.parse(fileReader);
                    analysisFile = obj;
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                // Trả về kết quả phân tích từ tệp JSON
                return ResponseEntity.ok(analysisFile);
            }
        } catch (IOException e) {
            // Xử lý ngoại lệ và trả về phản hồi lỗi
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi xảy ra trong quá trình xử lý yêu cầu: " + e.getMessage());
        }
    }

    @GetMapping(value = "/read")
    @Operation(
            summary = "Đây là API gọi đến một method",
            description = "API trả về các thông tin chi tiết của một method class",
            parameters = {@io.swagger.v3.oas.annotations.Parameter(name = "targetId", description = "Id của hàm mà người dùng muốn lấy thông tin chi tiết", example = "15"),
                    @io.swagger.v3.oas.annotations.Parameter(name = "nameProject", description = "Tên của Project chứa hàm người dùng muốn lấy thông tin", example = "test.zip.project")}
    )
    public ResponseEntity<Object> getInfoMethod(@RequestParam int targetId, @RequestParam String nameProject) {
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
                        //System.out.println(simpleName);
                        String entityClass = nodeWithId.get("entityClass").asText();
                        String pathMethod = nodeWithId.get("path").asText();
                        List<String> children = new ArrayList<>();
                        JsonNode childrenNode = nodeWithId.get("children");
                        if (childrenNode.isArray()) {
                            for (JsonNode childNode : childrenNode) {
                                children.add(childNode.asText());
                            }
                        }
                        String qualifiedName = nodeWithId.get("qualifiedName").asText();
                        String uniqueName = nodeWithId.get("uniqueName").asText();
                        ArrayList<Integer> numberOfSentences = new ArrayList<>();
                        int openingParenthesisIndex = simpleName.indexOf("(");
                        String name = simpleName.substring(0, openingParenthesisIndex).trim();

                        StringBuilder content = new StringBuilder();
                        List<Parameter> parameters = new ArrayList<>();
                        CompilationUnit compilationUnit = StaticJavaParser.parse(new File(pathMethod));
                        List<com.github.javaparser.ast.body.MethodDeclaration> methodDeclarations = compilationUnit.findAll(MethodDeclaration.class);
                        for (MethodDeclaration methodDeclaration : methodDeclarations) {
                            String methodName = methodDeclaration.getSignature().asString();
                            //System.out.println(methodName);
                            if (methodName.equals(simpleName)) {
                                content = new StringBuilder(methodDeclaration.toString());
                                for (com.github.javaparser.ast.body.Parameter parameter : methodDeclaration.getParameters()) {
                                    parameters.add(new Parameter(parameter.getNameAsString(), parameter.getType().asString()));
                                }
                                String returnType = methodDeclaration.getType().asString();
                                parameters.add(new Parameter("return", returnType));
                                int startLine = methodDeclaration.getBegin().get().line;
                                int endLine = methodDeclaration.getEnd().get().line;
                                int linesInMethod = endLine - startLine + 1;

                                numberOfSentences.add(startLine);
                                numberOfSentences.add(endLine);
                                numberOfSentences.add(linesInMethod);
                            }
                        }
                        InfoMethod infoMethod = new InfoMethod(
                                targetId,
                                name,
                                children,
                                pathMethod,
                                qualifiedName,
                                uniqueName,
                                content,
                                parameters,
                                numberOfSentences
                        );
                        return ResponseEntity.ok(infoMethod);
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
            System.out.println("Error processing request. Exception: " + e.getMessage() + "\n");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request. Exception: " + e.getMessage());
        }
    }

    @PostMapping(value = "/expect")
    @Operation(
            summary = "Đây là API để người dùng nhập expected inputs/outputs",
            description = "API sẽ lưu lại những expected inputs/outputs mà người dùng nhập vào" +
                    "với inputs đó chương trình thực hiện các test case cho ra độ phủ actual outputs"
    )
    public ResponseEntity<? extends Object> setExpectValue(@RequestBody InfoMethod requestMethod) {
        if (requestMethod != null) {
            try {
                ResponseEntity<Object> response = uploadService.saveDataTest(requestMethod);
                return response;
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request. Exception: " + e.getMessage());
            }
        } else {
            return ResponseEntity.badRequest().body("Request body is empty.");
        }
    }
}