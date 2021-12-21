package com.quicksdk.tools.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Cmd {

    public static void run(String cmd){
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            new Thread(new StreamDrainer(process.getInputStream())).start();
            new Thread(new StreamDrainer(process.getErrorStream())).start();
            process.getOutputStream().close();
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


class StreamDrainer implements Runnable {

    private InputStream ins;

    public StreamDrainer(InputStream ins) {
        this.ins = ins;
    }

    public void run() {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(ins,"GBK"));
            String line = null;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}