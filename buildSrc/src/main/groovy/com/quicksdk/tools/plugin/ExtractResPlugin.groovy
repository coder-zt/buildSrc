package com.quicksdk.tools.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import com.quicksdk.tools.extension.JarExcludeExt
import com.quicksdk.tools.task.ChannelResourceCreateTask
import com.quicksdk.tools.utils.ChannelInfo
import com.quicksdk.tools.utils.Cmd
import com.quicksdk.tools.utils.HttpRequest

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileTree

/**
 * 本插件开发借鉴于：https://github.com/908657085/transform-aar
 * 测试实现于：E:\ChannelDemo\3011sdk\buildSrc\src\main\groovy\cn\mrobot\tools\transform\TransformLibraryPlugin.groovy
 */
class ExtractResPlugin implements Plugin<Project> {

    def Project mProject
    def List<String> projectArrFiles = new ArrayList<>()
    def List<String> tempResList = new ArrayList<>()
    def List<String> handleJarList = new ArrayList<>()
    def List<String> classesDeleteFile = new ArrayList<>()
    def String tempDir = "E:\\ResTestTemp"
    String tempResDir = tempDir + "\\res"
    String tempJarDir =  tempDir + "\\jar"
    String tempClassesDir =  tempDir + "\\classes"
    String tempAssetsDir =  tempDir + "\\assets"
    String tempLibDir =  tempDir + "\\lib"
    String tempDexDir =  tempDir + "\\dex"
    String cmdToolDir =  tempDir + "\\cmd"//src/assets/baksmali.jar
    String dxToolDir =   "\\buildSrc\\src\\assets\\dx.bat"//    String baksmaliToolDir =  E:\Coding\Android\Quick_Resource\buildSrc\src\assets
    String baksmaliToolDir =  "\\buildSrc\\src\\assets\\baksmali.jar"
    String smaliResultDir =  tempDir + "\\smali"
    JarExcludeExt ext
    String projectBasePath

    @Override
    void apply(Project project) {
        this.mProject = project
        if(mProject.parent != null){
            projectBasePath = mProject.parent.getProjectDir().absolutePath
            dxToolDir = projectBasePath + dxToolDir
            baksmaliToolDir = projectBasePath + baksmaliToolDir
        }
        println dxToolDir
        println "自定义插件开发"
        project.subprojects.each {
            if(it.name != "app"){
                it.apply plugin: ExtractResPlugin
            }
        }
        mProject.extensions.create("jarExcludeExt", JarExcludeExt.class)

        project.afterEvaluate {
            //添加需要删除classes下的文件名称
            classesDeleteFile.add("R.class");
            classesDeleteFile.add("BuildConfig.class");
            classesDeleteFile.add("TestApplication.class");
            //清理提取资源文件的目录
            cleanTempResFile()
            //创建需要的目录
            creatTemResFile()
            //获取工程项目lib下的aar文件
            getProjectArrFile()
            try {

                def jarExt = mProject.extensions.getByType(JarExcludeExt.class)
                println "jarExt ===> " + jarExt.excludeJar

                project.subprojects.each {
                    if(it.name != "app"){
                        it.jarExcludeExt{
                            for(item in jarExt.excludeJar){
                                addExcludeJar item
                            }
                        }
                    }
                }
                project.android.applicationVariants.all { applicationVariant ->
                    transformResFiles(applicationVariant)
//                    extractTempFile(applicationVariant)
                    createExtractTask(applicationVariant)
                }
            }catch(Exception e){
                e.printStackTrace()
            }
        }
    }

    /**
     * 创建提取该渠道的资源任务
     * @param variant
     */
    private void createExtractTask(ApplicationVariant variant){
        def xmlSlurper = new XmlSlurper()
        println "project path " + mProject.getProjectDir().absolutePath
        def projectPath = mProject.getProjectDir().absolutePath
        def response = xmlSlurper.parse(new File(projectPath + "\\src\\main\\assets\\quicksdk.xml"))
        def channelType = "-1";
        def channelName = "model";
        response.string.find{
            if(it.@name == 'channel_type'){
                println "quicksdk.xml: channel_type ===> " + it
                channelType = it
            }
            if(it.@name == 'quicksdk_channel_name'){
                println "quicksdk.xml: quicksdk_channel_name ===> " + it
                channelName = it
            }
        }
        mProject.tasks.create("extractChannelResource${variant.name.capitalize()}", ChannelResourceCreateTask) {
            it.group = "channelTool"
            it.description = "Extract channel resource"
            it.channelType = channelType
            it.channelName = channelName
            it.tempChannelResDir = tempDir
            it.codeVersion = "1.1.1"
        }
    }
    /**
     * 清理提取资源文件的目录
     */
    private void  cleanTempResFile(){
        tempResList.add(tempResDir)
        tempResList.add(tempJarDir)
        tempResList.add(smaliResultDir)
        tempResList.add(tempDexDir)
        tempResList.add(tempAssetsDir)
        tempResList.add(tempLibDir)
        for(res in tempResList){
            File resFile = new File(res)
            if(resFile.exists()){
                mProject.delete(resFile)
            }
        }
    }
    /**
     * 清理提取资源文件的目录
     */
    private void  creatTemResFile(){
        List<String> createResList = new ArrayList<>()
        createResList.add(smaliResultDir)
        createResList.add(tempDexDir)
        for(res in createResList){
            File resFile = new File(res)
            if(!resFile.exists()){
                resFile.mkdir()
            }
        }
    }

    /**
     * 获取工程项目lib下包含的aar文件
     */
    private void getProjectArrFile(){
        String libsPath = mProject.getProjectDir().absolutePath + '/libs'
        File libsDir = new File(libsPath)
        if(libsDir.isDirectory()){
            libsDir.traverse{
                if(it.name.endsWith('aar')){
                    projectArrFiles.add(it.name.replace('.aar',''))
                    println "libs file is ===> " + it.name
                }
            }
        }else{
            assert "指定的libs文件夹无效"
        }
    }


    /**
     * 复制res下的资源文件
     */
    private void getProjectResFile(){
        String resPath = mProject.getProjectDir().absolutePath + '/src/main/res'
        File resDir = new File(resPath)
        if(resDir.isDirectory()){
            mProject.copy {
                from resDir.absolutePath
                into tempResDir
            }
        }else{
            assert "指定的res文件夹无效"
        }
    }

    private void transformResFiles(ApplicationVariant variant) {
        println 'buildLog transformResFiles'
        extractChannelRes(variant)
        transJarToSmali(variant)
        createProjectSmali(variant)
        extractChannelAssets(variant)
    }

    /**
     * 分别hook mergeDebugAsset和packageDeug两个task，提取渠道的assets资源
     * @param variant
     */
    private void extractChannelAssets(ApplicationVariant variant) {
        //hook mergeDebugAsset task 提取assets下的文件
        String targetTaskName4 = 'merge' + variant.name.capitalize() + "Assets"
        Task targetTask4 = mProject.tasks.findByName(targetTaskName4)
        targetTask4.doFirst {task ->
            println '====== task：' + task.name + '======'
            task.inputs.files.each {
                println "input file " + it.absolutePath
                def that = it
                mProject.copy {
                    from that.absolutePath
                    into tempAssetsDir
                }
            }
            //删除quicksdk.xml
            File quicksdkXmlFile = new File(tempAssetsDir + File.separator + "quicksdk.xml")
            println "删除quicksdk.xml ===》" + quicksdkXmlFile.absolutePath
            if(quicksdkXmlFile.exists()){
                mProject.delete(quicksdkXmlFile)
            }
        }


        //hook packageDeug task 从out.jar中提取可能在jar里面的assets文件
        String targetTaskName5 = 'package' + variant.name.capitalize()
        Task targetTask5 = mProject.tasks.findByName(targetTaskName5)
        targetTask5.doFirst {task ->
            println '====== task：' + task.name + '======'
            task.inputs.files.each {
                println "input file " + it.absolutePath
                //merged_java_res\debug\out.jar
                if(it.absolutePath.contains("out.jar")){
                    def zipFile = mProject.file(it.absolutePath)
                    FileTree jarTree = mProject.zipTree(zipFile)
                    jarTree.files.each{
                        //E:\ChannelCode0(AS)\quickSDK_1778_game9917\build\tmp\expandedArchives\out.jar_d07cc2a64ec7c82281b2d67b1d51c0e9\assets\zlsioh.dat
                        if(it.absolutePath.contains("\\assets\\")){
                            def that = it
                            mProject.copy{
                                from that
                                into tempAssetsDir
                            }
                        }
                    }
                }
                //在此task下顺便提取so文件
                if(it.absolutePath.contains("stripped_native_libs\\" + variant.name.toLowerCase() + "\\out")){
                      println "out jar file " + it.absolutePath
                        def that = new File(it.absolutePath + "\\lib")
                        mProject.copy{
                            from that
                            into tempLibDir
                        }
                }
                //在此task下顺便提取minifest文件
                if(it.absolutePath.contains("packaged_manifests\\" + variant.name.toLowerCase())){
                    println "out jar file " + it.absolutePath
                    def that = new File(it.absolutePath + "\\AndroidManifest.xml")
                    mProject.copy{
                        from that
                        into tempDir
                    }
                }
            }
        }
    }

    /**
     * 创建接入工程的smali文件
     *
     * @param variant
     */
    private void createProjectSmali(ApplicationVariant variant) {
        //dexBuilderDebug
        //com/quick/apiadapter/game9917
        String targetTaskName3 = 'dexBuilder' + variant.name.capitalize()
        Task targetTask3 = mProject.tasks.findByName(targetTaskName3)
        targetTask3.doFirst {task ->
            println '====== task：' + task.name + '======'
            task.inputs.files.each {
                println "input file " + it.absolutePath
                if(it.absolutePath.endsWith('classes')){
                    def that = it
                    mProject.copy {
                        from that.absolutePath
                        into tempClassesDir
                    }
                }
            }
            //遍历classes目录，删除不需要的代码
            new File(tempClassesDir).listFiles().each{
                deleteClassOtherFile(it.absolutePath);
            }
            //将class下的*.class文件转化为smali
            //先转为*.jar文件
            //jar -cvf F:\JavaPlace\ExcRes\temp\proguard\quicksdk_channel.jar -C F:\JavaPlace\ExcRes\temp\classes/ .
            //jar -cvf E:\ResTestTemp\smali\channel.jar -C F:\JavaPlace\ExcRes\temp\classes/ .
            def cmd = "jar -cvf " + tempClassesDir + "\\quicksdk_channel.jar -C " + tempClassesDir + "/ ."
            println cmd
            Cmd.run(cmd)
            //在转为*.dex文件
            def outputDex = tempClassesDir + "\\" + "quicksdk_channel.dex"
            cmd = dxToolDir + ' --dex --output=' + outputDex + " " +  tempClassesDir + "\\quicksdk_channel.jar"
            println cmd
            Cmd.run(cmd)
            //在转为*.smali文件
            cmd = "java -jar " + baksmaliToolDir + " -x " + outputDex + " -o " + smaliResultDir
            println cmd
            Cmd.run(cmd)
        }
    }


    private boolean deleteClassOtherFile(String path){
        File handleTarget = new File(path)
        if(handleTarget.isDirectory()){
            boolean isDeleteDir
            handleTarget.listFiles().each{
                isDeleteDir = deleteClassOtherFile(it.absolutePath)
                def that = it
            }
            if(isDeleteDir || handleTarget.listFiles().size() == 0){
                mProject.delete(handleTarget);
                println "删除文件夹 ===> " + handleTarget.absolutePath
            }
        }else{
            if(classesDeleteFile.contains(handleTarget.name) || handleTarget.name.contains("R\$")){
                mProject.delete(handleTarget);
                println "删除文件 ===> " + handleTarget.absolutePath
                return true;
            }else{
                return false;
            }
        }
        return false;
    }
    /**
     * 提取项目中的jar文件并把jar转化为smali文件
     *
     * @param variant
     */
    private void transJarToSmali(ApplicationVariant variant) {
//mergeDebugJavaResource
        String targetTaskName2 = 'merge' + variant.name.capitalize() + 'JavaResource'
        Task targetTask2 = mProject.tasks.findByName(targetTaskName2)
        targetTask2.doFirst { task ->
            println '====== task：' + task.name + '======'
            task.inputs.files.each {
                File temp = it
                //排除quick的相关的库，不需要这些库的smali
                def jarExt = mProject.extensions.getByType(JarExcludeExt.class)
                if (jarExt != null && jarExt.excludeJar != null && !jarExt.excludeJar.contains(it.name)) {
                    mProject.copy {
                        from temp.absolutePath
                        into tempJarDir
                    }
                    if (it.name == 'classes.jar') {
                        def libName = it.parentFile.parentFile.name + ".jar"
                        def reNameLib = new File(tempJarDir + "\\" + libName)
                        def copiedFile = new File(tempJarDir + "\\classes.jar")
                        copiedFile.renameTo(reNameLib.absolutePath)
                        handleJarList.add(reNameLib.absolutePath)
                    } else {
                        handleJarList.add(tempJarDir + "\\" + it.name)
                    }
                } else {
                    println '排除quick中的库：' + it.name
                }
            }

            //遍历jar，将jar转化为dex
            List dexResultList = new ArrayList<>()
            for (jar in handleJarList) {

                File file = new File(jar)
                def outputDex = tempDexDir + "\\" + file.name.replace(".jar", ".dex")
                def cmd = dxToolDir + ' --dex --output=' + outputDex
                if (file.exists()) {
                    cmd += " " + jar
                    println cmd
                    dexResultList.add(outputDex)
                    Cmd.run(cmd)
                } else {
                    println "handle jar -ex " + file.absolutePath
                }
            }
            //将dex转化为smali
            for (dex in dexResultList) {
                def cmd = "java -jar " + baksmaliToolDir + " -x " + dex + " -o " + smaliResultDir
                println cmd
                Cmd.run(cmd)
            }
        }
    }

    /**
     * 提取项目中的res目录下的文件
     *
     * @param variant
     */
    private void extractChannelRes(ApplicationVariant variant) {
        String targetTaskName1 = 'merge' + variant.name.capitalize() + 'Resources'
        Task targetTask1 = mProject.tasks.findByName(targetTaskName1)
        targetTask1.doFirst { task ->
            println '====== task：' + task.name + '======'
            task.inputs.files.each {
                File temp = it
                if (projectArrFiles.contains(it.parentFile.name)) {
                    mProject.copy {
                        from temp.absolutePath
                        into tempResDir
                    }
                }
            }
            //复制工程项目res下的资源文件
            getProjectResFile()
        }
    }

    /**
     * 提取打包过程中的资源到指定目录
     *
     * @param variant
     */
    private void extractTempFile(ApplicationVariant variant) {
        println 'buildLog transformResFiles'
        String tempDir = "F:\\ResTestTemp\\Test"
//        String transformResFiles = 'generateDebugResources'
        mProject.tasks.each {
            if (it != null) {
                it.doFirst { task ->
                    println '====== task：' + task.name + '======'
                    task.inputs.files.each {

                        String taskTempDir = tempDir + "\\" + task.name
                        File taskDir = new File(taskTempDir)
                        if(!taskDir.exists()){
                            taskDir.mkdir()
                        }
                        File temp = it
                        mProject.copy {
                            from temp.absolutePath
                            into taskDir.absolutePath
                        }

                    }
                }
            }
        }

    }

}
