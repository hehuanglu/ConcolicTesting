package core.uploadProjectUtils.cloneProjectUtils.dataModel;

import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.ArrayList;
import java.util.List;

public class ClassData {

    private String typeOfClass;
    private String className;
    private String classModifier;
    private String superClassName;
    private List<String> superInterfaceName;

    private String fields = "";

    public ClassData() {
    }

    public ClassData(TypeDeclaration typeDeclaration) {
        if(typeDeclaration.isInterface()) {
            typeOfClass = "interface";
        } else {
            typeOfClass = "class";
        }
        className = typeDeclaration.getName().getIdentifier();
        int modifiers = typeDeclaration.getModifiers();
        switch (modifiers) {
            case Modifier.PUBLIC:
                classModifier = "public";
                break;
            case Modifier.PRIVATE:
                classModifier = "private";
                break;
            case Modifier.PROTECTED:
                classModifier = "protected";
                break;
            default:
                classModifier = "default";
                break;
        }
        if(typeDeclaration.getSuperclassType() != null) {
            superClassName = typeDeclaration.getSuperclassType().toString();
        }
        if(typeDeclaration.superInterfaceTypes().size() != 0) {
            List interfaceList = typeDeclaration.superInterfaceTypes();
            superInterfaceName = new ArrayList<>();
            for (int i = 0; i < interfaceList.size(); i++) {
                superInterfaceName.add(interfaceList.get(i).toString());
            }
        }

        FieldDeclaration[] fieldDeclarations = typeDeclaration.getFields();
        StringBuilder extractedFields = new StringBuilder();
        for(FieldDeclaration fieldDeclaration : fieldDeclarations) {
            extractedFields.append(fieldDeclaration).append("\n");
        }
        fields = extractedFields.toString();
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassModifier() {
        return classModifier;
    }

    public void setClassModifier(String classModifier) {this.classModifier = classModifier;}

    public String getSuperClassName() {
        return superClassName;
    }

    public void setSuperClassName(String superClassName) {
        this.superClassName = superClassName;
    }

    public String getTypeOfClass() {
        return typeOfClass;
    }

    public void setTypeOfClass(String typeOfClass) {
        this.typeOfClass = typeOfClass;
    }

    public List<String> getSuperInterfaceName() {
        return superInterfaceName;
    }

    public void setSuperInterfaceName(List<String> superInterfaceName) {
        this.superInterfaceName = superInterfaceName;
    }

    public String getFields() {
        return fields;
    }
}

