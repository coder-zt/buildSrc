package com.quicksdk.tools.hook

import com.android.build.gradle.api.ApplicationVariant
import com.quicksdk.tools.extension.ChannelToolExt
import com.quicksdk.tools.utils.DataMan
import com.quicksdk.tools.utils.Logger
import com.quicksdk.tools.utils.PathUtils
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * hook mergeResources task
 * 提取工程或者aar文件的资源文件（res）文件
 * 重命名values下的文件，充值values.xml被覆盖
 */
class MergeResourcesHook extends HookTask{

    private static String hookTaskName = 'mergeVariantResources'

    MergeResourcesHook(ApplicationVariant variant, Project project) {
        super(hookTaskName, project)
        taskName = hookTaskName.replace("Variant",  variant.name.capitalize())
    }

    @Override
    void doFirst(Task task) {
        Logger.i("hook ${task.name} 提取res目录下的文件")
        task.inputs.files.each {
            File temp = it
            Logger.i(it.absolutePath)
            def libName = it.parentFile.name.replace("jetified-", "")
            if (DataMan.instance.projectArrFiles.contains(libName) || libName == "main") {
                mProject.copy {
                    from temp.absolutePath
                    into PathUtils.getInstance().getTempResDir()
                }
            }else{
                Logger.i("没有处理：" + libName)
            }
            //res/values下values.xml可能会被同名的文件覆盖
            renameResValuesFiles(libName)
        }
    }

    /**
     * res/values下values.xml可能会被同名的文件覆盖
     */
    static def renameResValuesFiles(String parentName){
        new File(PathUtils.getInstance().getTempResDir()).listFiles().each {
            if(it.name.startsWith("values")){
                def that = it
                it.listFiles().each {
                    if(it.name == that.name + ".xml"){
                        def newName = it.name.replace(".xml","(" + parentName + ").xml")
                        it.renameTo(it.parentFile.absolutePath + File.separator + newName)
                    }
                }
            }
        }
    }

}