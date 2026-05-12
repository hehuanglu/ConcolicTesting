package com.example.unittesting.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MethodTest {
    private Integer id;

    private String name;

    private String qualifiedClassName;

    private String path;

}
