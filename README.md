# Quick_Resource

quickSDK 提取渠道 sdk 资源的 gradle 插件

1. 拉取 gradle 脚本到本地 Android 接入工程根目录下

```ps
git clone https://github.com/coder-zt/buildSrc.git
```

![](src\assets\buildSrc.png)

2. 配置 as 工程根目录下的 build.gradle

```gardle
apply plugin: 'com.quicksdk.tools.extract'
```

- 添加插件后同步项目，如果没有报错即为引用插件成功并且在 Gradle 目录中会生成自定义的 task

![](src\assets\gradleTask.png)

配置渠道工具插件的相关信息配置

    配置打包工具资源目录，extractChnanleResource task 直接将资源提取到资源目录中
    配置临时目录是提取的中间文件会存放在改目录中，但是每次打包都会删除这个目录下的文件

```gradle
channelToolInfo{
    //打包工具的channel目录
    channelResourceDir = "D:\\QuickToolsBate\\Data\\Channels"
    //提取打包过程中的临时文件目录
    channelTempDir = "E:\\ResTestTemp"
}
```

    该配置是去除不需要 smali 的 jar，即 quicskdsdk 相关的库，和 quick 的基础模块

```gradle
jarExcludeExt{
    //不需要转化为smali的lib
    addExcludeJar "com.quickjoy.lib.jkhttp_v2017081702_proguard.jar"
    addExcludeJar "quicksdk_2.7.0.jar"
    //非application模块
    addExcludeApplication "quickSDK_base"
    addExcludeApplication "quickSDK_base_huawei"
    addExcludeApplication "quickSDK_base_huawei_2.7.2"
}
```

    该配置内容主要配置新的工程项目，修改配置后再同步即可使用 ChannelProjectGennerator 生成新的工程项目，task 执行完成后再同步工程就添加新的渠道工程成功，前提是已经将 QuickSDK_0_model 引入进入工程

```gradle
newChannelBean{
    //渠道号
    channelType = "2107"
    //渠道名称
    channelName = "mushouyouxi"
    //渠道包名
    channelPackName = "mushaoyouxi"
}
```

3. 配置子项目的 build.gradle，配置其需要生成重新处理 R 文件的包名，如果是生成在包名下即为 pack，如果不需要重新生成 R 文件子项目则无需配置

```
genRFiles{
    addPack "com.baidu.passport.sapi2"
    addPack "pack"
}
```
