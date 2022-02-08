package com.quicksdk.tools.hook

import com.android.build.gradle.api.ApplicationVariant
import com.quicksdk.tools.utils.DataMan
import com.quicksdk.tools.utils.Logger
import com.quicksdk.tools.utils.PathUtils
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * hook mergeAssets task
 * 提取assets文件
 */
class MergeAssetsHook extends HookTask{

    private static String hookTaskName = 'mergeVariantAssets'

    MergeAssetsHook(ApplicationVariant variant, Project project) {
        super(hookTaskName, project)
        taskName = hookTaskName.replace("Variant",  variant.name.capitalize())
    }

    @Override
    void doFirst(Task task) {
        Logger.i("hook ${task.name} 提取assets文件")
        task.inputs.files.each {
            def that = it
            mProject.copy {
                from that.absolutePath
                into PathUtils.getInstance().getTempAssetsDir()
            }
        }
        //删除quicksdk.xml
        File quicksdkXmlFile = new File(PathUtils.getInstance().getTempAssetsDir() + File.separator + "quicksdk.xml")
        if(quicksdkXmlFile.exists()){
            mProject.delete(quicksdkXmlFile)
        }
    }

}