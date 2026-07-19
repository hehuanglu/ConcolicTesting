package core.path;

import core.ast.additionalNodes.Node;
import core.cfg.CfgNode;

public class Path {

    private Node currentFirst;
    private Node currentLast;

    public Path() {
    }

    public Path(Path other) {
        Node current = other.currentFirst;

        while (current != null) {
            addLast(current.getData());
            current = current.getNext();
        }
    }

    public boolean isEmpty() {
        return currentFirst == null;
    }

    public void addLast(CfgNode data) {
        Node newNode = new Node(data);

        if (isEmpty()) {
            currentFirst = newNode;
            currentLast = newNode;
        } else {
            currentLast.setNext(newNode);
            currentLast = newNode;
        }
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

    /**
     * Nối một bản sao của path vào cuối path hiện tại.
     */
    public void addPath(Path path) {
        if (path == null || path.isEmpty()) {
            return;
        }

        Node current = path.currentFirst;

        while (current != null) {
            addLast(current.getData());
            current = current.getNext();
        }
    }

    public void removeLast() {
        if (isEmpty()) {
            return;
        }

        if (currentFirst == currentLast) {
            currentFirst = null;
            currentLast = null;
            return;
        }

        Node current = currentFirst;

        while (current.getNext() != currentLast) {
            current = current.getNext();
        }

        current.setNext(null);
        currentLast = current;
    }

    public Node getCurrentFirst() {
        return currentFirst;
    }

    public Node getCurrentLast() {
        return currentLast;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("===============\n");

        Node current = currentFirst;

        while (current != null) {
            builder.append(current.getData()).append('\n');
            current = current.getNext();
        }

        builder.append("===============");
        return builder.toString();
    }
}