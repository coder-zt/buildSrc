package com.quicksdk.tools.extension

class CreateRFileExt {

    //需要生成修改后的R文件的包名
    List<String> genPackList = new ArrayList<>()

    void addPack(String pack){
        genPackList.add(pack)
    }

}