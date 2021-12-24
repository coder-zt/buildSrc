package com.quicksdk.tools.task

import com.quicksdk.tools.extension.ChannelProjectExt
import org.apache.commons.io.FileUtils
import org.apache.commons.io.LineIterator
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * 任务内容:
 * 根据配置在根目录的build.gradle中的newChannelBean创建一个新的项目
 * 创建完成后需要sync
 *
 */
class ChannelProjectGenerateTask extends DefaultTask {

    @Input
    String demoProjectPath

    @Input
    ChannelProjectExt channelProjectExt


    String parentProjectPath

    String newProjectPath

    String newProjectName

    String newQuickSDKXmlPath

    String newModelPath

    String newGradlePath

    String settingsGradlePath

    @TaskAction
    void execute(){
        init()
        if(check(channelProjectExt.channelType)){
            getProject().copy {
                from demoProjectPath
                into newProjectPath
            }
            channelSpecialHandle()
            addProjectWithSettings()
        }else {
            println "该项目已在上次生成，无需再次生成"
        }
    }

    /**
     * 初始化，初始化重要文件和目录的路径
     *
     * @return
     */
    def init(){
        parentProjectPath = getProject().getProjectDir().absolutePath
        newProjectName = "QuickSDK_" +  channelProjectExt.channelType + "_" + channelProjectExt.channelName
        newProjectPath = parentProjectPath + "\\" +  newProjectName
        newQuickSDKXmlPath = newProjectPath + "\\src\\main\\assets\\quicksdk.xml"
        newModelPath = newProjectPath + "\\src\\main\\java\\com\\quicksdk\\apiadapter\\model"
        newGradlePath = newProjectPath + "\\build.gradle"
        settingsGradlePath = parentProjectPath + "\\settings.gradle"
    }

    /**
     * 通过读取newChannelType.txt获取上次创建渠道工程的channel号
     * 前后不能相等
     * @param sdkAdapterPath
     * @return
     */
    boolean check(String channelType) {
        String oldChannelType = ""
        String newChannelTypePath = parentProjectPath + "\\buildSrc\\src\\assets\\newChannelType.txt"
        File file = new File(newChannelTypePath)
        LineIterator iter;
        try {
            iter = FileUtils.lineIterator(file, "UTF-8")
            while (iter.hasNext()) {
                oldChannelType = iter.next()
            }
            iter.close()
        } catch (IOException e) {
            e.printStackTrace()
        }
        if(oldChannelType != channelType){
            write(newChannelTypePath, channelType)
        }
        return oldChannelType != channelType
    }

    /**
     * 在settings.gradle文件中注册项目
     */
    def addProjectWithSettings(){
        def settingsFile = new File(settingsGradlePath)
        settingsFile.append("\ninclude ':" + newProjectName + "'")
    }


    /**
     *  渠道项目个性化处理
     */
    def channelSpecialHandle(){
        //修改quicksdk.xml中的信息
        String xmlContent = read(newQuickSDKXmlPath)
        xmlContent = xmlContent.replace("<string name=\"channel_type\">0</string>",
                "<string name=\"channel_type\">" + channelProjectExt.channelType + "</string>")
        xmlContent = xmlContent.replace("<string name=\"quicksdk_channel_name\">model</string>",
                "<string name=\"quicksdk_channel_name\">" + channelProjectExt.channelName + "</string>")
        write(newQuickSDKXmlPath, xmlContent)
        //修改apiadapter的路径
        def modelDir = new File(newModelPath)
        modelDir.listFiles().each {
            String javaContent = read(it.absolutePath)
            javaContent = javaContent.replace("com.quicksdk.apiadapter.model",
                    "com.quicksdk.apiadapter." + channelProjectExt.channelPackName)
            write(it.absolutePath, javaContent)
        }
        def channelPath = newModelPath.replace("model",channelProjectExt.channelPackName)
        modelDir.renameTo(channelPath)
        //修改包名
        String gradleContent = read(newGradlePath)
        gradleContent = gradleContent.replace(" applicationId \"com.quicksdk.base\"",
                " applicationId \"com.quicksdk." + channelProjectExt.channelPackName + ".demo\"")
        write(newGradlePath, gradleContent)
    }


    static def write(String destPath, String content) {
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
    static String read(String destPath) {
        List<String> lines
        try{
            // 创建目标文件
            File destFile = new File(destPath)
            destFile.withReader{ reader ->
               lines = reader.readLines()
            }
        }catch(Exception e){
            e.printStackTrace()
        }
        StringBuilder sbContent = new StringBuilder()
        if(lines != null){
            int lineIndex = 0
            lines.each {
                sbContent.append(it)
                lineIndex++
                if(lineIndex < lines.size()){
                    sbContent.append("\n")
                }
            }
        }
        return sbContent.toString()
    }
}
