package com.example.uploadprojectservice.ast.data;

public class DataTest {
    private String NameTest;
    private int IdMethod;
    private String status;
    private InfoMethod infoMethod;
    private String creationDate;
    private String output;

    public DataTest(String nameTest, int idMethod, String status, InfoMethod infoMethod, String creationDate, String output) {
        NameTest = nameTest;
        IdMethod = idMethod;
        this.status = status;
        this.infoMethod = infoMethod;
        this.creationDate = creationDate;
        this.output = output;
    }

    public String getNameTest() {
        return NameTest;
    }

    public void setNameTest(String nameTest) {
        NameTest = nameTest;
    }

    public int getIdMethod() {
        return IdMethod;
    }

    public void setIdMethod(int idMethod) {
        IdMethod = idMethod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public InfoMethod getInfoMethod() {
        return infoMethod;
    }

    public void setInfoMethod(InfoMethod infoMethod) {
        this.infoMethod = infoMethod;
    }

    public String getCreationDateTime() {
        return creationDate;
    }

    public void setCreationDateTime(String creationDateTime) {
        this.creationDate = creationDate;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }
}
