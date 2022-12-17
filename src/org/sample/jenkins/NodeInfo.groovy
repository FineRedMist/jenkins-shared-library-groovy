package org.sample.jenkins

import org.jenkinsci.plugins.workflow.cps.CpsScript

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

// Determine OS for the current node and cache the results.
class NodeInfo {
    private static def nodes = new ConcurrentHashMap<>()

    static boolean isUnix(CpsScript script) {
        String nodeName = script.env.NODE_NAME
        if(!nodeName) {
            throw new Exception("Attempting to determine if the platform is unix for a node outside of a node declaration.")
        }
        if(nodes.containsKey(nodeName)) {
            return nodes[nodeName]
        }
        boolean result = script.isUnix()
        nodes[nodeName] = result
        return result
    }
}