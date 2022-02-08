package com.quicksdk.tools.utils;

import java.util.ArrayList;
import java.util.List;

public class DataMan {
    private static DataMan instance = null;

    public List<String> projectArrFiles = new ArrayList<>();
    public List<String> handleJarList = new ArrayList<>();

    private DataMan(){

    }

    public static DataMan getInstance(){
        if(instance == null){
            instance = new DataMan();
        }
        return instance;
    }

    public void addProjectAar(String aar){
        if (!projectArrFiles.contains(aar)) {
            projectArrFiles.add(aar);
        }
    }

    public void addHandleJar(String jar){
        if (!handleJarList.contains(jar)) {
            handleJarList.add(jar);
        }
    }
}
