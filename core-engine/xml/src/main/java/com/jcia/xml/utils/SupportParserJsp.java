package com.jcia.xml.utils;

import com.jcia.xml.dom.Node;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SupportParserJsp {

    private static final Logger logger = LogManager.getLogger(SupportParserJsp.class);
    /**
     * read file from absolutePath of node
     * @param node
     * @return content file format string with no comment jsp tag
     *
     */
    public String getContentFileWithoutComment(Node node){
        if(node == null ) return null;
        String content = null;
        try {
//            logger.debug(node.getAbsolutePath());
            content = readFileContent(node.getAbsolutePath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String regex = "(?s)<%--.*?--%>";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);
        String output = matcher.replaceAll("");
        return output;
    }

    public static String readFileContent(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), "UTF-8");
    }

    /**
     * @param attr
     * @return
     * @function: check value of an atrribute contain intrernal tag
     */
    public boolean checkContainTagInAttribute(String attr) {
        //logger.debug(attr);
        String regex = "(?s)\\<.*?\\>";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(attr);
        int cout = 0 ;
        while (matcher.find()){
            cout++;
        }

        if(cout != 0){
            return true;
        }
        return false;
    }

    /**
     * @param att
     * @return
     * @function: check value of an attribute is a OGNL expression
     */
    public List<String> checkOgnlInAttribute(String att) {
        List<String> result = new ArrayList<>();
        String regex = "(?s)\\%\\{.*?\\}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(att);
        while (matcher.find()){
            result.add(matcher.group(0));
        }
//        logger.debug(result);
        return result;
    }

    /**
     * normalize action if contain "/' at begin
     * @param action
     * @return action with not "/" at begin
     */
    public String nomalizeActionString( String action){
        String result;
        if(action.charAt(0) == '/'){
            StringBuilder stringBuilder = new StringBuilder(action);
            stringBuilder.deleteCharAt(0);
            result = stringBuilder.toString();
        }
        else result = action;
        return result;
    }

    /**
     * catch namespace if namespace nest into action
     * @param action
     * @return [0] : namespace, [1] : action
     */
    public List<String> getNamespacefromAction(String action){
        if(action==null || action.equals("")) return null;
        List<String> result = new ArrayList<>();
        String[] split = action.split("/");
        String namespace="";
        namespace+=split[0];
        for(int i = 1 ; i < split.length-1 ; i++){
            namespace +="/" + split[i];
        }
        result.add(namespace);
        result.add(split[split.length-1]);
        //System.out.println("namesapce_cation: " + result);
        return result;
    }

    /**
     * split action: delete all elements after  "!" or "."
     * @param action
     * @return
     */
    public String splitAction(String action){
        if(action.equals("") || action == null) return action ;
        String result ;
        result = action.split("!")[0];
        result = result.split("\\.")[0];
        return result;
    }

}
