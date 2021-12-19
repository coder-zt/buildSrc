package com.quicksdk.tools.task

import com.quicksdk.tools.utils.ChannelInfo
import com.quicksdk.tools.utils.HttpRequest
import com.quicksdk.tools.utils.ParseManifestTool
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


    String oaid

    String resourceVersion

    String resourceName

    String channelResDir

    @TaskAction
    void execute(){
        init()
        new ParseManifestTool().execute()
//        def created = createChannelResDir()
//        if(created){
//            createFirstDirAndFile()
//        }else{
//            println "创建渠道文件夹失败"
//            return
//        }
    }

    /**
     * 获取所有渠道额信息，初始化该渠道资源信息
     */
    void init(){
        String channelsInfo = HttpRequest.getChannelInfos()
        ChannelInfo.init(channelsInfo, channelType)
        resourceName = ChannelInfo.getResourceName()
        resourceVersion = ChannelInfo.getResourceVersion()
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
        File filesDir = new File(channelResDir + File.separator + "files")
        filesDir.mkdir()
        //生成description.txt
        oaid = "0"
        //{"versionNo":"90","channelVersion":"1.8.0.0","OAID":0,"aboveVersion":200,"needAddBaseLib":"1"}
        def descPath = channelResDir + File.separator + "description.txt"
        JSONObject descJson = new JSONObject()
        descJson.put("versionNo", resourceVersion)
        descJson.put("channelVersion", codeVersion)
        descJson.put("OAID", oaid)
        descJson.put("aboveVersion", 200)
        descJson.put("needAddBaseLib", 1)
        write(descPath, descJson.toString())
    }

    def write(String destPath, String content) {
        println "ChannelResourceCreateTask-write"

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
