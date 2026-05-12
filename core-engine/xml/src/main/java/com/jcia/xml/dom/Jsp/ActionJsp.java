package com.jcia.xml.dom.Jsp;

public class ActionJsp {

    private String actionName;

    private String actionNamespace;
    private JspTagNode jspTagNode;

    public JspTagNode getJspTagNode() {
        return jspTagNode;
    }

    public void setJspTagNode(JspTagNode jspTagNode) {
        this.jspTagNode = jspTagNode;
    }

    public ActionJsp() {

    }

    public String getActionNamespace() {
        return actionNamespace;
    }

    public void setActionNamespace(String actionNamespace) {
        this.actionNamespace = actionNamespace;
    }

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    @Override
    public String toString() {
        return "ActionJsp{" +
                "actionName='" + actionName + '\'' +
                ", actionNamespace='" + actionNamespace + '\'' +
                '}';
    }

}
