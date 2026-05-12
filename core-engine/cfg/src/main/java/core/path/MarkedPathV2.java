package core.path;

import core.ast.additionalNodes.Node;
import core.cfg.CfgNode;

import java.util.ArrayList;
import java.util.List;

public final class MarkedPathV2 {
    private static List<String> markedStatements = new ArrayList<>();

    private MarkedPathV2(){}

    public static void add(String statement) {
        markedStatements.add(statement);
    }

    public static boolean check(Path path) {
        int i = 0;
        Node currentNode = path.getCurrentFirst();
        while (currentNode != null && i < markedStatements.size()) {
            CfgNode cfgNode = currentNode.getData();
            if(cfgNode.getContent().equals("")) {
                currentNode = currentNode.getNext();
                continue;
            }

            if(!cfgNode.getContent().equals(markedStatements.get(i))) {
                reset();
                return false;
            }

            // Updater
            i++;
            currentNode = currentNode.getNext();
        }
        reset();
        return true;
    }

    private static void reset() {
        markedStatements = new ArrayList<>();
    }
}
