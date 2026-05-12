package core.path;

import core.ast.additionalNodes.Node;
import core.cfg.CfgNode;

public class Path {

    private Node currentFirst;

    private Node currentLast;

    public boolean isEmpty() {
        return currentFirst == null;
    }

    public Path(Path other) {
        Node current = other.getCurrentFirst();
        while (current != null) {
            this.addLast(current.getData());
            current = current.getNext();
        }
    }

    public Path() {
    }

    public void addLast(CfgNode data) {
        Node lastNode = currentLast;
        currentLast = new Node(data);
        if (isEmpty()) {
            currentFirst = currentLast;
        } else lastNode.setNext(currentLast);
    }

    public void addFirst(CfgNode data) {
        Node newNode = new Node(data);
        if (isEmpty()) {
            currentFirst = newNode;
            currentLast = newNode;
        } else {
            newNode.setNext(currentFirst);
            currentFirst = newNode;
        }
    }

    public void removeLast() {
        if (isEmpty()) return;

        if (currentLast == currentFirst) {
            currentLast = null;
            currentFirst = null;
            return;
        }

        Node currentNode = currentFirst;
        while (currentNode.getNext() != currentLast) {
            currentNode = currentNode.getNext();
        }
        currentNode.setNext(null);
        currentLast = currentNode;
    }

    @Override
    public String toString() {
        StringBuilder p = new StringBuilder("===============\n");
        Node tmpNode = currentFirst;
        while (tmpNode != null) {
            p.append(tmpNode.getData().toString());
            p.append("\n");
            tmpNode = tmpNode.getNext();
        }
        p.append("===============");
        return p.toString();
    }

    public Node getCurrentFirst() {
        return currentFirst;
    }

    public Node getCurrentLast() {
        return currentLast;
    }
}

