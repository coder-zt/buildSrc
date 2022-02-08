package com.quicksdk.tools.hook

import com.android.build.gradle.api.ApplicationVariant
import com.quicksdk.tools.extension.JarExcludeExt
import com.quicksdk.tools.utils.DataMan
import com.quicksdk.tools.utils.Logger
import com.quicksdk.tools.utils.PathUtils
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * hook dexBuilder task
 * 提取接入渠道的工程代码和R.jar
 */
class DexBuilderHook extends HookTask{

    private static String hookTaskName = 'dexBuilderVariant'


    DexBuilderHook(ApplicationVariant variant, Project project) {
        super(hookTaskName, project)
        taskName = hookTaskName.replace("Variant",  variant.name.capitalize())
    }

    @Override
    void doFirst(Task task) {
        Logger.i("hook ${task.name} 提取接入渠道的工程代码和R.jar")
        task.inputs.files.each {
            def that = it
            //将接入工程中的*.class文件复制的临时目录
            if(it.absolutePath.endsWith('classes') ||!it.absolutePath.endsWith('jar')){
                mProject.copy {
                    from that.absolutePath
                    into PathUtils.getInstance().getTempClassesDir()
                }
            }
            //删除多余的文件
            def classesTempDir = new File( PathUtils.getInstance().getTempClassesDir())
            deleteExtraFile(classesTempDir, false)
            //将R.jar文件复制到jar和res-jar目录下
            if(it.name == "R.jar") {
                mProject.copy {
                    from that.absolutePath
                    into PathUtils.getInstance().getTempJarDir()
                }
                mProject.copy {
                    from that.absolutePath
                    into PathUtils.getInstance().getTempResJarDir()
                }
            }
        }
        //            projectClass2Smali()
        //            Map<String, String> allRFilePacks = extractResJarClass()
        //            classes2Java(allRFilePacks)
    }

    //com\quicksdk\apiadapter
    private void deleteExtraFile(File classesTempDir, boolean delete) {
        classesTempDir.listFiles().each {
            if(it.directory && it.listFiles().length > 0){
                deleteExtraFile(it, false)
            }
            if(it.directory){
                if( !it.absolutePath.endsWith("com") &&
                        !it.absolutePath.endsWith("com\\quicksdk") &&
                        !it.absolutePath.endsWith("com\\quicksdk\\apiadapter") &&
                        !it.absolutePath.contains("com\\quicksdk\\apiadapter")){
                    mProject.delete(it)
                }
            }else {
                if( !it.parentFile.absolutePath.endsWith("com") &&
                        !it.parentFile.absolutePath.endsWith("com\\quicksdk") &&
                        !it.parentFile.absolutePath.endsWith("com\\quicksdk\\apiadapter") &&
                        !it.parentFile.absolutePath.contains("com\\quicksdk\\apiadapter")){
                    mProject.delete(it)
                }
            }

        }
    }

}