package com.quicksdk.tools.hook

import com.android.build.gradle.api.ApplicationVariant
import org.gradle.api.Project
import org.gradle.api.Task

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
    }

    abstract void doFirst(Task task)
}