package com.quicksdk.tools.extension

class JarExcludeExt {

    //不用提取smali的库
    List<String> excludeJar = new ArrayList<>()
    //不是渠道的接入工程，不用添加插件
    List<String> excludAapplication = new ArrayList<>()

     void addExcludeJar(String jar){
        excludeJar.add(jar)
    }

    void addExcludeApplication(String application){
        excludAapplication.add(application)
    }

}