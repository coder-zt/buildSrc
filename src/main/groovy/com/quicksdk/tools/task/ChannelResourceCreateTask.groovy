package com.quicksdk.tools.task

import com.quicksdk.tools.extension.CreateRFileExt
import com.quicksdk.tools.utils.ChannelInfo
import com.quicksdk.tools.utils.Cmd
import com.quicksdk.tools.utils.HttpRequest
import com.quicksdk.tools.utils.JarCompiler
import com.quicksdk.tools.utils.Logger
import com.quicksdk.tools.utils.ParseManifestTool
import com.quicksdk.tools.utils.PathUtils
import com.quicksdk.tools.utils.RFileHandlerTool
import com.quicksdk.tools.utils.RJavaHandlerTool
import net.sf.json.JSONObject
import org.apache.commons.io.LineIterator;
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.IOFileFilter
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
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
    String tempHandleDir

    @Input
    String projectPackName

    @Input
    String channelResourceDir

    //是否移动到打包工具的渠道资源目录下
    @Input
    boolean move

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


    boolean test = false

    @TaskAction
    void execute(){

        if(test){
            handleStyleableClass()
            return
        }
        init()
        File oldChannelRes = new File(channelResourceDir + File.separator + resourceName)
        File tempChannelRes = new File(tempHandleDir + File.separator + resourceName)
        Logger.i("oldChannelRes:$oldChannelRes")
        Logger.i("tempChannelRes:$tempChannelRes")
        if(tempChannelRes.exists()){
            getProject().delete(tempChannelRes)
        }
        def created = createChannelResDir()
        if(created){
            createFirstDirAndFile()
            new ParseManifestTool().execute(tempChannelResDir,tempHandleDir, resourceName,projectPackName)
            moveResToFiles()
            //将工程接入代码编译为jar复制到jar中
            projectClass2Smali()
            //将jar编译为smali
            compileJar2Smali()
            //处理R.jar
            handlerRSmali()
            //复制smali文件夹到Files下
            moveSmaliToFiles()
            createDescText()
            renameValueXml()
            removeDuplicateResources(filesDir.absolutePath + "\\" + "res")
        }else{
            println "创建渠道文件夹失败"
            return
        }
        if(move){
            //移动渠道资源到打包工具的目录下
            Logger.i("移动渠道资源到打包工具的目录下")
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
    }

    def handleStyleableClass(String rJavaPath){
        def res = read(rJavaPath)
        Map<String, String> map = new HashMap()
        //public static final class attr
        String cName = ""
        res.split("\n").each {
            if(it.strip().contains("public static final class ")){
                cName = it.strip().replace("public static final class ", "")
            }
            if(it.contains(" = 0x")){
                def resLineSplit = it.split(" = 0x")
                if(resLineSplit.size() == 2){
                    def frontString = resLineSplit[0]
                    def value = resLineSplit[1].trim().replace(";","")
                    def resFront = frontString.split(" int ")
                    def varName = resFront[1]
                    String key = "0x" + value
                    map.put(key, cName + '.' + varName.trim())
                    Logger.println(key + "===" + map.get(key))
                }
            }
        }
        StringBuilder sbHandler = new StringBuilder();
        res.split("\n").each {
            String newLine = it
            if(!it.contains(" = 0x") && it.strip().startsWith("0x")){
                def oxNumberLineSplit = it.split(",")
                def that = it
                oxNumberLineSplit.each {
                    if(map.containsKey(it.trim())){
                        that = that.replace(it.trim(), map.get(it.trim()))
                    }
//                    Logger.println(it.trim() + "== ${map.size()} =" + map.get(it.trim()))
                }
                newLine = that
            }
            sbHandler.append(newLine).append("\n")
//            Logger.i(newLine)
        }
        write(rJavaPath, sbHandler.toString())
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
        channelResDir = tempHandleDir + File.separator + resourceName
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
     */
    void moveResToFiles(){
        List<String> resDirNames = new ArrayList<>();
        resDirNames.add("assets")
        resDirNames.add("lib")
        resDirNames.add("res")
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

    private void compileJar2Smali(){
        def jarPath = tempChannelResDir + "//jar"
        def jarDirs = new File(jarPath)
        if(!jarDirs.exists()){
            return
        }
        def jarFiles = jarDirs.listFiles()
        Logger.i("compileJar2Smali jar file size => " + jarFiles.size())
        JarCompiler.check(tempHandleDir + "\\temp")
        def quickChannelJar = null
        for(jarFile in jarFiles){
            //quicksdk_channel.jar先不编译，因为后面会删除com/quicksdk下多余R$*.smali文件
            if(jarFile.name == "quicksdk_channel.jar"){
                quickChannelJar = jarFile
                continue
            }
            JarCompiler.execute(jarFile, tempHandleDir + "\\temp")
        }
        //清除R.jar生成的com\quicksdk下的文件
        def quickSDKDir = tempHandleDir + "\\temp\\smali\\com\\quicksdk"
        def quickSDKFile = new File(quickSDKDir)
        if(quickSDKFile.exists()){
           getProject().delete(quickSDKFile)
        }
        if(quickChannelJar != null){
            JarCompiler.execute(quickChannelJar, tempHandleDir + "\\temp")
        }
    }

    private void handlerRSmali(){
        def createRFileExt = getProject().extensions.getByType(CreateRFileExt.class)
        if(createRFileExt.genPackList.size() < 0){
            return
        }
        String pack = "no_pack"
        if(createRFileExt.genPackList.contains("pack")){
            pack = getProject().android.defaultConfig.applicationId

        }
        def result = extractResJarClass()
        result.each {
            if (createRFileExt.genPackList.contains(it.key) || it.key == pack) {
                //jad -o -r -s java -d src classes/**/*.class
                StringBuilder sbCmd = new StringBuilder( PathUtils.getInstance().getJadToolDir())
                sbCmd.append(" -o -r -s java -d ")
                sbCmd.append(PathUtils.getInstance().getTempResJarDir())
                sbCmd.append(" ")
                sbCmd.append(it.value)
                sbCmd.append(File.separator)
                sbCmd.append("*.class")
                //println sbCmd.toString()
//E:\ChannelCode0\buildSrc\src\assets\jad -o -r -s java -d E:\ResTestTemp\res-jar E:\ResTestTemp\res-jar\com\baidu\passport\sapi2\*.class
                Cmd.run(sbCmd.toString())
                //得到的R.java文件：
                //it.value + "\\R.java"
                //对R.java文件进行特殊处理
                def RJavaPath = it.value + "\\R.java"
                handleStyleableClass(RJavaPath)
                handleJavaFile2Jar(it.getKey(), RJavaPath, it.key == pack)
            }
        }
    }

    /**
     * 对R.java进行特殊处理，将十六进制的值转为ActivityAdapter中的getResId()获取
     * @param javaPath
     */
    def handleJavaFile2Jar(String packName,String javaPath, boolean isPack){
        Logger.i("packName === > $packName")
        Logger.i("javaPath === > $javaPath")
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
            def nextDirName = packName.split("\\.")[0]

            getProject().copy{
                from PathUtils.getInstance().getSmaliResResultDir() + File.separator + nextDirName
                into PathUtils.getInstance().getSmaliResultDir() + File.separator + nextDirName
            }
        }else{
            getProject().copy{
                from PathUtils.getInstance().getSmaliResResultDir() +"\\R"
                into PathUtils.getInstance().getTempDir() + "\\temp\\R"
            }
        }
    }

    private String readChannelInfo(){
        String channelName = ""
        String channelType = ""
        def projectPath = getProject().getProjectDir().absolutePath
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

    private Map<String, String>  extractResJarClass(){
        //处理复制后的res.jar,提取jar中的R$.class
        def rJarPath = PathUtils.getInstance().getTempResJarDir() + File.separator + "R.jar"
        if(!new File(rJarPath).exists()){
            return
        }
        def zipFile = getProject().file(rJarPath)
        FileTree jarTree = getProject().zipTree(zipFile)
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
            getProject().copy {
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

    void moveSmaliToFiles(){
        List<String> resDirNames = new ArrayList<>()
        resDirNames.add("R")
        resDirNames.add("smali")
        resDirNames.each{
            def that = it
            File resFile = new File(tempHandleDir + "\\temp\\" + that)
            if(resFile.exists()){
                if(it == "R"){//处理R文件夹的文件，替换包名
                    File[] files = resFile.listFiles();
                    for (File file : files) {
                        String content = FileUtils.readFileToString(file, "UTF-8").replaceAll("LR/R", "L{{\\\$packNamePath}}/R");
                        FileUtils.writeStringToFile(file, content, "UTF-8");
                    }
                }
                if(it == "smali"){//处理smali文件夹的文件，判断有不有smali\com\bun
                    boolean hasOaid = new File(tempHandleDir + "\\temp\\smali\\com\\bun").exists()
                    oaid = hasOaid?1:0
                }
                getProject().copy {
                    from tempHandleDir + "\\temp\\" + that
                    into filesDir.absolutePath + "\\" + that
                }
            }
        }
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

    /**
     * 将接入工程的的class文件转为jar，并复制到jar目录中
     */
    private void projectClass2Smali() {
        //转为*.jar文件
        //jar -cvf F:\JavaPlace\ExcRes\temp\proguard\quicksdk_channel.jar -C F:\JavaPlace\ExcRes\temp\classes/ .
        //jar -cvf E:\ResTestTemp\smali\channel.jar -C F:\JavaPlace\ExcRes\temp\classes/ .
        def channelJarPath = PathUtils.getInstance().getTempClassesDir() + "\\quicksdk_channel.jar";
        def cmd = "jar -cvf " + channelJarPath + " -C " + PathUtils.getInstance().getTempClassesDir() + "/ ."
        Cmd.run(cmd)
        getProject().copy {
            from new File(channelJarPath)
            into tempChannelResDir + "//jar"
        }
    }

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
