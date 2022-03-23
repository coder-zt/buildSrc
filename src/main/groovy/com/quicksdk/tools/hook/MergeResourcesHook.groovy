package com.quicksdk.tools.hook

import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.ide.dependencies.ArtifactUtils
import com.android.build.gradle.internal.scope.VariantScopeImpl
import com.quicksdk.tools.extension.ChannelToolExt
import com.quicksdk.tools.utils.DataMan
import com.quicksdk.tools.utils.Logger
import com.quicksdk.tools.utils.PathUtils
import groovy.util.slurpersupport.NodeChild
import groovy.xml.XmlUtil
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.internal.artifacts.dependencies.DefaultClientModule
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.internal.artifacts.dependencies.DefaultProjectDependency
import org.gradle.api.internal.artifacts.dependencies.DefaultSelfResolvingDependency
import org.gradle.api.internal.artifacts.dsl.DefaultComponentModuleMetadataHandler
import org.gradle.internal.impldep.org.sonatype.aether.util.artifact.ArtifacIdUtils

import javax.xml.crypto.Data

/**
 * hook mergeResources task
 * 提取工程或者aar文件的资源文件（res）文件
 * 重命名values下的文件，避免values.xml被覆盖
 */
class MergeResourcesHook extends HookTask{

    private static String hookTaskName = 'mergeVariantResources'

    private ApplicationVariant mVariant = null
    MergeResourcesHook(ApplicationVariant variant, Project project) {
        super(hookTaskName, project)
        mVariant = variant
        taskName = hookTaskName.replace("Variant",  variant.name.capitalize())
//        showAllDeps(mProject, mVariant)
    }

    @Override
    void doFirst(Task task) {

        Logger.i("hook ${task.name} 提取res目录下的文件")
        List<String> dependencies = getProjectDependencies(mProject)
//        Logger.i("===-start-本项目依赖的第三方库-start-===")
        StringBuilder sbDependencies = new StringBuilder()
        dependencies.each {
            if(sbDependencies.length()>0){
                sbDependencies.append("&")
            }
            sbDependencies.append(it)
        }
//        Logger.i("===-end-本项目依赖的第三方库-end-===")
        task.inputs.files.each {
            File temp = it
            def libName = it.parentFile.name.replace("jetified-", "")
            //过滤掉quicksdk的资源
            //todo: 无法提取项目res下的文件
//            Logger.i("${task.name} <=====> $libName <=====> ${it.absolutePath}")

            if (isExtractLibRes(sbDependencies.toString(),libName) || libName == "main") {
                Logger.i("提取res的库 ===> $libName}")
                mProject.copy {
                    from temp.absolutePath
                    into PathUtils.getInstance().getTempResDir()
                }
                renameResValuesFiles(libName)
            }
        }
    }

    static def isExtractLibRes(String dependencies,String libName) {
        Logger.i("dependencies === $dependencies")
       return dependencies.contains(libName)
    }
    /**
     * res/values下values.xml可能会被同名的文件覆盖
     */
    static def renameResValuesFiles(String parentName){
        new File(PathUtils.getInstance().getTempResDir()).listFiles().each {
            if(isRepeatFileName(it.name)){
                def that = it
                it.listFiles().each {
                    def newName = ""
                    if(it.name == that.name + ".xml"){
                        if(parentName == "main"){
                            newName = "csdk_" + it.name
                            it.renameTo(it.parentFile.absolutePath + File.separator + newName)
                        }else{
                            newName = it.name.replace(".xml","(" + parentName + ").xml")
                            it.renameTo(it.parentFile.absolutePath + File.separator + newName)
                        }
                    }
                    def xmlSlurper = new XmlSlurper()
                    if(newName.empty){
                        return
                    }
                    def response = xmlSlurper.parse(it.parentFile.absolutePath + File.separator + newName)
                    NodeChild deleteNode = null
                    response.depthFirst().each { NodeChild i ->
                       if(i.attributes()["name"] == "app_name"){
                           deleteNode = i
                           Logger.i(deleteNode.text())
                        }
                    }
                    if(deleteNode == null){
                        return
                    }
                    def result = read(it.parentFile.absolutePath + File.separator + newName)
                            .replace("\n<string name=\"app_name\">$deleteNode</string>", "")
                    def newFile = new File(it.parentFile.absolutePath + File.separator + newName)
                    if(result.split("\n").length == 3){
                        newFile.delete()
                       return
                    }
                    newFile.withWriter("UTF-8") {
                        it.write(result)
                    }
                }
            }
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


    static boolean isRepeatFileName(String name){
        return name.startsWith("attrs") ||
                name.startsWith("colors") ||
                name.startsWith("dimens") ||
                name.startsWith("strings") ||
                name.startsWith("styles") ||
                name.startsWith("values")
    }
}