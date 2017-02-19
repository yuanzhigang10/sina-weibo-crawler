package ngn.yzg.swc.demo;

import java.io.File;

import ngn.yzg.swc.entity.UserAll;
import ngn.yzg.swc.util.JsonUtils;
import ngn.yzg.swc.util.Utils;

/**
 * JsonUtils使用demo
 * 
 * <p>
 * 演示了如何根据json字符串在内存中重建对象
 * @author PeterYuan
 *
 */
public class DemoJson {

	public static void main(String[] args) {
		File file = new File("163556381.json");
		String jsonStr = Utils.readToString(file);

		// 根据json字符串和UserAll类，在内存中重建UserAll对象
		UserAll userAll = JsonUtils.fromJson(jsonStr, UserAll.class);
		
		System.out.println("userInfo:\t" + JsonUtils.toJson(userAll.userInfo).substring(0, 100));
		System.out.println("weiboList:\t" + JsonUtils.toJson(userAll.weiboList).substring(0, 100));
		System.out.println("fansList:\t" + JsonUtils.toJson(userAll.fansList).substring(0, 100));
		System.out.println("followList:\t" + JsonUtils.toJson(userAll.followList).substring(0, 100));

		System.out.println("done!");


	}

}
