package com.example.unittesting.controller;

import com.example.unittesting.Sevice.UTestService;

import com.example.unittesting.UnitTestingApplication;
import core.testGeneration.TestGeneration;
import io.swagger.v3.oas.annotations.Operation;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.io.*;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
@RestController
@RequestMapping("/api/unit-testing-service")
public class UTestController {
    private final UTestService utestService;

    public UTestController(UTestService utestService) {
        this.utestService = utestService;
    }

    @GetMapping("/restartService")
    public void restartService() {
        UnitTestingApplication.restart();
    }

    @GetMapping(value = "/unit")
    @Operation(
            summary = "This is API auto run full Concolic ",
            description = "Đây là API có input (targetId, nameProject) gửi về backend và phân tích method có" +
                    "id là targetId thuộc projecet có tên là nameProject và chạy tự động kiểm thử đơn vị",
            parameters = {@io.swagger.v3.oas.annotations.Parameter(name = "targetId", description = "Id của hàm mà người dùng muốn lấy thông tin chi tiết", example = "15"),
                    @io.swagger.v3.oas.annotations.Parameter(name = "nameProject", description = "Tên của Project chứa hàm người dùng muốn lấy thông tin", example = "test.zip.project"),
                    @io.swagger.v3.oas.annotations.Parameter(name = "coverageType", description = "Loại độ phủ cần kiểm thử")}
    )
    public ResponseEntity<Object> getUnitTest(@RequestParam int targetId, @RequestParam String nameProject, @RequestParam String coverageType) throws IOException {
        long t0 = System.currentTimeMillis();
        String thread = Thread.currentThread().getName();

        System.err.println("[REQ-START] thread=" + thread);

        try {
            TestGeneration.Coverage coverage;

            switch (coverageType) {
                case "statement":
                    coverage = TestGeneration.Coverage.STATEMENT;
                    break;
                case "branch":
                    coverage = TestGeneration.Coverage.BRANCH;
                    break;
                case "path":
                    coverage = TestGeneration.Coverage.PATH;
                    break;
                case "mcdc":
                    coverage = TestGeneration.Coverage.MCDC;
                    break;
                default:
                    throw new RuntimeException("Invalid coverage type");
            }

            ResponseEntity<Object> tmp = ResponseEntity.ok(utestService.runAutomationTest(targetId, nameProject, coverage));
            int ok = 1;
            return tmp;
        } catch (Exception e) {
            throw new  RuntimeException(e);
        } finally {
            long dt = System.currentTimeMillis() - t0;
            System.err.println("[REQ-END] thread=" + thread + " timeMs=" + dt);
        }
    }

    @GetMapping(value = "/regressionTest")
    @Operation(
            parameters = {@io.swagger.v3.oas.annotations.Parameter(name = "nameProject"),
                    @io.swagger.v3.oas.annotations.Parameter(name = "coverageType")}
    )
    public ResponseEntity<Object> getRegressionTest(@RequestParam String nameProject, @RequestParam String coverageType) throws IOException {
        TestGeneration.Coverage coverage;
        switch (coverageType) {
            case "statement":
                coverage = TestGeneration.Coverage.STATEMENT;
                break;
            case "branch":
                coverage = TestGeneration.Coverage.BRANCH;
                break;
            case "path":
                coverage = TestGeneration.Coverage.PATH;
                break;
            case "mcdc":
                coverage = TestGeneration.Coverage.MCDC;
                break;
            default:
                throw new RuntimeException("Invalid coverage type");
        }

        return ResponseEntity.ok(utestService.runRegressionTest(nameProject, coverage));
    }

    @GetMapping(value = "/multiple-unit")
    public ResponseEntity<Object> getUnitTests(@RequestParam List<Integer> targetIds, @RequestParam String nameProject, @RequestParam String coverageType) throws IOException {
        TestGeneration.Coverage coverage;
        switch (coverageType) {
            case "statement":
                coverage = TestGeneration.Coverage.STATEMENT;
                break;
            case "branch":
                coverage = TestGeneration.Coverage.BRANCH;
                break;
            case "path":
                coverage = TestGeneration.Coverage.PATH;
                break;
            case "mcdc":
                coverage = TestGeneration.Coverage.MCDC;
                break;
            default:
                throw new RuntimeException("Invalid coverage type");
        }
        List<Object> results = new ArrayList<>();
        for (int targetId : targetIds){
            Object result = utestService.runAutomationTest(targetId, nameProject, coverage);
            results.add(result);
        }
        return ResponseEntity.ok(results);
    }
    @GetMapping(value="/unit-test-file")
    public ResponseEntity<Object> getUnitTestFile(@RequestParam List<Integer> targetIds, @RequestParam String nameProject, @RequestParam String coverageType) throws IOException {
        TestGeneration.Coverage coverage;
        switch (coverageType) {
            case "statement":
                coverage = TestGeneration.Coverage.STATEMENT;
                break;
            case "branch":
                coverage = TestGeneration.Coverage.BRANCH;
                break;
            case "path":
                coverage = TestGeneration.Coverage.PATH;
                break;
            case "mcdc":
                coverage = TestGeneration.Coverage.MCDC;
                break;
            default:
                throw new RuntimeException("Invalid coverage type");
        }
        List<Object> results = new ArrayList<>();
        for (int targetId : targetIds){
            Object result = utestService.runAutomationTestFile(targetId, nameProject, coverage);
            results.add(result);
        }
        return ResponseEntity.ok(results);
    }
    @GetMapping(value="/unit-test-project")
    public ResponseEntity<Object> getUnitTestProject(@RequestParam String nameProject, @RequestParam String coverageType) throws IOException {
        TestGeneration.Coverage coverage;
        switch (coverageType) {
            case "statement":
                coverage = TestGeneration.Coverage.STATEMENT;
                break;
            case "branch":
                coverage = TestGeneration.Coverage.BRANCH;
                break;
            case "path":
                coverage = TestGeneration.Coverage.PATH;
                break;
            case "mcdc":
                coverage = TestGeneration.Coverage.MCDC;
                break;
            default:
                throw new RuntimeException("Invalid coverage type");
        }
        return ResponseEntity.ok(utestService.runAutomationTestProject(nameProject, coverage));
    }
    @GetMapping(value="/unit-test-all")
    public ResponseEntity<Object> getUnitTestAll(@RequestParam List<Integer> targetIds, @RequestParam String nameProject, @RequestParam String coverageType) throws IOException {
        TestGeneration.Coverage coverage;
        switch (coverageType) {
            case "statement":
                coverage = TestGeneration.Coverage.STATEMENT;
                break;
            case "branch":
                coverage = TestGeneration.Coverage.BRANCH;
                break;
            case "path":
                coverage = TestGeneration.Coverage.PATH;
                break;
            case "mcdc":
                coverage = TestGeneration.Coverage.MCDC;
                break;
            default:
                throw new RuntimeException("Invalid coverage type");
        }
        return ResponseEntity.ok(utestService.runAutomationTestAll(targetIds, nameProject, coverage));
    }
}