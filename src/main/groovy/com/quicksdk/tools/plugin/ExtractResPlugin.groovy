package com.quicksdk.tools.plugin

import com.android.build.gradle.api.ApplicationVariant
import com.quicksdk.tools.extension.ChannelProjectExt
import com.quicksdk.tools.extension.ChannelToolExt
import com.quicksdk.tools.extension.CreateRFileExt
import com.quicksdk.tools.extension.JarExcludeExt
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
 */
class ExtractResPlugin implements Plugin<Project> {

    Project mProject
    def projectArrFiles = new ArrayList<>()
    def tempResList = new ArrayList<>()
    def handleJarList = new ArrayList<>()
    def classesDeleteFile = new ArrayList<>()

    @Override
    void apply(Project project) {
        this.mProject = project


        if(mProject.parent != null){
            def projectBasePath = mProject.parent.getProjectDir().absolutePath
            PathUtils.getInstance().initToolPath(projectBasePath)
        }
        mProject.extensions.create("jarExcludeExt", JarExcludeExt.class)
        mProject.extensions.create("newChannelBean", ChannelProjectExt.class)
        mProject.extensions.create("channelToolInfo", ChannelToolExt.class)
        mProject.extensions.create("genRFiles", CreateRFileExt.class)
        project.afterEvaluate {
            //子工程添加插件
            def jarExt = mProject.extensions.getByType(JarExcludeExt.class)
            def channelInfoExt = mProject.extensions.getByType(ChannelToolExt.class)
            project.subprojects.each {
                if(!jarExt.excludAapplication.toString().contains(it.name)){
                    it.apply plugin: ExtractResPlugin
                }
            }
            try {
                //子工程添加不需要处理的jar
                project.subprojects.each {
                    if(!jarExt.excludAapplication.toString().contains(it.name)){
                        it.jarExcludeExt{
                            for(item in jarExt.excludeJar){
                                addExcludeJar item
                            }
                        }
                        it.channelToolInfo{
                            channelResourceDir = channelInfoExt.channelResourceDir
                            //提取打包过程中的临时文件目录
                            channelTempDir = channelInfoExt.channelTempDir
                        }
                    }
                }
                if(project.hasProperty("android")){
                    project.android.applicationVariants.all { applicationVariant ->
                        def channelToolExt = mProject.extensions.getByType(ChannelToolExt.class)
                        PathUtils.getInstance().initFilePath(channelToolExt.channelTempDir)
                        //preBuild
                        String targetTaskName0 = 'preBuild'
                        Task targetTask0 = mProject.tasks.findByName(targetTaskName0)
                        targetTask0.doFirst { task ->
                            println("hook preBuild 初始化提取工具")
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
                        }
                        transformResFiles(applicationVariant)
                        createExtractTask(applicationVariant)
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
        println "project path " + mProject.getProjectDir().absolutePath
        String[] channelInfo = readChannelInfo().split("====")
        def channelType = channelInfo[0]
        def channelName = channelInfo[1]

        def channelToolExt = mProject.extensions.getByType(ChannelToolExt.class)
        def channelResourceDir = channelToolExt.channelResourceDir
        println "channelResourceDir ===> " + channelResourceDir
        mProject.tasks.create("extractChannelResource${variant.name.capitalize()}", ChannelResourceCreateTask) {
            it.group = "channelTool"
            it.description = "Extract channel resource"
            it.channelType = channelType
            it.channelName = channelName
            it.projectPackName = mProject.android.defaultConfig.applicationId
            it.tempChannelResDir = PathUtils.getInstance().getTempDir()
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
                println "quicksdk.xml: channel_type ===> " + it
                channelType = it
            }
            if(it.@name == 'quicksdk_channel_name'){
                println "quicksdk.xml: quicksdk_channel_name ===> " + it
                channelName = it
            }
        }

        return  channelType + "====" + channelName
    }

    /**
     * 清理提取资源文件的目录
     */
    private void  cleanTempResFile(){
        tempResList.add(PathUtils.getInstance().getTempResDir())
        tempResList.add(PathUtils.getInstance().getTempJarDir())
        tempResList.add(PathUtils.getInstance().getSmaliResultDir())
        tempResList.add(PathUtils.getInstance().getTempClassesDir())
        tempResList.add(PathUtils.getInstance().getTempManifestFile())
        tempResList.add(PathUtils.getInstance().getTempDexDir())
        tempResList.add(PathUtils.getInstance().getTempAssetsDir())
        tempResList.add(PathUtils.getInstance().getTempResJarDir())
        tempResList.add(PathUtils.getInstance().getTempLibDir())
        tempResList.add(PathUtils.getInstance().getTempRDir())
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
        createResList.add(PathUtils.getInstance().getSmaliResultDir())
        createResList.add(PathUtils.getInstance().getTempDexDir())
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
                into PathUtils.getInstance().getTempResDir()
            }
            renameResValuesFiles("csdk")
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
     * 对R.java进行特殊处理，将十六进制的值转为ActivityAdapter中的getResId()获取
     * @param javaPath
     */
     def handleJavaFile2Jar(String packName,String javaPath, boolean isPack){
        String channelName = readChannelInfo().split("====")[1]
         if (isPack){
             packName = "R"
         }
        String activityAdapter = "com.quicksdk.apiadapter." + channelName
         //处理R.java进行的资源获取方式
        new RFileHandlerTool().execute(packName, activityAdapter, javaPath)
         //将java编译为R$*.class
         //需要channel.jar和quicksdk.jar
       new RJavaHandlerTool().execute(javaPath, PathUtils.getInstance().getTempClassesDir() + "\\quicksdk_channel.jar",
               PathUtils.getInstance().getQuickSDKJarDir(), PathUtils.getInstance().getTempResJarDir())
         //将生成的R$*.smali文件复制smali文件下
         if(!isPack){
             mProject.copy{
                 from PathUtils.getInstance().getSmaliResResultDir()
                 into PathUtils.getInstance().getSmaliResultDir()
             }
         }else{
             mProject.copy{
                 from PathUtils.getInstance().getSmaliResResultDir()
                 into PathUtils.getInstance().getTempDir()
              }
         }
    }

    static String getPack(String RClassFile){
        StringBuilder sbPack = new StringBuilder()
        String[] splits = RClassFile.split("\\\\")
        for(int i=0;i<splits.size(); i++){
            sbPack.append(splits[i])
            if(i < splits.size() - 2){
                sbPack.append(".")
            }else{
                break
            }
        }
        return sbPack.toString()
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
//                println "input file " + it.absolutePath
                def that = it
                mProject.copy {
                    from that.absolutePath
                    into PathUtils.getInstance().getTempAssetsDir()
                }
            }
            //删除quicksdk.xml
            File quicksdkXmlFile = new File(PathUtils.getInstance().getTempAssetsDir() + File.separator + "quicksdk.xml")
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
//                println "input file " + it.absolutePath
                //merged_java_res\debug\out.jar
                if(it.absolutePath.contains("out.jar")){
                    def zipFile = mProject.file(it.absolutePath)
                    FileTree jarTree = mProject.zipTree(zipFile)
                    jarTree.files.each{
                        //E:\ChannelCode0(AS)\quickSDK_1778_game9917\build\tmp\expandedArchives\out.jar_d07cc2a64ec7c82281b2d67b1d51c0e9\assets\zlsioh.dat
                        if(it.absolutePath.contains("\\assets\\")){
                            println "out.jar files ===> " + it.absolutePath
                            def that = it
                            //获取文件的父目录，如果不是assets，需要添加父目录，直到assets的下一级
                            def middleDir = ""
                            def parent = that.parentFile
                            while(true){
                                if(parent.name.equals("assets")){
                                    break;
                                }
                                middleDir = "\\" + parent.name + middleDir
                                parent = parent.parentFile
                            }
                            println "assets tagert file dir ===> " + PathUtils.getInstance().getTempAssetsDir() + middleDir
                            mProject.copy{
                                from that
                                into PathUtils.getInstance().getTempAssetsDir() + middleDir
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
                            into PathUtils.getInstance().getTempLibDir()
                        }
                }
                //在此task下顺便提取minifest文件
                if(it.absolutePath.contains("packaged_manifests\\" + variant.name.toLowerCase())){
                    println "out jar file " + it.absolutePath
                    def that = new File(it.absolutePath + "\\AndroidManifest.xml")
                    mProject.copy{
                        from that
                        into PathUtils.getInstance().getTempDir()
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
                def that = it
                //将接入工程中的*.class文件复制的临时目录
                if(it.absolutePath.endsWith('classes')){
                    mProject.copy {
                        from that.absolutePath
                        into PathUtils.getInstance().getTempClassesDir()
                    }
                }
                //将R.jar文件复制到临时目录
                if(it.name == "R.jar") {
                    mProject.copy {
                        from that.absolutePath
                        into PathUtils.getInstance().getTempResJarDir()
                    }
                }
            }
            projectClass2Smali()
            Map<String, String> allRFilePacks = extractResJarClass()
            classes2Java(allRFilePacks)

        }
    }

    /**
     * 将R$*.class文件转化为java文件
     * @param allRFilePacks
     * @return
     */
    private void classes2Java(Map<String, String> allRFilePacks){
        def createRFileExt = mProject.extensions.getByType(CreateRFileExt.class)
        if(createRFileExt.genPackList.size() < 0){
            return
        }
        String pack = "no_pack"
        if(createRFileExt.genPackList.contains("pack")){
            pack = mProject.android.defaultConfig.applicationId
        }
        println "classes2Java 需要处理的res文件的包名：" + createRFileExt.genPackList
        //处理已经注册的包名
        allRFilePacks.each {
            println "classes2Java 有R.java的包名：" + it.key
            if (createRFileExt.genPackList.contains(it.key) || it.key == pack) {
                //jad -o -r -s java -d src classes/**/*.class
                StringBuilder sbCmd = new StringBuilder( PathUtils.getInstance().getJadToolDir())
                sbCmd.append(" -o -r -s java -d ")
                sbCmd.append(PathUtils.getInstance().getTempResJarDir())
                sbCmd.append(" ")
                sbCmd.append(it.value)
                sbCmd.append(File.separator)
                sbCmd.append("*.class")
                println sbCmd.toString()
//E:\ChannelCode0\buildSrc\src\assets\jad -o -r -s java -d E:\ResTestTemp\res-jar E:\ResTestTemp\res-jar\com\baidu\passport\sapi2\*.class
                Cmd.run(sbCmd.toString())
                //得到的R.java文件：
                //it.value + "\\R.java"
                //对R.java文件进行特殊处理
                def RJavaPath = it.value + "\\R.java"
                handleJavaFile2Jar(it.getKey(), RJavaPath, it.key == pack)
            }
        }
    }

    private Map<String, String>  extractResJarClass(){
        //处理复制后的res.jar,提取jar中的R$.class
        def rJarPath = PathUtils.getInstance().getTempResJarDir() + File.separator + "R.jar"
        def zipFile = mProject.file(rJarPath)
        FileTree jarTree = mProject.zipTree(zipFile)
        Map<String, String> allRFilePacks = new HashMap<>()
        jarTree.files.each {
            def it2 = it
            //E:\ChannelCode0\quicksdk_14_baidu\build\tmp\expandedArchives\R.jar_f83205a88863c5ede8568c111a248598\com\game\dubuwulin\g\baidu\R$id.class
            def jarInFilePath = it.absolutePath
            def classFilePath = new StringBuilder()
            String[] splits = jarInFilePath.split("R\\.jar_")
            def appendable = false
            if (splits.size() == 2) {
                splits[1].each {
                    if (appendable) {
                        classFilePath.append(it)
                    } else if (it == '\\') {
                        appendable = true
                    }
                }
            }
            def targetClassFile = new File(PathUtils.getInstance().getTempResJarDir() + File.separator + classFilePath.toString())
            mProject.copy {
                from it2.absolutePath
                into targetClassFile.parentFile.absolutePath
            }
            //将不同的R文件路径与包名关联起来
            def packR = getPack(classFilePath.toString())
            if (!allRFilePacks.keySet().contains(packR)) {
                allRFilePacks.put(packR, targetClassFile.parentFile.absolutePath)
            }
        }
        return allRFilePacks
    }

    /**
     * 将接入工程的的class文件转为smali
     */
    private void projectClass2Smali() {
        //遍历classes目录，删除不需要的代码
        new File(PathUtils.getInstance().getTempClassesDir()).listFiles().each {
            deleteClassOtherFile(it.absolutePath);
        }
        //将class下的*.class文件转化为smali
        //先转为*.jar文件
        //jar -cvf F:\JavaPlace\ExcRes\temp\proguard\quicksdk_channel.jar -C F:\JavaPlace\ExcRes\temp\classes/ .
        //jar -cvf E:\ResTestTemp\smali\channel.jar -C F:\JavaPlace\ExcRes\temp\classes/ .
        def cmd = "jar -cvf " + PathUtils.getInstance().getTempClassesDir() + "\\quicksdk_channel.jar -C " + PathUtils.getInstance().getTempClassesDir() + "/ ."
        println cmd
        Cmd.run(cmd)
        //在转为*.dex文件
        def outputDex = PathUtils.getInstance().getTempClassesDir() + "\\" + "quicksdk_channel.dex"
        cmd =  PathUtils.getInstance().getDxToolDir() + ' --dex --output=' + outputDex + " " + PathUtils.getInstance().getTempClassesDir() + "\\quicksdk_channel.jar"
        println cmd
        Cmd.run(cmd)
        //在转为*.smali文件
        cmd = "java -jar " +  PathUtils.getInstance().getBaksmaliToolDir() + " -x " + outputDex + " -o " + PathUtils.getInstance().getSmaliResultDir()
        println cmd
        Cmd.run(cmd)
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
                mProject.delete(handleTarget)
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
                        into PathUtils.getInstance().getTempJarDir()
                    }
                    if (it.name == 'classes.jar') {
                        def libName = it.parentFile.parentFile.name + ".jar"
                        def reNameLib = new File(PathUtils.getInstance().getTempJarDir() + "\\" + libName)
                        def copiedFile = new File(PathUtils.getInstance().getTempJarDir() + "\\classes.jar")
                        copiedFile.renameTo(reNameLib.absolutePath)
                        handleJarList.add(reNameLib.absolutePath)
                    } else {
                        handleJarList.add(PathUtils.getInstance().getTempJarDir() + "\\" + it.name)
                    }
                } else {
                    println '排除quick中的库：' + it.name
                }
            }

            //遍历jar，将jar转化为dex
            List dexResultList = new ArrayList<>()
            for (jar in handleJarList) {

                File file = new File(jar)
                def outputDex = PathUtils.getInstance().getTempDexDir() + "\\" + file.name.replace(".jar", ".dex")
                def cmd =  PathUtils.getInstance().getDxToolDir() + ' --dex --output=' + outputDex
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
                def cmd = "java -jar " +  PathUtils.getInstance().getBaksmaliToolDir() + " -x " + dex + " -o " + PathUtils.getInstance().getSmaliResultDir()
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
                        into PathUtils.getInstance().getTempResDir()
                    }
                }
                //res/values下values.xml可能会被同名的文件覆盖
                renameResValuesFiles(it.parentFile.name)
            }
            //复制工程项目res下的资源文件
            getProjectResFile()
        }
    }

    /**
     * res/values下values.xml可能会被同名的文件覆盖
     */
    def renameResValuesFiles(String parentName){
        new File(PathUtils.getInstance().getTempResDir()).listFiles().each {
            if(it.name.startsWith("values")){
                def that = it
                it.listFiles().each {
                    if(it.name == that.name + ".xml"){
                        def newName = parentName + "_" + it.name
                        println it.parentFile.absolutePath + File.separator + newName
                        it.renameTo(it.parentFile.absolutePath + File.separator + newName)
                    }
                }
            }
        }
    }

    /**
     * 提取打包过程中的资源到指定目录
     *
     * @param variant
     */
    private void extractTempFile(ApplicationVariant variant) {
//        String transformResFiles = 'generateDebugResources'
        mProject.tasks.each {
            if (it != null) {
                it.doFirst { task ->
                    println '====== task：' + task.name + '======'
                    task.inputs.files.each {

                        String taskTempDir = PathUtils.getInstance().getTempDir() + "\\" + task.name
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

    /**
     * 通过读取SdkAdapter.java获取资源版本
     * @param sdkAdapterPath
     * @return
     */
    String getChannelVersionFromCode(String sdkAdapterPath) {
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