package com.quicksdk.tools.utils;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;


public class ChannelInfo {
	
	public final static String TW_CHANNEL = "96";

	public final static String NO_CHANNEL_TYPE = "-1";

	private static String mChannelType = "-1";

	private static String mResourceName = "";

	private static String mResourceVersion = "-1";

	private static boolean isTwquick = false;

	/**
	 * 初始化工作,设置了项目地址后调用
	 */
	public static void init(String channelInfos,String channelType) {
		mChannelType = channelType;
		mResourceName = "channel_" + channelType;
		parseChannelInfos(channelInfos);
		if(TW_CHANNEL.equals(channelType)){
			isTwquick = true;
		}else{
			isTwquick = false;
		}
	}

	/**
	 * 获取渠道id
	 * @return
	 */
	public static String getChannelType() {
		return mChannelType;
	}

	/**
	 * 获取资源名称
	 * @return
	 */
	public static String getResourceName() {
		return mResourceName;
	}

	/**
	 * 获取资源版本号
	 * @return
	 */
	public static String getResourceVersion() {
		return mResourceVersion;
	}

	/**
	 * 是否为twquick渠道
	 * @return
	 */
	public static boolean isTwquick() {
		return isTwquick;
	}

	private static void parseChannelInfos(String channelInfos) {
		try {
			String result = channelInfos.substring(1, channelInfos.length());
			JSONArray jsonArray = JSONArray.fromObject(channelInfos);
			int length = jsonArray.size();
			for (int i = 0; i < length; i++) {
				JSONObject jsonObject = (JSONObject) jsonArray.get(i);
				String channelCode = jsonObject.getString("channelCode");
				String name = jsonObject.getString("name");
				String resourceVersionNo = jsonObject.getString("resourceVersionNo").equals("") ? "0" : jsonObject.getString("resourceVersionNo");
				if(channelCode.equals(mChannelType)){
					mResourceName = name;
					mResourceVersion = resourceVersionNo;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
