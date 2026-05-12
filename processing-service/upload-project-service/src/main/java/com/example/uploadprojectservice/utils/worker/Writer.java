package com.example.uploadprojectservice.utils.worker;

import com.example.uploadprojectservice.model.Response;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

public class Writer {
    public static void write(String filePath, Response response, String project) {
        ObjectMapper mapper = new ObjectMapper();
        File file = new File(filePath);
        try {
            mapper.writeValue(new File(file.getPath() + "/" + project + ".json"), response);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
