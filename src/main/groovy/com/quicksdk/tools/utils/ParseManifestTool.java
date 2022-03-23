package com.quicksdk.tools.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;



public class ParseManifestTool {

	private final static String testActivity = "com.quicksdk.test";

	public static String channelVersion = "";

	public static String resVersion = "";

	public static int OAID = 0;

	public static boolean placeholderRight = true;

	private String channelResourceName = "";
	private String channelProjectPackName = "";
	private String tempHandlePath = "";

	public void execute(String tempPath,String handlePath,String resourceName,String packName) {
		channelResourceName = resourceName;
		channelProjectPackName = packName;
		tempHandlePath = handlePath;
		placeholderRight = true;
		File file = new File(tempPath + "\\AndroidManifest.xml");
		System.out.println(file.getAbsoluteFile());
		String str = replaceConfig(file);
		SAXReader saxReader = new SAXReader();
		Document document;
		try {
			document = saxReader.read(new ByteArrayInputStream(str.getBytes("UTF-8")));
			Element root = document.getRootElement();
			writePermissionToFile(root);
			writeActiveToFile(root);
			writeRootToFile(root);
//			writeDescriptionToFile();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String replaceConfig(File file) {
		InputStream in = null;
		try {
			OAID=0;
			in = new FileInputStream(file.getAbsolutePath());
			InputStreamReader inputReader = new InputStreamReader(in);
			BufferedReader bufReader = new BufferedReader(inputReader);
			LineNumberReader reader = new LineNumberReader(bufReader);
			StringBuilder sb = new StringBuilder();
			String result = "";
			while ((result = reader.readLine()) != null) {
				if (!"".equals(result)) {
					if (result.contains("<!-- {{#quicksdk")) {
						String quickData = result.substring(result.indexOf("<!-- {{#") + "<!-- {{#".length(),
								result.indexOf("#}} -->"));
						if (quickData.indexOf(":") > 0) {
							String quickKey = quickData.substring(0, quickData.indexOf(":"));
							String quickValue = quickData.substring(quickData.indexOf(":") + 1, quickData.length());
							String replaceKey = "{{$" + quickKey.substring("quicksdk_".length(), quickKey.length())
									+ "}}";
							String tmp = result.replace(quickValue, replaceKey);
							String removeContent = tmp.substring(tmp.indexOf("<!-- {{#"), tmp.indexOf("#}} -->")
									+ "#}} -->".length());
							result = tmp.replace(removeContent, "");

						}
//						/Log.i("占位符：" + result.trim());
						if ("".equals(result) || !result.contains("$")) {
							placeholderRight = false;
						}
					} else if (result.contains("<!-- {{#")) {
						String quickData = result.substring(result.indexOf("<!-- {{#") + "<!-- {{#".length(),
								result.indexOf("#}} -->"));
						if (quickData.indexOf(":") > 0) {
							String quickKey = quickData.substring(0, quickData.indexOf(":"));
							String quickValue = quickData.substring(quickData.indexOf(":") + 1, quickData.length());
							String replaceKey = "{{$" + quickKey + "}}";
							String tmp = result.replace(quickValue, replaceKey);
							Logger.i(tmp);
							String removeContent = tmp.substring(tmp.indexOf("<!-- {{#"), tmp.indexOf("#}} -->")
									+ "#}} -->".length());
							result = tmp.replace(removeContent, "");
						}
//						Log.i("占位符：" + result.trim());
						if ("".equals(result) || !result.contains("$")) {
							placeholderRight = false;
						}
					} else if (result.contains("<!--Q:OAID")) {
//						Log.i("SDK包含OAID sdk");
						OAID = 1;
					}
					sb.append(result + "\r\n");
				}
			}
			reader.close();
			bufReader.close();
			return sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public void writePermissionToFile(Element root) throws IOException {
		String data = getPermission(root);
		if (!"".equals(data.trim())) {
			data = data.replaceAll(channelProjectPackName,"quicksdk_packName");
			File file = FileUtils.getFile(tempHandlePath + "\\"  + channelResourceName + "\\permission.txt");
			FileUtils.writeStringToFile(file, data, "UTF-8");
		}
	}

	public void writeActiveToFile(Element root) throws IOException {
		String data = exchangeAttr(getActive(root));
		//替换一下出现过的包名
		data = data.replaceAll(channelProjectPackName,"quicksdk_packName");
		if (!"".equals(data.trim())) {
			File file = FileUtils.getFile(tempHandlePath + "\\"  + channelResourceName + "\\active.txt");
			FileUtils.writeStringToFile(file, data, "UTF-8");
		}
	}

	public void writeRootToFile(Element root) throws IOException {
		String data = exchangeAttr(getRoot(root));
		if (!"".equals(data.trim())) {
			data = data.replaceAll(channelProjectPackName,"quicksdk_packName");
			File file = FileUtils.getFile(tempHandlePath + "\\" + channelResourceName + "\\root.txt");
			FileUtils.writeStringToFile(file, data, "UTF-8");
		}

	}

	public void writeDescriptionToFile() throws IOException {
		String data = getDescription();
		if (!"".equals(data.trim())) {
			File file = FileUtils.getFile(tempHandlePath + "\\"  + channelResourceName + "\\description.txt");
			FileUtils.writeStringToFile(file, data, "UTF-8");
		}
	}

	private String getDescription() {
		channelVersion = "";
		resVersion = "";
//		String sdkAdapterPath = getSdkAdapterPath(ProjectPath.getSrc());
//		channelVersion = getChannelVersionFromCode(sdkAdapterPath);
//		int version = Integer.parseInt(ChannelInfo.getResourceVersion()) + 1;
//		resVersion = String.valueOf(version);
//		String description = Utils.addString(
//				"{\"versionNo\":\"",
//				resVersion,
//				"\",\"channelVersion\":\"",
//				channelVersion,
//				"\",\"OAID\":",OAID+"",
//				",\"aboveVersion\":200",Config.isDivided?",\"needAddBaseLib\":\"1\"}":"}");
		
		return "";
	}

	public String getSdkAdapterPath(String path) {
		List<String> list = new ArrayList<String>();
		getAllFilesPath(new File(path), list);
		for (String javaFilePath : list) {
			if (javaFilePath.contains("SdkAdapter.java")) {
				return javaFilePath;
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

	public String getChannelVersionFromCode(String sdkAdapterPath) {
		if (!"".equals(sdkAdapterPath)) {
			File file = new File(sdkAdapterPath);
			LineIterator iter;
			try {
				iter = FileUtils.lineIterator(file, "UTF-8");
				String line = "";
				while (iter.hasNext()) {
					line = iter.next();
					if (line.contains("getChannelSdkVersion()")) {
						String next = iter.next();
						if (next.contains("return")) {
							String version = next.substring(next.indexOf("\"") + 1, next.lastIndexOf("\""));
							return version;
						}
					}
				}
				iter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return "";
	}

	@SuppressWarnings("unchecked")
	private String getRoot(Element root) {
		StringBuilder sb = new StringBuilder();

		List<Element> childElements = root.elements("uses-feature");
		for (Element child : childElements) {
			sb.append(child.asXML());
		}

		childElements = root.elements("permission");
		for (Element child : childElements) {
			sb.append(child.asXML());
		}

		childElements = root.elements("meta-data");
		for (Element child : childElements) {
			sb.append(child.asXML());
		}

		Element element = root.element("supports-screens");
		if (element != null) {
			sb.append(element.asXML());
		}

		String str = sb.toString();
		if (str.indexOf("xmlns") >= 0) {
			str = str.replace(" xmlns:android=\"http://schemas.android.com/apk/res/android\"", "");
		}

		return str;
	}

	@SuppressWarnings("unchecked")
	public String getPermission(Element root) {
		List<Element> childElements = root.elements("uses-permission");
		StringBuilder sb = new StringBuilder();
		for (Element child : childElements) {
			// 未知属性名情况下
			List<Attribute> attributeList = child.attributes();
			for (Attribute attr : attributeList) {
				sb.append(attr.getValue().trim()).append("\r\n");
			}
		}
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	public String getActive(Element root) {
		Element application = root.element("application");
		StringBuilder sb = new StringBuilder();
		List<Element> childElements = application.elements();
		for (Element child : childElements) {
			boolean need = true;
			List<Attribute> attributeList = child.attributes();
			for (Attribute attr : attributeList) {
				if ("name".equals(attr.getName()) && attr.getValue().contains(testActivity)) {
					need = false;
				}
			}
			if (need) {
				String str = child.asXML();
				if (str.indexOf("xmlns") >= 0) {
					str = str.replace(" xmlns:android=\"http://schemas.android.com/apk/res/android\"", "");
				}
				sb.append(str).append("\r\n");
			}
		}
		String result = sb.toString().replace("android:screenOrientation=\"locked\"",
				"android:screenOrientation=\"{{$screenOrientation}}\"");
		return result;
	}

	public String exchangeAttr(String str) {
		String result = "";
		result = str.replace("android:screenOrientation=\"locked\"",
				"android:screenOrientation=\"{{$screenOrientation}}\"");
		return result;
	}
}
