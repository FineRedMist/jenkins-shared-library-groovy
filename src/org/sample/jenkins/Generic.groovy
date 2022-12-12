package org.sample.jenkins

import org.jenkinsci.plugins.workflow.cps.CpsScript

import groovy.xml.*

class Generic {
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