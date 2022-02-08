package com.quicksdk.tools.hook

import com.android.build.gradle.api.ApplicationVariant
import com.quicksdk.tools.utils.DataMan
import com.quicksdk.tools.utils.Logger
import com.quicksdk.tools.utils.PathUtils
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileTree

/**
 * aapt dump badging
 *
 * hook package task
 * 从out.jar中提取可能在jar里面的assets文件
 * 在此task下顺便提取so文件
 * 在此task下顺便提取 manifest.xml文件
 */
class PackageHook extends HookTask{

    private static String hookTaskName = 'packageVariant'
    private static ApplicationVariant mVariant

    PackageHook(ApplicationVariant variant, Project project) {
        super(hookTaskName, project)
        taskName = hookTaskName.replace("Variant",  variant.name.capitalize())
        mVariant = variant
    }

    @Override
    void doFirst(Task task) {
        Logger.i("hook ${task.name} 提取asstes、manifest.xml、libs")
        task.inputs.files.each {
            if(it.absolutePath.contains("out.jar")){
                def zipFile = mProject.file(it.absolutePath)
                FileTree jarTree = mProject.zipTree(zipFile)
                jarTree.files.each{
                    if(it.absolutePath.contains("\\assets\\")){
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
                        mProject.copy{
                            from that
                            into PathUtils.getInstance().getTempAssetsDir() + middleDir
                        }
                    }
                }
            }
            //在此task下顺便提取so文件
            if(it.absolutePath.contains("stripped_native_libs\\debug\\out")){
                def that = new File(it.absolutePath + "\\lib")
                mProject.copy{
                    from that
                    into PathUtils.getInstance().getTempLibDir()
                }
            }
            //在此task下顺便提取manifest.xml文件
            if(it.absolutePath.contains("packaged_manifests\\debug")){
                def that = new File(it.absolutePath + "\\AndroidManifest.xml")
                mProject.copy{
                    from that
                    into PathUtils.getInstance().getManifestFileDir()
                }
            }
        }
    }

}