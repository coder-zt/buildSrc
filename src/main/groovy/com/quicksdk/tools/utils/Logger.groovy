package com.quicksdk.tools.utils


class Logger {

    private static final String pluginName = "quicksdk-extract-tool"
    private static final boolean logSwitch = true


    static void i(String msg){
        if(!logSwitch){
            return
        }
        println pluginName + ": " + msg
        new File("E:\\ChannelCode0\\buildSrc\\src\\assets\\log.txt").append(pluginName + ": " + msg + "\n")
    }
}