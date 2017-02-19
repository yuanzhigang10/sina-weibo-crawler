package ngn.yzg.swc.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ngn.yzg.swc.entity.EntityType;

/**
 * 工具类。
 * 
 * <p>
 * 提供辅助于本项目的一些列工具函数。
 * 
 * @author PeterYuan
 * @since jdk1.8
 *
 */
public final class Utils {

	private Utils() {}

	private static DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance();



	/**
	 * 一般字符串处理（过滤掉数据库不安全字符）
	 * @param text
	 * @return
	 */
	public static String generalProcess(String text) {
		if (text == null)
			return "";
		return SqlUtils.filterStr(text).replaceAll("\n", "").replaceAll("\t", "");
	}

	
	/**
	 * 将list转为str存储，分隔符sep
	 * @param lists
	 * @return
	 */
	public static String list2Str(List<? extends Object> lists, String sep) {
		if (lists == null)
			return "";
		StringBuffer result = new StringBuffer();
		for (int i=0; i<lists.size(); i++) {
			result.append(lists.get(i).toString());
			if (i < lists.size()-1) {
				result.append(sep);
			}
		}
		return result.toString();
	}

	/**
	 * 获取JVM内存使用信息
	 * @return
	 */
	public static String getMemoryInfo() {
		double memory = Runtime.getRuntime().freeMemory()/1024.0/1024;
		double rate = (double)Runtime.getRuntime().freeMemory() / Runtime.getRuntime().totalMemory();
		return "[freeRate=" + df.format(100*rate) + "%, freeMem=" + df.format(memory) + "M]";
	}

	/** 
	 * 将emoji表情替换成*
	 * <p>
	 * 由于MySQL的utf8格式仅支持3字节内的utf8编码，因此需要过滤掉类似于Emoji等4字节的字符，以保证数据添加成功
	 * @param source 原文本字符串
	 * @return 过滤后的字符串 
	 */  
	public static String filterEmoji(String text) {
		if (text == null)
			return "";
		if (StringUtils.isNotBlank(text)) {
			return text.replaceAll("[\\ud800\\udc00-\\udbff\\udfff\\ud800-\\udfff]", "*");
		} else {
			return text;
		}
	}


	/**
	 * 提取微博文本中的实际微博内容
	 * @param text 原始微博文本
	 * @return 实际微博文本
	 */
	public static String getTrueWeiboText(String text) {
		if (text == null)
			return "";
		text = text.replaceAll(" ", "");
		text = text.substring(text.indexOf(":") + 1);
		if (text.contains("举报 赞")) {
			text = text.substring(0, text.indexOf("举报 赞"));
		}
		int index1 = text.indexOf("原图");
		int index2 = text.indexOf("赞[");
		if ((((index1 == -1)||(index2 < index1))) && (index2 != -1))
			index1 = index2;
		if (index1 != -1)
			text = text.substring(0, index1);
		return text;
	}


	/**
	 * 封装<tt>jdk1.8</tt>中的{code getOrDefault}函数，将默认值设为{code Integer.MAX_VALUE}。
	 * @param configs 爬取最大页数的配置，{@link java.util.HashMap HashMap}格式。
	 * @param key 键。
	 * @return <tt>key</tt>对应的最大页数。
	 */
	public static Integer getOrDefault(HashMap<EntityType, Integer> configs, EntityType key) {
		return configs.getOrDefault(key, Integer.MAX_VALUE);
		/*if (configs.containsKey(key)) {
			return configs.get(key);
		} else {
			return Integer.MAX_VALUE;
		}*/
	}


	/**
	 * 从文件中读取<tt>userIdList</tt>。
	 * 
	 * <p>
	 * 对文件格式的要求：<br>
	 * 每行一个<tt>User</tt>信息；<br>
	 * <tt>UserId</tt>需是每行的第一个字段，字段间用<tt>Tab</tt>或<tt>Space</tt>区分。
	 * @param fileName 文件名。
	 * @return 读取获得的<tt>userIdList</tt>。
	 */
	public static List<String> getUserIdListFromFile(String fileName) {
		List<String> userIdList = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF8"));
			String line = null;
			String userId = null;
			while ((line=br.readLine()) != null) {
				String[] fields = line.trim().split("\\s");
				userId = fields[0];
				if (userId.matches("\\d+")) {
					userIdList.add(userId);
				} else {
					System.err.println("getUserIdListFromFile: invalid userId " + userId);
				}
			}
			br.close();
		} catch (UnsupportedEncodingException e) {
			System.err.println("getUserIdListFromFile: UnsupportedEncodingException");
			return null;
		} catch (FileNotFoundException e) {
			System.err.println("getUserIdListFromFile: FileNotFoundException");
			return null;
		} catch (IOException e) {
			System.err.println("getUserIdListFromFile: IOException");
			return null;
		}
		return userIdList;
	}



	/**
	 * 将搜索的关键词转为微博接受的搜索格式。
	 * @param keyword 搜索关键词。
	 * @return 微博接受的搜索格式。
	 */
	public static String parseKeyword(String keyword) {
		String result = "";
		try {
			result = URLEncoder.encode(keyword, "UTF8");
		} catch (UnsupportedEncodingException e) {
			System.err.println("搜索关键词编码失败！");
			e.printStackTrace();
		}
		return result;
	}


	/**
	 * 对{@link java.io.PrintStream PrintStream}封装，处理异常。
	 * 
	 * <p>
	 * {@link java.io.FileOutputStream FileOutputStream}，<tt>UTF8</tt>格式。
	 * @param fileName 文件名。
	 * @return 打开的{@link java.io.PrintStream PrintStream}对象，打开失败时返回<tt>null</tt>。
	 */
	public static PrintStream openFilePS(String fileName) {
		try {
			PrintStream ps = new PrintStream(new FileOutputStream(fileName), true, "UTF8");
			return ps;
		} catch (UnsupportedEncodingException e) {
			System.err.println("openFilePS: UnsupportedEncodingException");
			return null;
		} catch (FileNotFoundException e) {
			System.err.println("openFilePS: FileNotFoundException");
			return null;
		}
	}


	/**
	 * 对{@link java.io.BufferedReader BufferedReader}封装，处理异常。
	 * <p>
	 * {@link java.io.FileOutputStream FileOutputStream}，<tt>UTF8</tt>格式。
	 * @param fileName 文件名。
	 * @return 打开的{@link java.io.BufferedReader BufferedReader}对象，打开失败时返回<tt>null</tt>。
	 */
	public static BufferedReader openFileBR(String fileName) {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF8"));
			return br;
		} catch (UnsupportedEncodingException e) {
			System.err.println("openFileBR: UnsupportedEncodingException");
			return null;
		} catch (FileNotFoundException e) {
			System.err.println("openFileBR: FileNotFoundException");
			return null;
		}
	}


	/**
	 * 一次读取文本文件的所有内容。
	 * @param file 文件对应的{@link java.io.File File}对象。
	 * @return 文本全部内容对应的{@code String}对象。
	 */
	public static String readToString(File file) {
		Long filelength = file.length(); // 获取文件长度
		byte[] filecontent = new byte[filelength.intValue()];
		try {
			FileInputStream in = new FileInputStream(file);
			in.read(filecontent);
			in.close();
		} catch (FileNotFoundException e) {
			System.err.println("readToString: FileNotFoundException");
		} catch (IOException e) {
			System.err.println("readToString: IOException");
		}
		return new String(filecontent); //返回文件内容，默认编码
	}


}
