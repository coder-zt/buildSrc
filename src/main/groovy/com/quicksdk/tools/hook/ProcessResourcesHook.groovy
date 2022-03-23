package com.quicksdk.tools.hook

import com.android.build.gradle.api.ApplicationVariant
import com.quicksdk.tools.utils.Logger
import com.quicksdk.tools.utils.PathUtils
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * hook dexBuilder task
 * 提取接入渠道的工程代码和R.jar
 */
class ProcessResourcesHook extends HookTask{

    private static String hookTaskName = 'processVariantResources'


    ProcessResourcesHook(ApplicationVariant variant, Project project) {
        super(hookTaskName, project)
        taskName = hookTaskName.replace("Variant",  variant.name.capitalize())
    }

    @Override
    void doLast(Task task) {
        Logger.i("hook ${task.name} 提取R.jar")
        task.outputs.files.each {


//            //将R.jar文件复制到jar和res-jar目录下
            if(it.name == "R.jar") {
                Logger.i("ProcessResourcesHook: yes " + it.absolutePath)
                def that = it
                mProject.copy {
                    from that.absolutePath
                    into PathUtils.getInstance().getTempJarDir()
                }
                mProject.copy {
                    from that.absolutePath
                    into PathUtils.getInstance().getTempResJarDir()
                }
            }else {
                Logger.i("ProcessResourcesHook: no " + it.absolutePath)
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