package com.jcia.xml.dom.Properties;

import com.jcia.xml.dom.Node;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PropertiesFileNode extends Node {
    List<PropertiesNode> properties;
}
