package com.quicksdk.tools.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RJavaHandlerTool {

    String smaliResultDir;
    public void execute(String javaPath, String channelJarPath, String quickSDKJarPath,String output) {
        java2Classes(javaPath, channelJarPath, quickSDKJarPath, output);
        class2Smali(output);
        copySmali();
    }

    public void java2Classes(String javaPath, String channelJarPath, String quickSDKJarPath, String output) {
        File outputDir = new File(output + "\\handledClass");
        if (!outputDir.exists()) {
            outputDir.mkdir();
        };
        StringBuilder sbCmd = new StringBuilder("javac -source 1.7 -target 1.7 -cp ");
        sbCmd.append(channelJarPath);
        sbCmd.append(";");
        sbCmd.append(quickSDKJarPath);
        sbCmd.append(" -d ");//输出的class路径
        sbCmd.append(outputDir);
        sbCmd.append(" ");
        sbCmd.append(javaPath);
        Cmd.run(sbCmd.toString());
    }

    //R*.class文件转为R.jar
    //R.jar转为R.Dex
    //R.dex转为smali
    private void class2Smali(String output) {
        File outputDir = new File(output + "\\handledJar");
        if (!outputDir.exists()) {
            outputDir.mkdir();
        }
        //将class下的*.class文件转化为smali
        //先转为*.jar文件
        //jar -cvf F:\JavaPlace\ExcRes\temp\proguard\quicksdk_channel.jar -C F:\JavaPlace\ExcRes\temp\classes/ .
        //jar -cvf E:\ResTestTemp\smali\channel.jar -C F:\JavaPlace\ExcRes\temp\classes/ .
        String cmd = "jar -cvf " + outputDir + "\\quicksdk_res.jar -C " + output + "\\handledClass" + "/ .";
        System.out.println(cmd);
        Cmd.run(cmd);
        String dxToolDir = PathUtils.getInstance().getDxToolDir();
        //再转为*.dex文件
        String outputDex = output + "\\handledDex\\" + "quicksdk_res.dex";
        File outputDexDir = new File(output + "\\handledDex");
        if (!outputDexDir.exists()) {
            outputDexDir.mkdir();
        }
        cmd = dxToolDir + " --dex --output=" + outputDex + " " + outputDir + "\\quicksdk_res.jar";
        System.out.println(cmd);
        Cmd.run(cmd);
        String baksmaliToolDir = PathUtils.getInstance().getBaksmaliToolDir();
        //再转为*.smali文件
        smaliResultDir = output + "\\" + "smali";
        File smaliDir = new File(smaliResultDir);
        if (!smaliDir.exists()) {
            smaliDir.mkdir();
        }
        cmd = "java -jar " + baksmaliToolDir + " -x " + outputDex + " -o " + smaliResultDir;
        System.out.println(cmd);
        Cmd.run(cmd);
    }

    private void copySmali() {
        PathUtils.getInstance().setSmaliResResultDir(smaliResultDir);
    }
}
