package com.example.unittesting.Sevice;

import core.testGeneration.NTDTestGeneration.NTDPairwiseTesting.NTDPairwiseTesting;
import core.testGeneration.TestGeneration;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;

public interface UTestService {

    public ResponseEntity<Object> runAutomationTest(int targetId, String nameProject, TestGeneration.Coverage coverage) throws IOException;
    public ResponseEntity<Object> runRegressionTest(String nameProject, TestGeneration.Coverage coverage) throws IOException;
    public ResponseEntity<Object> runAutomationTestFile(int targetId, String nameProject, NTDPairwiseTesting.Coverage coverage) throws IOException;
    public ResponseEntity<Object> runAutomationTestProject(String nameProject, NTDPairwiseTesting.Coverage coverage) throws IOException;
    public ResponseEntity<Object> runAutomationTestAll(List<Integer> targetIds, String nameProject, NTDPairwiseTesting.Coverage coverage) throws IOException;

}
