package com.quicksdk.tools.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;


public class HttpRequest {

	public static String getChannelInfos() {
		if(true){
			return  "[{\"channelCode\":\"1778\",\"name\":\"Game9917_1778\",\"resourceVersionNo\":\"111\"}," +
					"{\"channelCode\":\"2093\",\"name\":\"XiaFengYouXi_2093\",\"resourceVersionNo\":\"66\"}]";
		}
		StringBuffer sb = new StringBuffer();
		try {
			URL url = new URL("http://bt.quickapi.net:82/base/getChannelBaseData");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.connect();
			InputStream inputStream = connection.getInputStream();
			Reader reader = new InputStreamReader(inputStream, "UTF-8");
			BufferedReader bufferedReader = new BufferedReader(reader);
			String str = null;
			while ((str = bufferedReader.readLine()) != null) {
				sb.append(str);
			}
			reader.close();
			connection.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sb.toString();
	}
}
