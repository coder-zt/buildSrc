package com.quicksdk.tools.utils;

import com.android.tools.r8.utils.F;

import java.io.File;

/**
 * 将jar文件编译为smali文件
 */
public class JarCompiler {

    public static void check(String handleTempPath) {
        //检查文件夹是否存在
        File tempDir = new File(handleTempPath);
        if (!tempDir.exists()) {
            tempDir.mkdir();
        }
        createSubTempDir(tempDir);
    }

    /**
     * 将jar文件编译为smali文件
     *
     * @param jarFile
     * @param handleTempPath
     */
    public static void execute(File jarFile, String handleTempPath){
        String outputDex = PathUtils.getInstance().getTempDexDir() + "\\" + jarFile.getName().replace(".jar", ".dex");
        String cmd =  PathUtils.getInstance().getDxToolDir() + " --dex --min-sdk-version=26 --output=" + outputDex;//--min-sdk-version=26
        if (jarFile.exists()) {
            cmd += " " + jarFile.getAbsolutePath();
            Logger.i("jar -> dex cmd：" + cmd);
            Cmd.run(cmd);
        }
        File dexFile = new File(outputDex);
        if (dexFile.exists()) {
            cmd = "java -jar " +  PathUtils.getInstance().getBaksmaliToolDir() + " d " + outputDex + " -o " + PathUtils.getInstance().getSmaliResultDir();
            Logger.i("dex -> smali cmd：" + cmd);
            Cmd.run(cmd);
        }
    }

    private static void createSubTempDir(File tempDir) {
        String dexDirPath = tempDir.getAbsolutePath() + "//dex";
        File dexDir = new File(dexDirPath);
        if (!dexDir.exists()) {
            dexDir.mkdir();
        }
        String smaliDirPath = tempDir.getAbsolutePath() + "//smali";
        File smaliDir = new File(smaliDirPath);
        if (!smaliDir.exists()) {
            smaliDir.mkdir();
        }
    }
}
