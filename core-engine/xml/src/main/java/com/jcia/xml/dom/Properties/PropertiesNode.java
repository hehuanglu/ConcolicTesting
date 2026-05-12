package com.jcia.xml.dom.Properties;

import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

@Data
public class PropertiesNode {
    private static final AtomicInteger count = new AtomicInteger(0);
    private int id;
    private String name;
    private String value;
    public PropertiesNode(String name, String value) {
        this.name = name;
        this.value = value;
        this.id = count.incrementAndGet();
    }
    public PropertiesNode() {
        this.id = count.incrementAndGet();
    }
}
