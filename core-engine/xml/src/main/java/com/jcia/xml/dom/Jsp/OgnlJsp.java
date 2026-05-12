package com.jcia.xml.dom.Jsp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OgnlJsp {

    private static final Logger logger = LogManager.getLogger(OgnlJsp.class);
    private String type;
    private String ognl;
    private boolean formMapping;
    private JspTagNode jspTagNode;
    private JspFileNode jspFileNode;

    public boolean isFormMapping() {
        return formMapping;
    }

    public void setFormMapping(boolean formMapping) {
        this.formMapping = formMapping;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    public JspTagNode getJspTagNode() {
        return jspTagNode;
    }

    public void setJspTagNode(JspTagNode jspTagNode) {
        this.jspTagNode = jspTagNode;
    }

    public String getOgnl() {
        return ognl;
    }
    public JspFileNode getJspFileNode() {
        return jspFileNode;
    }

    public void setJspFileNode(JspFileNode jspFileNode) {
        this.jspFileNode = jspFileNode;
    }

    public void setOgnl(String ognl) {
        this.ognl = ognl;
        // normalize();
    }

    public void normalize(){
        //start cut %{#....}
        if(ognl == null || ognl.equals("")) return;
        this.ognl = this.ognl.trim();
        StringBuilder stringBuilder = new StringBuilder(ognl);
        if(stringBuilder.charAt(0) == '%'){
            stringBuilder.deleteCharAt(0);
            stringBuilder.deleteCharAt(0);
            stringBuilder.deleteCharAt(stringBuilder.length()-1);
        }

        this.ognl = stringBuilder.toString();
        this.ognl = this.ognl.trim();

        handle3();
        if(ognl == null || ognl.equals("")) return;

        StringBuilder builder2 = new StringBuilder(ognl);
        if(builder2.charAt(0) == '#'){
            builder2.deleteCharAt(0);
        }
        this.ognl = builder2.toString();
        this.ognl = this.ognl.trim();
        //end cut %{#....}

        hanlde1();
        handle2();
    }


    /**
     * handle ognl type same as: session['name']
     */
    private void hanlde1(){
        // check contain []
        int check = this.ognl.indexOf('[');
        if(check == -1) return;
        //end check

        String[] split = ognl.split("\\[");
        this.type = split[0].trim();
        Pattern pattern = Pattern.compile("\\['(.*?)\\']");
        Matcher matcher = pattern.matcher(this.getOgnl());
        String newOgnl = null;
        while (matcher.find()){
            newOgnl = matcher.group(1).trim();
        }
        this.ognl = newOgnl;

    }

    /**
     * handle ognl type same as: session.name
     */
    private void handle2(){
        int check = this.ognl.indexOf('[');
        if(check != -1) return;
        else{
            int checkdot = this.ognl.indexOf('.');
            //logger.debug("checkdot: " + checkdot);
            if(checkdot != -1){
                String[] split = this.ognl.split("\\.");
//                logger.debug(split.length);
                if(split.length < 2 ) return;
//                logger.debug(split[0]);
                if(split[0].equals("action")
                        || split[0].equals("attr")
                        || split[0].equals("session")
                        || split[0].equals("request")
                        || split[0].equals("parameters")
                        || split[0].equals("application")){
//                    logger.debug(split[1]);
                    this.type = split[0];
                    this.ognl = split[1];
                }
                else {
                    this.ognl = split[0];
                    this.type = "action";
                }
            }
        }
    }

    /**
     * check if contain + same as : %{#action.name + "hieu"}
     */
    private void handle3(){
        int check = this.ognl.indexOf('+');
        int checkMinus = this.ognl.indexOf('-');
        int checkEqual = this.ognl.indexOf('=');
        int checkMore = this.ognl.indexOf('>');
        int checkLess = this.ognl.indexOf('<');
        int checkAnd = this.ognl.indexOf('&');
        int checkOr = this.ognl.indexOf('|');
        if(checkOr == -1 && checkAnd == -1 && checkMore == -1 && checkLess == -1
                && check == -1 && checkMinus == -1 && checkEqual == -1) return;
        else{
            String[] split = this.ognl.split("\\-|\\+|=|!|>|<|&|\\|");
            for(String temp : split){
                String temp2 = temp.trim();
                //  if(temp2.charAt(0)!= ' '){
                this.ognl = null;
                OgnlJsp ognlJsp = new OgnlJsp();
                ognlJsp.setJspTagNode(this.jspTagNode);
                ognlJsp.setJspFileNode(this.jspFileNode);
                ognlJsp.setOgnl(temp2);
                this.jspFileNode.addOgnl(ognlJsp);
                // }
            }
        }
    }

    @Override
    public String toString() {
        return "OgnlJsp{" +
                "type='" + type + '\'' +
                ", ognl='" + ognl + '\'' +
                ", formMapping=" + formMapping +
                '}';
    }

}
