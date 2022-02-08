package com.quicksdk.tools.utils


class Logger {

    private static final String pluginName = "quicksdk-extract-tool"
    private static final boolean logSwitch = true


    static void i(String msg){
        if(!logSwitch){
            return
        }
        println pluginName + ": " + msg
    }
}