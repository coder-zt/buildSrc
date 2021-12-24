package com.quicksdk.tools.utils;

public class PathUtils {
    private static PathUtils instance = null;
    private boolean initToolPath = false;
    private boolean initFilePath = false;
    String dxToolDir =   "\\buildSrc\\src\\assets\\dx.bat";
    String baksmaliToolDir =  "\\buildSrc\\src\\assets\\baksmali.jar";
    String jadToolDir =  "\\buildSrc\\src\\assets\\jad";
    String quickSDKJarDir =  "\\buildSrc\\src\\assets\\quicksdk_2.7.0.jar";

    String tempDir;
    String tempResDir;
    String tempJarDir;
    String tempClassesDir;
    String tempManifestFile;
    String tempAssetsDir;
    String tempResJarDir;
    String tempRDir;
    String tempLibDir;
    String tempDexDir;
    String smaliResultDir;
    String smaliResResultDir;

    private PathUtils(){

    }

    public static PathUtils getInstance(){
        if(instance == null){
            instance = new PathUtils();
        }
        return instance;
    }

    public void initToolPath(String projectBasePath){
        if(initToolPath){
            return;
        }else{
            initToolPath = true;
        }
        dxToolDir = projectBasePath + dxToolDir;
        baksmaliToolDir = projectBasePath + baksmaliToolDir;
        jadToolDir = projectBasePath + jadToolDir;
        quickSDKJarDir = projectBasePath + quickSDKJarDir;
    }

    public void initFilePath(String tempPath){
        if(initFilePath){
            return;
        }else{
            initFilePath = true;
        }
        tempDir = tempPath;
        tempResDir = tempDir + "\\res";
        tempJarDir =  tempDir + "\\jar";
        tempClassesDir =  tempDir + "\\classes";
        tempManifestFile =  tempDir + "\\AndroidManifest.xml";
        tempAssetsDir =  tempDir + "\\assets";
        tempLibDir =  tempDir + "\\lib";
        tempDexDir =  tempDir + "\\dex";
        smaliResultDir =  tempDir + "\\smali";
        tempResJarDir =  tempDir + "\\res-jar";
        tempRDir =  tempDir + "\\R";
    }

    public String getDxToolDir() {
        return dxToolDir;
    }

    public String getBaksmaliToolDir() {
        return baksmaliToolDir;
    }

    public String getJadToolDir() {
        return jadToolDir;
    }

    public String getQuickSDKJarDir() {
        return quickSDKJarDir;
    }

    public String getTempDir() {
        return tempDir;
    }

    public String getTempResDir() {
        return tempResDir;
    }

    public String getTempJarDir() {
        return tempJarDir;
    }

    public String getTempClassesDir() {
        return tempClassesDir;
    }

    public String getTempManifestFile() {
        return tempManifestFile;
    }

    public String getTempAssetsDir() {
        return tempAssetsDir;
    }

    public String getTempResJarDir() {
        return tempResJarDir;
    }

    public String getTempLibDir() {
        return tempLibDir;
    }

    public String getTempDexDir() {
        return tempDexDir;
    }

    public String getSmaliResultDir() {
        return smaliResultDir;
    }

    public String getSmaliResResultDir() {
        return smaliResResultDir;
    }

    public void setSmaliResResultDir(String smaliResResultDir) {
        this.smaliResResultDir = smaliResResultDir;
    }

    public String getTempRDir() {
        return tempRDir;
    }
}
