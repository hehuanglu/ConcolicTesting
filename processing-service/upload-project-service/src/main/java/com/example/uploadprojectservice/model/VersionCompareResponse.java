package com.example.uploadprojectservice.model;

import java.util.List;
import java.util.Set;

public class VersionCompareResponse {
    private List changedNodes;
    private List deletedNodes;
    private List addedNodes;
    private Set impactedNodes;

    public List getChangedNodes() {
        return changedNodes;
    }

    public void setChangedNodes(List changedNodes) {
        this.changedNodes = changedNodes;
    }

    public List getDeletedNodes() {
        return deletedNodes;
    }

    public void setDeletedNodes(List deletedNodes) {
        this.deletedNodes = deletedNodes;
    }

    public List getAddedNodes() {
        return addedNodes;
    }

    public void setAddedNodes(List addedNodes) {
        this.addedNodes = addedNodes;
    }

    public Set getImpactedNodes() {
        return impactedNodes;
    }

    public void setImpactedNodes(Set impactedNodes) {
        this.impactedNodes = impactedNodes;
    }
}
