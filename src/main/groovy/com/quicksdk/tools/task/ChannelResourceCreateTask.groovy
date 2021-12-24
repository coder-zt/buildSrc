package com.quicksdk.tools.task

import com.quicksdk.tools.utils.ChannelInfo
import com.quicksdk.tools.utils.HttpRequest
import com.quicksdk.tools.utils.ParseManifestTool
import net.sf.json.JSONObject
import org.apache.commons.io.LineIterator;
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.IOFileFilter
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

/**
 * 任务内容：
 * 处理提取渠道的资源文件
 */
class ChannelResourceCreateTask extends DefaultTask {

    @Input
    String tempChannelResDir

    @Input
    String projectPackName

    @Input
    String channelResourceDir

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
            new ParseManifestTool().execute(tempChannelResDir, resourceName,projectPackName)
            moveResToFiles()
            createDescText()
            renameValueXml()
            removeDuplicateResources(filesDir.absolutePath + "\\" + "res")
        }else{
            println "创建渠道文件夹失败"
            return
        }

        //移动渠道资源到打包工具的目录下
        File oldChannelRes = new File(channelResourceDir + File.separator + resourceName)
        File tempChannelRes = new File(tempChannelResDir + File.separator + resourceName)
        if(oldChannelRes.exists()){
           getProject().delete(oldChannelRes)
        }
        getProject().copy {
            from tempChannelRes.absolutePath
            into oldChannelRes.absolutePath
        }
        if(tempChannelRes.exists()){
            getProject().delete(tempChannelRes)
        }
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
    }

    private void createDescText() {
//生成description.txt
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
     *  r
     */
    void moveResToFiles(){
        List<String> resDirNames = new ArrayList<>();
        resDirNames.add("assets")
        resDirNames.add("lib")
        resDirNames.add("res")
        resDirNames.add("smali")
        resDirNames.add("R")
        def task = this
        resDirNames.each{
            def that = it
            File resFile = new File(tempChannelResDir + "\\" + that)
            if(resFile.exists()){
                if(it == "R"){//处理R文件夹的文件，替换包名
                    if (!resFile.exists()) {
                        return;
                    }
                    File[] files = resFile.listFiles();
                    for (File file : files) {
                        String content = FileUtils.readFileToString(file, "UTF-8").replaceAll("LR/R", "L{{\\\$packNamePath}}/R");
                        FileUtils.writeStringToFile(file, content, "UTF-8");
                    }
                }
                if(it == "smali"){//处理smali文件夹的文件，判断有不有smali\com\bun
                    boolean hasOaid = new File(tempChannelResDir + "\\smali\\com\\bun").exists()
                    oaid = hasOaid?1:0
                }
                getProject().copy {
                    from tempChannelResDir + "\\" + that
                    into filesDir.absolutePath + "\\" + that
                }
            }
        }
        findSpecialFiles()
    }

    //获取渠道中的非png xml文件
    private  void findSpecialFiles(){
        String assetsFiles = getSpecialFiles(filesDir.absolutePath + "\\" + "assets");
        String resFiles = getSpecialFiles(filesDir.absolutePath + "\\" + "res");
        String specialFiles = (assetsFiles+resFiles).trim();
        if(!"".endsWith(specialFiles)){
            FileUtils.write(new File(filesDir.absolutePath + "\\" + "specialFiles.txt"), specialFiles, "utf-8");
        }
    }

    //将R文件中的包名路径替换为占位符
//    private static void renameRFilesPackName(File RDirFile){
//
//    }

    private static String getSpecialFiles(String dirPath){
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
            def renamePath = it.absolutePath.replace("values\\","values\\csdk_")
            it.renameTo(renamePath)
        }
    }

    private void removeDuplicateResources(String resPath) throws IOException{
        File res = new File(resPath);
        if(res.exists()){
            File[] files = res.listFiles();
            for (int i = 0; i < files.length; i++) {
                if(files[i].getName().startsWith("values")){
                    removeDuplicateResources2(files[i].getAbsolutePath());
                }
            }
        }
    }

    //移除values下与游戏有冲突的资源
    private void removeDuplicateResources2(String valuesDirPath) throws IOException {
        File valuesDir = new File(valuesDirPath);
        if (!valuesDir.exists()) {
            return;
        }
        for (File file : valuesDir.listFiles()) {
            if (file.isFile()) {
                String fileName = file.getName().toLowerCase();
                if (fileName.contains("string") || fileName.contains("value")) {
                    removeDuplicateStrings(file.getAbsolutePath());
                }
                if (fileName.contains("dimen") || fileName.contains("value")) {
                    removeDuplicateDimens(file.getAbsolutePath());
                }
            }
        }
    }

    private void removeDuplicateStrings(String filePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        File file = new File(filePath);
        LineIterator iterator = FileUtils.lineIterator(file, "UTF-8");
        while (iterator.hasNext()) {
            String line = iterator.next();
            if (line.contains("<string name=\"app_name\">") || line.contains("<string name=\"action_settings\">")
                    || line.contains("<string name=\"hello_world\">")) {

            } else {
                sb.append(line + "\r\n");
            }
        }
        iterator.close();
        FileUtils.writeStringToFile(file, sb.toString(), "UTF-8");
    }

    private void removeDuplicateDimens(final String filePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        File file = new File(filePath);
        LineIterator iterator = FileUtils.lineIterator(file, "UTF-8");
        while (iterator.hasNext()) {
            String line = iterator.next();
            if (line.contains("<dimen name=\"activity_horizontal_margin\">") || line.contains("<dimen name=\"activity_vertical_margin\">")) {;
            } else {
                sb.append(line + "\r\n");
            }
        }
        iterator.close();
        FileUtils.writeStringToFile(file, sb.toString(), "UTF-8");
    }

    def write(String destPath, String content) {
        try{
            // 创建目标文件
            File destFile = new File(destPath)
            if(!destFile.exists()){
                destFile.createNewFile()
            }
            destFile.withWriter{ writer ->
                writer.write(content)
            }
        }catch(Exception e){
            e.printStackTrace()
        }
    }
}
