# transform-aar
This is a gradle plugin that helps to tranform aar from android library

1. 在project的build.gradle 中加入classpath

buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:2.2.3'
        classpath 'cn.mrobot.tools:transform-aar-plugin:+'
    }
}

2. 在module的build.gradle 中 添加

apply plugin: 'cn.mrobot.tools.transform-aar' 

