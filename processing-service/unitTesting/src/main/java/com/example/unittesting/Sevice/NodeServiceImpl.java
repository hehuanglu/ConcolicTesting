package com.example.unittesting.Sevice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.entity.Parameter;
import core.entity.ParameterInput;
import core.entity.PrimitiveParameter;
import core.entity.ReferenceParameter;
import core.cfg.utils.FileService;
import lombok.Data;
import org.eclipse.jdt.core.dom.*;
import org.springframework.util.CollectionUtils;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Data
public class NodeServiceImpl {
    private List<JsonNode> javaNodes;

    private List<ParameterInput> parameterInputs;

    private List<String> typeParameterInputs;

    private Map<String, JsonNode> mapClassToNode = new HashMap<>();

    public NodeServiceImpl(JsonNode nodes) {
        javaNodes = new ArrayList<>();
        parameterInputs = new ArrayList<>();
        typeParameterInputs = new ArrayList<>();
//        javaNodes.add(null);

        getAllNodes(nodes);
    }
    public void dependencyGraphAnalysis(JsonNode unitNode) throws IOException {
        List<JsonNode> parameterNodes = null;
        parameterNodes = new ObjectMapper().readerForListOf(JsonNode.class).readValue(unitNode.get("parameters"));

        for (int i = 0; i < parameterNodes.size(); i++) {
            JsonNode par = parameterNodes.get(i);
            List<Parameter> parameters = new ArrayList<>();
            ParameterInput parameterInput = null;

            String simpleName = par.get("describe").textValue();
            if (par.get("entityClass").textValue().equals("JavaSyntheticType")) {
                List<JsonNode> bounds = null;
                try {
                    bounds = new ObjectMapper().readerForListOf(JsonNode.class).readValue(par.get("bounds"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                List<String> typeParameterOfName = bounds.stream().map(bound -> bound.get("describe").textValue()).collect(Collectors.toList());
                if (!CollectionUtils.isEmpty(typeParameterOfName)) {
                    for (String nameClass: typeParameterOfName) {
                        if (mapClassToNode.containsKey(nameClass)) {
                            JsonNode node = mapClassToNode.get(nameClass);
                            List<JsonNode> depNodes = new ObjectMapper().readerForListOf(JsonNode.class).readValue(node.get("dependencyFrom"));
                            List<JsonNode> depInheritanceNodes = depNodes.stream().filter(n -> n.get("dependency").get("inheritance").intValue() != 0).collect(Collectors.toList());

                            for (JsonNode depInheritanceNode: depInheritanceNodes) {
                                String qualifiedName = depInheritanceNode.get("node").get("qualifiedName").textValue();
                                try {
                                    Class<?> instanceClass = Class.forName(qualifiedName);
                                    if (Modifier.isAbstract(instanceClass.getModifiers()) || instanceClass.isInterface()) {
                                        break;
                                    }
                                } catch (ClassNotFoundException e) {
                                    throw new RuntimeException(e);
                                }

                                ReferenceParameter referenceParameter = new ReferenceParameter(qualifiedName, simpleName);
                                parameters.add(referenceParameter);
                            }
                        } else {
                            if (nameClass.equals("java.lang.Number")) {
                                ReferenceParameter integerReference = new ReferenceParameter("java.lang.Integer", simpleName);
                                parameters.add(integerReference);

                                ReferenceParameter longReference = new ReferenceParameter("java.lang.Long", simpleName);
                                parameters.add(longReference);

                                ReferenceParameter shortReference = new ReferenceParameter("java.lang.Short", simpleName);
                                parameters.add(shortReference);

                                ReferenceParameter byteReference = new ReferenceParameter("java.lang.Byte", simpleName);
                                parameters.add(byteReference);

                                ReferenceParameter doubleReference = new ReferenceParameter("java.lang.Double", simpleName);
                                parameters.add(doubleReference);

                                ReferenceParameter floatReference = new ReferenceParameter("java.lang.Float", simpleName);
                                parameters.add(floatReference);
                            }
                        }
                    }
                } else {
                    ReferenceParameter floatReference = new ReferenceParameter("java.lang.Object", simpleName);
                    parameters.add(floatReference);
                }
                parameterInput = new ParameterInput(parameters);
            } else if (par.get("entityClass").textValue().equals("JavaReferenceType")) {
                String nameClass = par.get("describe").textValue();
                if (mapClassToNode.containsKey(nameClass)) {
                    JsonNode rootNode = mapClassToNode.get(nameClass);
                    parameters = getInheritanceFromClass(rootNode, parameters);
                } else {
                    parameters.add(new ReferenceParameter(nameClass, ""));
                }
                parameterInput = new ParameterInput(parameters);
            } else {
                if (simpleName.equals("int") || simpleName.equals("long") || simpleName.equals("short")
                        || simpleName.equals("byte") || simpleName.equals("double") || simpleName.equals("float")) {
                    parameters.add(new PrimitiveParameter(simpleName));
                    parameterInput = new ParameterInput(parameters);
                } else {
                    parameters.add(new ReferenceParameter(simpleName, ""));
                    parameterInput = new ParameterInput(parameters);
                }
            }
            parameterInputs.add(parameterInput);
        }
        System.out.println(parameterInputs);

//        List<JsonNode> dependencyToNodes = new ObjectMapper().readerForListOf(JsonNode.class).readValue(unitNode.get("dependencyTo"));
//        Map<String, Integer> nameGenericToId = new HashMap<>();
//        for (JsonNode root: dependencyToNodes) {
//            nameGenericToId.put(root.get("node").get("qualifiedName").textValue(), root.get("node").get("id").intValue());
//        }
//
//        for (String genericName: mapGenericToIndex.keySet()) {
//            Integer i = mapGenericToIndex.get(genericName);
//            List<Parameter> parameters = parameterInputs.get(i).getParameters();
//
//            if (!CollectionUtils.isEmpty(mapGenericToTypePar.get(genericName))) {
//                for (String type: mapGenericToTypePar.get(genericName)) {
//                    if (nameGenericToId.containsKey(type)) {
//                        Integer id = nameGenericToId.get(type);
//                        JsonNode node = javaNodes.get(id);
//                        String qualifiedName = node.get("qualifiedName").textValue();
//                        ReferenceParameter referenceParameter = new ReferenceParameter(qualifiedName, genericName);
//
//                        if (!Modifier.isAbstract(referenceParameter.getClassParameter().getModifiers()) &&
//                                !referenceParameter.getClassParameter().isInterface()) {
//                            parameters.add(referenceParameter);
//                        }
//
//                        try {
//                            node = javaNodes.get(node.get("id").intValue());
//                            dependencyToNodes = new ObjectMapper().readerForListOf(JsonNode.class).readValue(node.get("dependencyFrom"));
//                        } catch (IOException e) {
//                            throw new RuntimeException(e);
//                        }
//                        dependencyToNodes.forEach(depNode -> {
//                            if (depNode.get("dependency").get("inheritance").intValue() != 0) {
//                                String qualifiedName1 = depNode.get("node").get("qualifiedName").textValue();
//
//                                parameters.add(new ReferenceParameter(qualifiedName1, genericName));
//                            }
//                        });
//                    } else {
//
//                    }
//                }
//            }
//
//        }
    }

    public List<Parameter> getInheritanceFromClass(JsonNode rootNode, List<Parameter> parameters) throws IOException {
        List<JsonNode> depNodes = new ObjectMapper().readerForListOf(JsonNode.class).readValue(rootNode.get("dependencyFrom"));
        List<JsonNode> depInheritanceNodes = depNodes.stream().filter(n -> n.get("dependency").get("inheritance").intValue() != 0).collect(Collectors.toList());

        String qualifiedName = rootNode.get("qualifiedName").textValue();
        try {
            Class<?> instanceClass = Class.forName(qualifiedName);
            if (!(Modifier.isAbstract(instanceClass.getModifiers()) || instanceClass.isInterface())) {
                parameters.add(new ReferenceParameter(qualifiedName, ""));
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        for (JsonNode depInheritanceNode: depInheritanceNodes) {
            String qualifiedName1 = depInheritanceNode.get("node").get("qualifiedName").textValue();
            parameters = getInheritanceFromClass(mapClassToNode.get(qualifiedName1), parameters);
        }
        return parameters;
    }

    public void dependencyToNodeAnalysis(JsonNode unitNode) throws IOException {
        List<JsonNode> dependencyToNodes = new ObjectMapper().readerForListOf(JsonNode.class).readValue(unitNode.get("dependencyFrom"));
        String methodNameTest = unitNode.get("simpleName").textValue();
        for (JsonNode node: dependencyToNodes) {
            if (node.get("dependency").get("invocation").intValue() != 0) {
                Integer id = node.get("node").get("id").intValue();
                JsonNode dpNode = findByIdNode(id);
                String methodNameDependency = dpNode.get("simpleName").toString();

                String path = dpNode.get("path").textValue();

                ASTParser astParser = ASTParser.newParser(AST.JLS8);
                try {
                    astParser.setSource(FileService.readFileToString(path).toCharArray());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                astParser.setKind(ASTParser.K_COMPILATION_UNIT);
                CompilationUnit compilationUnit = (CompilationUnit) astParser.createAST(null);
                ASTVisitor astVisitor = new ASTVisitor() {

                    private Map<String, String> mapVariableToDeclaration = new HashMap<>();

                    private String methodNameDependency;

                    private String methodNameTest;

                    private List<ParameterInput> parameterInputs;
                    private ASTVisitor init(String methodNameDependency, String methodNameTest, List<ParameterInput> parameterInputs) {
                        this.methodNameDependency = methodNameDependency;
                        this.methodNameTest = methodNameTest;
                        this.parameterInputs = parameterInputs;
                        return this;
                    }


                    @Override
                    public boolean visit(MethodInvocation node) {
                        String name = node.getName().toString();
                        if (name.equals(methodNameTest)) {
                            List<ASTNode> arguments = node.arguments();
                            for (int i = 0; i < arguments.size(); i++) {
                                ParameterInput parameterInput = parameterInputs.get(i);
                                String nameDeclaration = mapVariableToDeclaration.get(arguments.get(i).toString());
                                List<Parameter> parameters = parameterInput.getParameters();
                                if (typeParameterInputs.get(i) != null) {
                                    if (nameDeclaration != null && Character.isUpperCase(nameDeclaration.charAt(0))) {
                                        boolean isType = nameDeclaration.equals("Integer") || nameDeclaration.equals("Double")
                                                || nameDeclaration.equals("Float") || nameDeclaration.equals("String")
                                                || nameDeclaration.equals("Long") || nameDeclaration.equals("Short")
                                                || nameDeclaration.equals("Byte");
                                        if (isType) {
                                            parameters.add(new ReferenceParameter("java.lang." + nameDeclaration, typeParameterInputs.get(i)));
                                        }
                                    } else {
                                        if (arguments.get(i) instanceof NumberLiteral) {
                                            NumberLiteral numberLiteral = (NumberLiteral) arguments.get(i);
                                            String value = numberLiteral.getToken();
                                            try {
                                                Integer.parseInt(value);
                                                parameters.add(new ReferenceParameter(Integer.class.getName(), typeParameterInputs.get(i)));
                                            } catch (NumberFormatException eInt) {
                                                try {
                                                    Long.parseLong(value);
                                                    parameters.add(new ReferenceParameter(Long.class.getName(), typeParameterInputs.get(i)));
                                                } catch (NumberFormatException eLong) {
                                                    try {
                                                        Double.parseDouble(value);
                                                        parameters.add(new ReferenceParameter(Double.class.getName(), typeParameterInputs.get(i)));
                                                    } catch (NumberFormatException eDouble) {
                                                        try {
                                                            Float.parseFloat(value);
                                                            parameters.add(new ReferenceParameter(Float.class.getName(), typeParameterInputs.get(i)));
                                                        } catch (NumberFormatException eFloat) {
                                                            parameters.add(new ReferenceParameter(String.class.getName(), typeParameterInputs.get(i)));
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        return super.visit(node);
                    }

                    @Override
                    public boolean visit(MethodDeclaration node) {
                        String name = node.getName().toString();
                        if (name.equals(methodNameDependency)) {
                            return super.visit(node);
                        }
                        return true;
                    }

                    @Override
                    public boolean visit(VariableDeclarationFragment node) {
                        String variableName = node.getName().toString();
                        ASTNode astNode = node.getParent();
                        if (astNode instanceof VariableDeclarationStatement) {
                            Type type = ((VariableDeclarationStatement) astNode).getType();
                            if (type instanceof SimpleType) {
                                String typeName = ((SimpleType) type).getName().getFullyQualifiedName();
                                mapVariableToDeclaration.put(variableName, typeName);
                            }
                        }
//                        if (node.getInitializer() instanceof ClassInstanceCreation) {
//                            ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation) node.getInitializer();
//                            String nameDeclaration = classInstanceCreation.getType().toString();
//                            mapVariableToDeclaration.put(variableName, nameDeclaration);
//                        }

                        return super.visit(node);
                    }
                }.init(methodNameDependency, methodNameTest, parameterInputs);

                compilationUnit.accept(astVisitor);
            }
        }
    }


    public void getParameterDependency(Integer targetId) throws IOException {
        JsonNode node = findByIdNode(targetId);
        dependencyGraphAnalysis(node);
//        dependencyToNodeAnalysis(node);
    }

    public void getAllNodes(JsonNode node) {
        javaNodes.add(node);
        JsonNode name = node.get("qualifiedName");
        if (name == null) {
            name = node.get("simpleName");
        }
        mapClassToNode.put(name.textValue(), node);
//        if (node.get("entityClass").textValue().equals("JavaClassNode")) {
//            mapClassToNode.put(node.get("name").textValue(), node);
//        }
        for (JsonNode child: node.get("children")) {
            getAllNodes(child);
        }
    }

    public JsonNode findByIdNode(Integer targerId) {
        return javaNodes.get(targerId - 1);
    }

    public JsonNode findParentById(Integer targetId) {
        if (targetId > 0) {
            try {
                JsonNode node = javaNodes.get(targetId - 1);
                List<JsonNode> nodes = new ObjectMapper().readerForListOf(JsonNode.class).readValue(node.get("children"));
                if (nodes.isEmpty()) {
                    return findParentById(targetId - 1);
                } else {
                    boolean isCheck = nodes.stream().anyMatch(n -> n.get("id").intValue() == targetId);
                    return isCheck ? node : findParentById(targetId - 1);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }
}
