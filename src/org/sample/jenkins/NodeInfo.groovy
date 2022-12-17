package org.sample.jenkins

import org.jenkinsci.plugins.workflow.cps.CpsScript
import jenkins.model.Jenkins
import hudson.model.Node

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

    /**
    * Returns the {@link Node} that this computer represents.
    * 
    * From: https://www.tabnine.com/code/java/methods/jenkins.model.Jenkins/getNode (with tweaks)
    *
    * @return
    *      null if the configuration has changed and the node is removed, yet the corresponding {@link Computer}
    *      is not yet gone.
    */
    public static Node getNode(CpsScript script) {
        String nodeName = script.env.NODE_NAME
        if(!nodeName) {
            return null
        }
        Jenkins j = Jenkins.getInstanceOrNull() // TODO confirm safe to assume non-null and use getInstance()
        if (j == null) {
            return null
        }
        return j.getNode(nodeName);
    }
}