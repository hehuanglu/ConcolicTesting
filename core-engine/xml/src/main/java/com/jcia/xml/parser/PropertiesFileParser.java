package com.jcia.xml.parser;

import com.jcia.xml.dom.Node;
import com.jcia.xml.dom.Properties.PropertiesFileNode;
import com.jcia.xml.dom.Properties.PropertiesNode;
import com.jcia.xml.utils.Exception.JciaNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@AllArgsConstructor
@Getter
@Setter
public class PropertiesFileParser implements IParser, IPathParser, Callable {

    private static final Logger logger = LoggerFactory.getLogger(PropertiesFileParser.class);

    private String path;

    @Override
    public Node call() throws Exception {
        return parse(path);
    }

    @Override
    public Node parse(String path) throws JciaNotFoundException {
        PropertiesFileNode fileNode = new PropertiesFileNode();
        String parentName = new File(path).getParentFile().getName();
        String fileNameWithoutExt = removeFileExtension(new File(path).getName(), true);
        fileNode.setFullyQualifiedName(parentName + "." + fileNameWithoutExt);
        fileNode.setAbsolutePath(path);
        fileNode.setEntityClass("PropertiesFileNode");
        return parse(fileNode);
    }

    @Override
    public Node parse(Node node) throws JciaNotFoundException {
        try {
            PropertiesFileNode propertiesFileNode = (PropertiesFileNode) node;
            FileInputStream fis = new FileInputStream(node.getAbsolutePath());
            InputSource is = new InputSource(fis);

            propertiesFileNode.setName(new File((node.getAbsolutePath())).getName());
            List<PropertiesNode> propertiesNodes = parsePropertiesNode(node.getAbsolutePath());
            propertiesFileNode.setProperties(propertiesNodes);

            return propertiesFileNode;

        } catch (FileNotFoundException e) {
            logger.error(String.format("not found file [%s]", node.getAbsolutePath()));
        }
        return null;
    }

    public List<PropertiesNode> parsePropertiesNode(String path) {
        List<PropertiesNode> nodes = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                if(line.length() > 0) {
                    try {
                        if(line.charAt(0) != 35) {
                            if(line.split("=").length>1) {
                                PropertiesNode propertiesNode = new PropertiesNode();
                                propertiesNode.setName(line.split("=")[0]);
                                propertiesNode.setValue(line.split("=")[1]);
                                nodes.add(propertiesNode);
                            }
                        }
                    } catch (StringIndexOutOfBoundsException e) {
                        logger.error("StringIndexOutOfBoundsException in parsePropertiesNode in line: {}", line);
                    }
                }
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return nodes;
    }

    public static String removeFileExtension(String filename, boolean removeAllExtensions) {
        if (filename == null || filename.isEmpty()) {
            return filename;
        }

        String extPattern = "(?<!^)[.]" + (removeAllExtensions ? ".*" : "[^.]*$");
        return filename.replaceAll(extPattern, "");
    }

}
