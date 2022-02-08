package com.quicksdk.tools.plugin

import com.android.build.gradle.api.ApplicationVariant
import com.quicksdk.tools.extension.ChannelProjectExt
import com.quicksdk.tools.extension.ChannelToolExt
import com.quicksdk.tools.extension.CreateRFileExt
import com.quicksdk.tools.extension.JarExcludeExt
import com.quicksdk.tools.hook.DexBuilderHook
import com.quicksdk.tools.hook.MergeAssetsHook
import com.quicksdk.tools.hook.MergeJavaResourceHook
import com.quicksdk.tools.hook.MergeResourcesHook
import com.quicksdk.tools.hook.PackageHook
import com.quicksdk.tools.hook.PerBuildHook
import com.quicksdk.tools.task.ChannelResourceCreateTask
import com.quicksdk.tools.task.ChannelProjectGenerateTask
import com.quicksdk.tools.utils.Cmd
import com.quicksdk.tools.utils.PathUtils
import com.quicksdk.tools.utils.RFileHandlerTool
import com.quicksdk.tools.utils.RJavaHandlerTool
import org.apache.commons.io.FileUtils
import org.apache.commons.io.LineIterator
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileTree

/**
 * 本插件开发借鉴于：https://github.com/908657085/transform-aar
 * 测试实现于：E:\ChannelDemo\3011sdk\buildSrc\src\main\groovy\cn\mrobot\tools\transform\TransformLibraryPlugin.groovy
 *
 * temp
 *  channelResource
 *  handleTempResource
 *  extractResource
 */
class ExtractResPlugin implements Plugin<Project> {

    Project mProject
    def projectArrFiles = new ArrayList<>()
    def classesDeleteFile = new ArrayList<>()
    def extractTaskTempFile = false

    @Override
    void apply(Project project) {
        this.mProject = project
        //初始化打包工具的路径
        if(mProject.parent != null){
            def projectBasePath = mProject.parent.getProjectDir().absolutePath
            PathUtils.getInstance().initToolPath(projectBasePath)
        }
        createExt()
        project.afterEvaluate {
            try {
                //将父工程中配置的参数下发给子工程
                distributeParams()
                def channelToolExt = mProject.extensions.getByType(ChannelToolExt.class)
                PathUtils.instance.initFilePath(channelToolExt.channelTempDir)
                if(project.hasProperty("android")){
                    project.android.applicationVariants.all { applicationVariant ->
                        if(extractTaskTempFile){
                            extractTempFile(applicationVariant)
                        }else{
                            hookKeyTasks(applicationVariant)
                            createExtractTask(applicationVariant)
                        }
                    }
                }else{
                    if(project.subprojects.size() > 0){
                        createChannelProjectTask()
                    }
                }
            }catch(Exception e){
                e.printStackTrace()
            }
        }
    }

    /**
     * hook关键task获取对应的资源
     *
     * @param applicationVariant
     */
    private void hookKeyTasks(ApplicationVariant applicationVariant) {
        //hook preBuild 初始化相关参数
        new PerBuildHook(applicationVariant, mProject).hook()
        new MergeResourcesHook(applicationVariant, mProject).hook()
        new MergeJavaResourceHook(applicationVariant, mProject).hook()
        new DexBuilderHook(applicationVariant, mProject).hook()
        new MergeAssetsHook(applicationVariant, mProject).hook()
        new PackageHook(applicationVariant, mProject).hook()
    }

    /**
     * 创建extension参数
     *
     * @param mProject
     */
    private void createExt() {
        mProject.extensions.create("jarExcludeExt", JarExcludeExt.class)
        mProject.extensions.create("newChannelBean", ChannelProjectExt.class)
        mProject.extensions.create("channelToolInfo", ChannelToolExt.class)
        mProject.extensions.create("genRFiles", CreateRFileExt.class)
    }

    /**
     * 将父工程中配置的参数下发给各个子工程
     *
     * @return
     */
    private Object distributeParams() {
        //子工程添加插件
        def jarExt = mProject.extensions.getByType(JarExcludeExt.class)
        def channelInfoExt = mProject.extensions.getByType(ChannelToolExt.class)
        mProject.subprojects.each {
            if(!jarExt.excludAapplication.toString().contains(it.name)){
                it.apply plugin: ExtractResPlugin
            }
        }
        mProject.subprojects.each {
            if (!jarExt.excludAapplication.toString().contains(it.name)) {
                it.jarExcludeExt {
                    for (item in jarExt.excludeJar) {
                        addExcludeJar item
                    }
                }
                it.channelToolInfo {
                    channelResourceDir = channelInfoExt.channelResourceDir
                    //提取打包过程中的临时文件目录
                    channelTempDir = channelInfoExt.channelTempDir
                }
            }
        }
    }

    /**
     * 创建生成新渠道的接入项目的任务
     * @param variant
     */
    private void createChannelProjectTask(){
        def channelExt = mProject.extensions.getByType(ChannelProjectExt.class)
        if(channelExt == null){
            return
        }
        mProject.tasks.create("ChannelProjectGenerator", ChannelProjectGenerateTask) {
            it.group = "channelTool"
            it.description = "generate channel project"
            it.demoProjectPath = mProject.getProjectDir().absolutePath + "\\quickSDK_0_model"
            it.channelProjectExt = channelExt
        }
    }


    /**
     * 创建提取该渠道的资源任务
     * @param variant
     */
    private void createExtractTask(ApplicationVariant variant){
        String[] channelInfo = readChannelInfo().split("====")
        def channelType = channelInfo[0]
        def channelName = channelInfo[1]

        def channelToolExt = mProject.extensions.getByType(ChannelToolExt.class)
        def channelResourceDir = channelToolExt.channelResourceDir
        //println "channelResourceDir ===> " + channelResourceDir
        mProject.tasks.create("extractChannelResource${variant.name.capitalize()}", ChannelResourceCreateTask) {
            it.group = "channelTool"
            it.description = "Extract channel resource"
            it.channelType = channelType
            it.channelName = channelName
            it.projectPackName = mProject.android.defaultConfig.applicationId
            it.tempChannelResDir = PathUtils.getInstance().getManifestFileDir()
            it.tempHandleDir = PathUtils.getInstance().getTempDir()
            it.channelResourceDir = channelResourceDir
            it.codeVersion =
                    getChannelVersionFromCode(
                            mProject.getProjectDir().absolutePath +
                                    '\\src\\main\\java\\com\\quicksdk\\apiadapter\\' + channelName + '\\SdkAdapter.java')
        }
    }


     String readChannelInfo(){
        String channelName = ""
        String channelType = ""
        def projectPath = mProject.getProjectDir().absolutePath
        def xmlSlurper = new XmlSlurper()
        def response = xmlSlurper.parse(new File(projectPath + "\\src\\main\\assets\\quicksdk.xml"))
        response.string.find{
            if(it.@name == 'channel_type'){
                //println "quicksdk.xml: channel_type ===> " + it
                channelType = it
            }
            if(it.@name == 'quicksdk_channel_name'){
                //println "quicksdk.xml: quicksdk_channel_name ===> " + it
                channelName = it
            }
        }

        return  channelType + "====" + channelName
    }


    /**
     * 提取打包过程中的资源到指定目录
     *
     * @param variant
     */
    private void extractTempFile(ApplicationVariant variant) {
        def tempFileDir = new File("F:\\GradlePlugin")
        if(!tempFileDir.exists()){
            return
        }
        mProject.tasks.each {
            if (it != null) {
                def taskDir = new File(tempFileDir.absolutePath + File.separator + it.name)
                if(!taskDir.exists()){
                    taskDir.mkdir()
                }
                it.doFirst { task ->
                    //println '====== task：' + task.name + '======'
                    task.inputs.files.each {
                        def that = it
                        def putFileDir = new File(taskDir.absolutePath + "\\input")
                        if(!putFileDir.exists()){
                            putFileDir.mkdir()
                        }
                        mProject.copy {
                            from  that.absolutePath
                            into  taskDir.absolutePath + "\\input"
                        }
                    }
                }
                it.doLast { task ->
                    //println '====== task：' + task.name + '======'
                    task.outputs.files.each {
                        def that = it
                        def putFileDir = new File(taskDir.absolutePath + "\\output")
                        if(!putFileDir.exists()){
                            putFileDir.mkdir()
                        }
                        mProject.copy {
                            from  that.absolutePath
                            into  taskDir.absolutePath + "\\output"
                        }
                    }
                }
            }
        }
    }

    /**
     * 通过读取SdkAdapter.java获取资源版本
     * @param sdkAdapterPath
     * @return
     */
    static String getChannelVersionFromCode(String sdkAdapterPath) {
        if (!"".equals(sdkAdapterPath)) {
            File file = new File(sdkAdapterPath);
            LineIterator iter;
            try {
                iter = FileUtils.lineIterator(file, "UTF-8");
                String line = "";
                while (iter.hasNext()) {
                    line = iter.next();
                    if (line.contains("getChannelSdkVersion()")) {
                        String next = iter.next();
                        if (next.contains("return")) {
                            String version = next.substring(next.indexOf("\"") + 1, next.lastIndexOf("\""));
                            return version;
                        }
                    }
                }
                iter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "";
    }
}