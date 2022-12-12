package org.sample.jenkins

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

// Determine OS for the current node and cache the results.
class NodeInfo {
    static ConcurrentMap<String, boolean> nodes = new ConcurrentHashMap<>()

    static boolean isUnix(CpsScript script) {
        String node = script.env.containsKey('NODE_NAME') ? script.env.NODE_NAME : null
        if(!node) {
            throw new Exception("Attempting to determine if the platform is unix for a node outside of a node declaration.")
        }
        if(nodes.containsKey(node)) {
            return nodes[node]
        }
        boolean result = script.isUnix()
        nodes[node] = result
        return result
    }
}