package com.example.unittesting.Sevice;

import core.testGeneration.TestGeneration;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;

public interface UTestService {

    public ResponseEntity<Object> runAutomationTest(int targetId, String nameProject, TestGeneration.Coverage coverage) throws IOException;

}
