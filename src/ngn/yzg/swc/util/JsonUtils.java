package ngn.yzg.swc.util;

import java.lang.reflect.Type;

import com.google.gson.Gson;


/**
 * <tt>Java</tt>对象和<tt>Json</tt>字符串相互转化工具类。
 * 
 * @author yzg
 *
 */
public final class JsonUtils {
	
	private JsonUtils() {}
	
	
	/**
	 * 对象转换成<tt>Json</tt>字符串。
	 * @param obj 待转换的对象。
	 * @return <tt>Json</tt>字符串。
	 */
	public static String toJson(Object obj) {
		Gson gson = new Gson();
		return gson.toJson(obj);
	}
	
	
	/**
	 * <tt>Json</tt>字符串转成对象。
	 * @param str <tt>Json</tt>字符串。
	 * @param type 对象类型。
	 * @return 转换后的对象。
	 */
	public static <T> T fromJson(String str, Class<T> type) {
		Gson gson = new Gson();
		return gson.fromJson(str, type);
	}
	
	
	/**
	 * <tt>Json</tt>字符串转成对象，应用于<tt>container</tt>等嵌套类型。
	 * @param str <tt>Json</tt>字符串。
	 * @param type 对象类型。
	 * @return 转换后的对象。
	 */
	public static <T> T fromJson(String str, Type type) {  
        Gson gson = new Gson();  
        return gson.fromJson(str, type);  
    }
	
	
}
