package com.quicksdk.tools.hook

import com.android.build.gradle.api.ApplicationVariant
import com.quicksdk.tools.utils.DataMan
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency

abstract class HookTask {

    String taskName
    Project mProject

    HookTask(String taskName, Project project){
        this.taskName = taskName
        this.mProject = project
    }



    void hook(){
        Task targetTask0 = mProject.tasks.findByName(taskName)
        targetTask0.doFirst { task ->
            doFirst(task)
        }
        targetTask0.doLast{ task ->
            doLast(task)
        }
    }

    void doFirst(Task task){

    }

     void doLast(Task task){

    }

    static List<String> getProjectDependencies(Project project) {
        //implementation
        List<String> implementations = new ArrayList<>()
        DependencySet compilesDependencies = project.configurations.implementation.dependencies
        Set<DefaultExternalModuleDependency> allLibs = compilesDependencies.withType(DefaultExternalModuleDependency.class)
        allLibs.each {
            implementations.add("${it.name}-${it.version}")
        }
        DataMan.instance.projectArrFiles.each {
            implementations.add(it)
        }
        return implementations
    }

}