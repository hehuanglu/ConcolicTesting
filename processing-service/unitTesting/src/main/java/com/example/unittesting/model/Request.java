package com.example.unittesting.model;

public class Request {
    private String path;
    public Request() {
    }
    public Request(String path) {
        this.path = path;
    }


    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
