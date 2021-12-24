package com.quicksdk.tools.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RFileHandlerTool {
    
    static String RPackName = "";
    static String activityAdapter = "";

    public void execute(String packName,String activityAdapterPath, String javaPath){
        RPackName = packName;
        activityAdapter = activityAdapterPath  + ".ActivityAdapter";
        String handledJavaContent = createQuickRFile(javaPath);
        try {
            FileUtils.writeStringToFile(new File(javaPath), handledJavaContent, "utf-8", false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String createQuickRFile(String fileName) {
        StringBuilder sb = new StringBuilder();

        HashMap<String, String> saveAndroidResName = new HashMap<String, String>();
        ArrayList<String> list = new ArrayList<String>();

        try {
            File file = FileUtils.getFile(fileName);
            String result = FileUtils.readFileToString(file, "UTF-8");
            String data = result.replaceAll("/\\*{1,2}[\\s\\S]*?\\*/", "").replaceAll("^\\s*\\n", "");
            FileUtils.writeStringToFile(file, data, "UTF-8");
            String line = "";
            String curClass = "";// 当前的类
            HashMap<String, String> map = new HashMap<String, String>();
            String curStyleable = "";
            boolean isAtStyleable = false;
            String hasAndroidResStyleable = "";
            int currentIndex = 0;

            LineIterator iter = FileUtils.lineIterator(file, "UTF-8");
            while (iter.hasNext()) {
                line = iter.next();
                // 添加包名和引入的类
                if (line.indexOf("package") == 0) {
                    if (RPackName.equals("pack")) {
                        line = "package R;\r\n" + "import " + activityAdapter + ";";
                    } else {
                        line = "package " + RPackName + ";\r\n" + "import " + activityAdapter + ";";
                    }
                }

                // 获取到当前所在的类
                if (line.indexOf("static final class") >= 0) {
                    int startIndex = line.indexOf("class") + 5;
                    int endIndex = line.indexOf(" {");
                    if(endIndex < startIndex){
                        endIndex = line.length();
                    }
                    curClass = line.substring(startIndex, endIndex).trim();
                }

                // 替换一般文件
                if (line.indexOf("static final int ") >= 0 && line.indexOf("0x") >= 0) {
                    int startIndex = line.indexOf("int") + 3;
                    int endIndex = line.indexOf("=");
                    String name = line.substring(startIndex, endIndex).trim();
                    String id = line.substring(line.indexOf("0x"), line.lastIndexOf(";"));

                    String beforebody = line.substring(0, endIndex + 1);
                    String afterbody = "ActivityAdapter.getResId(\"" + name + "\", \"" + curClass + "\")";
                    line = beforebody + afterbody + ";";
                    map.put(id, afterbody);
                }

                if ("styleable".equals(curClass) && line.indexOf("static final int[]") >= 0) {
                    currentIndex = 0;
                    int start = line.indexOf("int[]") + 5;
                    int end = line.indexOf("=");
                    curStyleable = line.substring(start, end).trim();
                    if (line.indexOf("{") >= 0 && line.indexOf("}") >= 0) {
                        // 所有的ids在一行上
                        if (line.indexOf("0x") >= 0) {
                            String ids = line.substring(line.indexOf("0x"), line.indexOf("}"));
                            String[] arr = ids.split(",");
                            for (int i = 0; i < arr.length; i++) {
                                String id = arr[i].trim();
                                if (!id.contains("0x")) {
                                    continue;
                                }
                                if (map.get(id) != null && !"".equals(map.get(id))) {
                                    line = line.replace(id, map.get(id));
                                } else {
                                    line = line.replace(id, "[" + curStyleable + "_" + currentIndex + "]");
                                    hasAndroidResStyleable = curStyleable;
                                    list.add("[" + curStyleable + "_" + currentIndex + "]");
                                    currentIndex++;
                                }
                            }
                        }
                        currentIndex = 0;
                    }

                    if (line.indexOf("{") >= 0 && line.indexOf("0x") < 0) {
                        isAtStyleable = true;
                        sb.append(line + "\r\n");
                        continue;
                    }

                    if (line.indexOf("{") >= 0 && line.indexOf("0x") >= 0) {
                        String ids = line.substring(line.indexOf("0x"), line.lastIndexOf(","));
                        String[] arr = ids.split(",");
                        for (int i = 0; i < arr.length; i++) {
                            String id = arr[i].trim();
                            if (!id.contains("0x")) {
                                continue;
                            }
                            if (map.get(id) != null && !"".equals(map.get(id))) {
                                line = line.replace(id, map.get(id));
                            } else {
                                line = line.replace(id, "[" + curStyleable + "_" + currentIndex + "]");
                                hasAndroidResStyleable = curStyleable;
                                list.add("[" + curStyleable + "_" + currentIndex + "]");
                            }
                            currentIndex++;
                        }
                        isAtStyleable = true;
                        sb.append(line + "\r\n");
                        continue;
                    }
                }

                if (isAtStyleable && line.indexOf("static final int[]") < 0 && line.indexOf("0x") >= 0) {

                    String str = "";
                    if (line.indexOf("};") >= 0) {
                        str = line.substring(0, line.indexOf("}"));
                        isAtStyleable = false;
                    } else {
                        str = line;
                    }
                    String[] arr = str.split(",");
                    for (int i = 0; i < arr.length; i++) {
                        String id = arr[i].trim();
                        if (!id.contains("0x")) {
                            continue;
                        }
                        if (map.get(id) != null && !"".equals(map.get(id))) {
                            line = line.replace(id, map.get(id));
                        } else {
                            line = line.replace(id, "[" + curStyleable + "_" + currentIndex + "]");
                            hasAndroidResStyleable = curStyleable;
                            list.add("[" + curStyleable + "_" + currentIndex + "]");
                        }
                        currentIndex++;
                    }
                }

                if (!hasAndroidResStyleable.equals("") && line.contains(hasAndroidResStyleable + "_android")) {
                    int start = line.indexOf("=") + 1;
                    int end = line.indexOf(";");
                    String id = line.substring(start, end).trim();
                    if (list.contains("[" + curStyleable + "_" + id + "]")) {
                        int begin = line.indexOf("android_") + 8;
                        int finish = line.indexOf("=") - 1;
                        String name = line.substring(begin, finish).trim();
                        saveAndroidResName.put("[" + curStyleable + "_" + id + "]", "android.R.attr." + name);
                    }
                }
                sb.append(line + "\r\n");
            }
            iter.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        String result = sb.toString();
        if (list.size() > 0) {
            for (String string : list) {
                result = result.replace(string, saveAndroidResName.get(string));
            }
        }
        return result;
    }

    public String getActivityAdapterPackName(String path) {
        List<String> list = new ArrayList<String>();
        getAllFilesPath(new File(path), list);
        for (String javaFilePath : list) {
            if (javaFilePath.contains("ActivityAdapter.java")) {
                String channelName = new File(javaFilePath).getParentFile().getName();
                return "com.quicksdk.apiadapter." + channelName + ".ActivityAdapter";
            }
        }
        return "";
    }

    private void getAllFilesPath(File file, List<String> list) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files)
                getAllFilesPath(f, list);
        } else {
            list.add(file.getAbsolutePath());
        }
    }

    /**
     * 找到再temp/tempR文件夹下生成的R.java的文件路径
     *
     * @param file
     * @param file
     * @return
     */
    public String getRFilePath(File file) {
        File[] files = file.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                return getRFilePath(files[i]);
            }
            if (files[i].getName().equals("R.java")) {
                return files[i].getAbsolutePath();
            }
        }
        return "";
    }
}
