package com.quicksdk.tools.extension

class JarExcludeExt {
    List<String> excludeJar = new ArrayList<>()

     void addExcludeJar(String jar){
        excludeJar.add(jar)
    }

}