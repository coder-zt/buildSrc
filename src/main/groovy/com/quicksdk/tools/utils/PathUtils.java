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
    String manifestFileDir;

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
        tempResDir = tempDir + "\\channelResources\\res";
        tempJarDir =  tempDir + "\\channelResources\\jar";
        tempClassesDir =  tempDir + "\\channelResources\\classes";
        tempManifestFile =  tempDir + "\\channelResources\\AndroidManifest.xml";
        tempAssetsDir =  tempDir + "\\channelResources\\assets";
        tempLibDir =  tempDir + "\\channelResources\\lib";
        tempDexDir =  tempDir + "\\temp\\dex";
        smaliResultDir =  tempDir + "\\temp\\smali";
        tempResJarDir =  tempDir + "\\channelResources\\res-jar";
        tempRDir =  tempDir + "\\R";
        manifestFileDir =  tempDir + "\\channelResources";
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

    public String getManifestFileDir() {
        return manifestFileDir;
    }
}
