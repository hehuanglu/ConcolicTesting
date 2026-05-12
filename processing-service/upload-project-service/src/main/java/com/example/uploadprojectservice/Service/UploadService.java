package com.example.uploadprojectservice.Service;

import com.example.uploadprojectservice.ast.Node.Node;
import com.example.uploadprojectservice.ast.data.InfoMethod;
import com.example.uploadprojectservice.model.VersionCompareResponse;
import core.cfg.utils.ASTHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface UploadService {
    public ResponseEntity<Object> build(String path) throws IOException;
    public String buildProject(List<String> parser, MultipartFile file, String user, String project) throws IOException;
    public VersionCompareResponse compareTwoVersions(List<MultipartFile> files, String user, String project);
    public ResponseEntity<Object> saveDataTest(InfoMethod requestMethod);
    public void preprocessSourceCode(String path, ASTHelper.Coverage coverage) throws IOException, InterruptedException;
    public void applyVersionCompareInfo(Node node, VersionCompareResponse versionCompareResponse);
}
