package com.quicksdk.tools.hook

import com.android.build.gradle.api.ApplicationVariant
import com.android.ddmlib.Log
import com.quicksdk.tools.extension.JarExcludeExt
import com.quicksdk.tools.utils.DataMan
import com.quicksdk.tools.utils.Logger
import com.quicksdk.tools.utils.PathUtils
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * hook mergeResources task
 * 提取渠道相关的jar文件
 */
class MergeJavaResourceHook extends HookTask{

    private static String hookTaskName = 'mergeVariantJavaResource'


    MergeJavaResourceHook(ApplicationVariant variant, Project project) {
        super(hookTaskName, project)
        taskName = hookTaskName.replace("Variant",  variant.name.capitalize())
    }

    @Override
    void doFirst(Task task) {
        Logger.i("hook ${task.name} 提取项目使用的jar")
        List<String> dependencies = getProjectDependencies(mProject)
        dependencies.forEach{
            Logger.i("dependencies ===> $it")
        }
        task.inputs.files.each {
            File temp = it
            //排除quick的相关的库，不需要这些库的smali
            def jarExt = mProject.extensions.getByType(JarExcludeExt.class)
            def originLibName = it.name;
            if(originLibName.contains("jetified-")){
                originLibName = it.name.replace("jetified-", "")
            }
            if (jarExt != null && jarExt.excludeJar != null && !jarExt.excludeJar.contains(originLibName)) {
                Logger.i("project jar ===> " + temp.absolutePath)
                mProject.copy {
                    from temp.absolutePath
                    into PathUtils.getInstance().getTempJarDir()
                }
                //去除"jetified-"
                def jarCopiedPath = PathUtils.getInstance().getTempJarDir() + File.separator + temp.name
                if(jarCopiedPath.contains("jetified-")){
                    def reNamePath = jarCopiedPath.replace("jetified-", "")
                    def reNameLib = new File(reNamePath)
                    def copiedFile = new File(jarCopiedPath)
                    copiedFile.renameTo(reNameLib)
                }
                if (it.name == 'classes.jar') {
                    def libName = it.parentFile.parentFile.name + ".jar"
                    def originalName = libName.replace("jetified-", "")
                    def reNameLib = new File(PathUtils.getInstance().getTempJarDir() + "\\" + originalName)
                    def copiedFile = new File(PathUtils.getInstance().getTempJarDir() + "\\classes.jar")
                    copiedFile.renameTo(reNameLib.absolutePath)
                    DataMan.instance.addHandleJar(reNameLib.absolutePath)
                } else {
                    DataMan.instance.addHandleJar(PathUtils.getInstance().getTempJarDir() + "\\" + it.name)
                }
            } else {
                Logger.i( '排除quick中的库：' + it.name)
            }
        }
    }

}