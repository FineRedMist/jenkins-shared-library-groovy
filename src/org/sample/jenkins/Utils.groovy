package org.sample.jenkins

import org.jenkinsci.plugins.workflow.cps.CpsScript

class Utils {
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
}