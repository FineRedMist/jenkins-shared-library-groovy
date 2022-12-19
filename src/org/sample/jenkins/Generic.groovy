package org.sample.jenkins

import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.sample.jenkins.NodeInfo

import groovy.xml.*

class Generic {
    /**
     * Simple batch file to execute a script based on the platform of the node.
     */
    static def exec(CpsScript script, String cmd, String encoding = null, String label = null, boolean returnStatus = false, boolean returnStdout = false) {
        if(NodeInfo.isUnix(script)) {
            return script.sh script: "#!/bin/sh\n${cmd}", encoding: encoding, label: label, returnStatus: returnStatus, returnStdout: returnStdout
        } else {
            return script.bat script: cmd, encoding: encoding, label: label, returnStatus: returnStatus, returnStdout: returnStdout
        }
    }

    static String readTextFile(CpsScript script, String filePath) {
        def bin64 = script.readFile(file: filePath, encoding: 'Base64')
        def binDat = bin64.decodeBase64()

        if(binDat.size() >= 3 
            && binDat[0] == -17
            && binDat[1] == -69
            && binDat[2] == -65) {
            return new String(binDat, 3, binDat.size() - 3, "UTF-8")
        } else {
            return new String(binDat)
        }
    }

    static Map gatherXmlResults(CpsScript script, String searchPath, Closure<Map> closure) {
        Map results = [:]
        def files = 0

        script.findFiles(glob: searchPath).each { f ->
            String fullName = f
            String data = readTextFile(script, fullName)
            
            def xml = new XmlParser(false, true, true).parseText(data)
            Map temp = closure(xml)

            temp.each { key, value ->
                if(results.containsKey(key)) {
                    results[key] += value
                } else {
                    results[key] = value
                }
            }
            files += 1
        }

        results.files = files

        return results
    }

}