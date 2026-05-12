package com.example.uploadprojectservice.utils.worker;

import com.fasterxml.jackson.databind.JsonNode;

public class findNode {
    public static JsonNode getNodeById(int targetId, JsonNode node){
        if (node.get("id").asInt() == targetId) {
            return node;
        }else if (node.has("children")) {
            for (JsonNode child : node.get("children")) {
                JsonNode result = getNodeById(targetId, child);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
}
