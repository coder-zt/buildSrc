package com.quicksdk.tools.task

import com.quicksdk.tools.utils.ChannelInfo
import com.quicksdk.tools.utils.HttpRequest
import com.quicksdk.tools.utils.ParseManifestTool
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.IOFileFilter
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.json.JSONObject

class ChannelResourceCreateTask extends DefaultTask {

    @Input
    String tempChannelResDir

    @Input
    @Optional
    String channelName

    @Input
    @Optional
    String channelType

    @Input
    @Optional
    String codeVersion


    int oaid

    String resourceVersion

    String resourceName

    String channelResDir

    File filesDir

    @TaskAction
    void execute(){
        init()

        def created = createChannelResDir()
        if(created){
            createFirstDirAndFile()
            moveResToFiles()
            renameValueXml()
        }else{
            println "创建渠道文件夹失败"
            return
        }
        new ParseManifestTool().execute(resourceName)
    }

    /**
     * 获取所有渠道额信息，初始化该渠道资源信息
     */
    void init(){
        String channelsInfo = HttpRequest.getChannelInfos()
        ChannelInfo.init(channelsInfo, channelType)
        resourceName = ChannelInfo.getResourceName()
        resourceVersion = ChannelInfo.getResourceVersion()
        int versionInt = ++Integer.parseInt(resourceVersion)
        resourceVersion = String.valueOf(versionInt)
        println "resourceName: " + resourceName
        println "resourceVersion: " + resourceVersion
    }

    /**
     * 创建渠道的资源文件夹
     */
    boolean createChannelResDir(){
        channelResDir = tempChannelResDir + File.separator + resourceName
        File channelResFile = new File(channelResDir)
        if(channelResFile.exists()){
            if(channelResFile.deleteDir()){
                return channelResFile.mkdir()
            }
        }else{
            return channelResFile.mkdir()
        }
        return false
    }

    /**
     *  生成第一级文件夹下的文件及目录
     *  files
     *  active.txt
     *  description.txt
     *  permission.txt
     *  root.txt
     */
    void createFirstDirAndFile(){
        //生成files目录
        filesDir = new File(channelResDir + File.separator + "files")
        filesDir.mkdir()
        //生成description.txt
        oaid = 0
        //{"versionNo":"90","channelVersion":"1.8.0.0","OAID":0,"aboveVersion":200,"needAddBaseLib":"1"}
        def descPath = channelResDir + File.separator + "description.txt"
        JSONObject descJson = new JSONObject()
        descJson.put("versionNo", resourceVersion)
        descJson.put("channelVersion", codeVersion)
        descJson.put("OAID", oaid)
        descJson.put("aboveVersion", 200)
        descJson.put("needAddBaseLib", "1")
        write(descPath, descJson.toString())
    }

    /**
     *  将打包生成的文件移动到渠道资源的files目录下
     *  assets
     *  lib
     *  res
     *  smali
     */
    void moveResToFiles(){
        List<String> resDirNames = new ArrayList<>();
        resDirNames.add("assets")
        resDirNames.add("lib")
        resDirNames.add("res")
        resDirNames.add("smali")
        resDirNames.each{
            def that = it
            if(new File(tempChannelResDir + "\\" + that).exists()){
                getProject().copy {
                    from tempChannelResDir + "\\" + that
                    into filesDir.absolutePath + "\\" + that
                }
            }
        }
        findSpecialFiles()
    }

    //获取渠道中的非png xml文件
    private void findSpecialFiles() throws IOException {
        String assetsFiles = getSpecialFiles(filesDir.absolutePath + "\\" + "assets");
        String resFiles = getSpecialFiles(filesDir.absolutePath + "\\" + "res");
        String specialFiles = (assetsFiles+resFiles).trim();
        if(!"".endsWith(specialFiles)){
            FileUtils.write(new File(filesDir.absolutePath + "\\" + "specialFiles.txt"), specialFiles, "utf-8");
        }
    }

    private String getSpecialFiles(String dirPath){
        StringBuilder sb = new StringBuilder();
        File dirFiles = new File(dirPath);
        if (dirFiles.exists()) {
            Collection<File> files = FileUtils.listFiles(dirFiles, new IOFileFilter() {

                public boolean accept(File dir, String name) {
                    return false;
                }

                public boolean accept(File file) {
                    return !(file.getName().endsWith("xml") || file.getName().endsWith("png"));
                }

            }, new IOFileFilter() {

                public boolean accept(File dir, String name) {
                    return false;
                }

                public boolean accept(File file) {
                    return !(file.getName().endsWith("xml") || file.getName().endsWith("png"));
                }
            });
            for (File file : files) {
                String filePath = file.getAbsolutePath();
                String saveString = (filePath.substring(filePath.indexOf("\\files\\") + "\\files\\".length(), filePath.length())).replace("\\", "/");
                sb.append(saveString).append("\r\n");
            }
        }
        return sb.toString();
    }

    def renameValueXml(){
        def valuesPath = filesDir.absolutePath + "\\res\\values"
        new File(valuesPath).listFiles().each{
           println it.absolutePath
        }
    }

    def write(String destPath, String content) {
        try{
            // 创建目标文件
            File destFile = new File(destPath)
            if(!destFile.exists()){
                destFile.createNewFile()
            }
            destFile.withWriter{ writer ->
                println content
                writer.write(content)
            }
        }catch(Exception e){
            e.printStackTrace()
        }
    }
}
