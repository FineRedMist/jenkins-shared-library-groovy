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
        boolean result = false
        try
        {
            if(nodes.containsKey(nodeName)) {
                result = nodes[nodeName]
                return result
            }
            result = script.isUnix()
            nodes[nodeName] = result
            return result
        }
        finally {
            script.echo("${nodeName} isUnix=${result}")
        }
    }
}