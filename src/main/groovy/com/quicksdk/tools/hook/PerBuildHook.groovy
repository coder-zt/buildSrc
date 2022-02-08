package com.quicksdk.tools.hook

import com.android.build.gradle.api.ApplicationVariant
import com.quicksdk.tools.extension.ChannelToolExt
import com.quicksdk.tools.utils.DataMan
import com.quicksdk.tools.utils.Logger
import com.quicksdk.tools.utils.PathUtils
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * hook perBuild task
 * 初始化插件中的相关参数
 * 清除上次提取文件的目录
 * 创建需要的文件夹目录
 * 获取该工程下的arr文件名称
 */
class PerBuildHook extends HookTask{

    private static final String hookTaskName = "preBuild"

    PerBuildHook(ApplicationVariant variant, Project project) {
        super(hookTaskName, project)
    }

    @Override
    void doFirst(Task task) {
        Logger.i("hook preBuild 初始化提取工具")
        //清理提取资源文件的目录
        cleanTempResFile()
        //创建需要的目录
        createTemResFile()
        //获取工程项目lib下的aar文件
        getProjectArrFile()
    }

    /**
     * 清理提取资源文件的目录
     */
    private void  cleanTempResFile(){
        def tempResList = new ArrayList<String>()
        tempResList.add(PathUtils.instance.getTempResDir())
        tempResList.add(PathUtils.instance.getTempJarDir())
        tempResList.add(PathUtils.instance.getSmaliResultDir())
        tempResList.add(PathUtils.instance.getTempClassesDir())
        tempResList.add(PathUtils.instance.getTempManifestFile())
        tempResList.add(PathUtils.instance.getTempDexDir())
        tempResList.add(PathUtils.instance.getTempAssetsDir())
        tempResList.add(PathUtils.instance.getTempResJarDir())
        tempResList.add(PathUtils.instance.getTempLibDir())
        tempResList.add(PathUtils.instance.getTempRDir())
        for(res in tempResList){
            File resFile = new File(res)
            if(resFile.exists()){
                mProject.delete(resFile)
            }
        }
    }


    /**
     * 创建需要的目录
     */
    private static void createTemResFile(){
        List<String> createResList = new ArrayList<>()
        createResList.add(PathUtils.getInstance().getSmaliResultDir())
        createResList.add(PathUtils.getInstance().getTempDexDir())
        for(res in createResList){
            File resFile = new File(res)
            if(!resFile.exists()){
                resFile.mkdirs()
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
                    def aarName = it.name.replace('.aar','')
                    DataMan.instance.addProjectAar(aarName)
                }
            }
        }else{
            assert "指定的libs文件夹无效"
        }
    }
}