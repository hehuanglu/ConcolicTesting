package com.jcia.xml.dom.Jsp;

import com.jcia.xml.dom.FileNode;
import com.jcia.xml.utils.Exception.JciaIgnore;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;

public class JspFileNode extends FileNode {

    @JsonIgnore
    @JciaIgnore
    private Document document;
    @JsonIgnore @JciaIgnore
    private List<ActionJsp> listAction;
    @JsonIgnore @JciaIgnore
    private List<OgnlJsp> listOgnl;
    @JsonIgnore @JciaIgnore
    private List<IncludeJsp> listRelativePathJspInclude;

    public JspFileNode() {
        listAction = new ArrayList<>();
        listOgnl = new ArrayList<>();
        listRelativePathJspInclude = new ArrayList<>();
    }

    public List<ActionJsp> getListAction() {
        return listAction;
    }

    public List<OgnlJsp> getListOgnl() {
        return listOgnl;
    }

    public void addAction(ActionJsp action) {
        this.listAction.add(action);
    }

    public void addOgnl(OgnlJsp listOgnl) {
        listOgnl.setJspFileNode(this);
        listOgnl.normalize();
        this.listOgnl.add(listOgnl);
    }

    public List<IncludeJsp> getListRelativePathJspInclude(){
        return this.listRelativePathJspInclude;
    }
    public void addRelativePathJspInclude(IncludeJsp path){
        this.listRelativePathJspInclude.add(path);
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public void setListAction(List<ActionJsp> list){
        this.listAction = list;
    }

    public void setListOgnl(List<OgnlJsp> list){
        this.listOgnl = list;
    }

    public void setListRelativePathJspInclude(List<IncludeJsp> list){
        this.listRelativePathJspInclude = list;
    }

}
