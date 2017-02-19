package ngn.yzg.swc.demo;

import java.io.PrintStream;
import java.util.HashMap;

import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import ngn.yzg.swc.control.Console;
import ngn.yzg.swc.crawler.UserAllCrawler;
import ngn.yzg.swc.entity.CrawlerException;
import ngn.yzg.swc.entity.EntityType;
import ngn.yzg.swc.entity.UserAll;
import ngn.yzg.swc.entity.WeiboAccount;
import ngn.yzg.swc.util.DriverUtils;
import ngn.yzg.swc.util.JsonUtils;
import ngn.yzg.swc.util.Utils;

/**
 * UserAllCrawler使用demo
 * @author PeterYuan
 *
 */
public class DemoUserAll {

	public static void main(String[] args) throws CrawlerException {
		// 全局参数设置
		Console.debugMode = true; // 是否debug输出模式，不指定时默认false
		Console.waitTime = 2500L; // 单次访问后的等待时间，单位ms，不指定时默认2500ms
		Console.waitTimeIfBanned = 3600_000L; // 被封号后的等待时间，单位ms，不指定时默认3600_000ms，即1小时
		WeiboAccount account = new WeiboAccount("yuanzhigang10@126.com", "********");

		// 根据userId爬取用户所有信息
		HtmlUnitDriver[] drivers = DriverUtils.createUsableDrivers(account, 2);
		String userId = "1635563814";  // 1635563814
		// 配置各个对象爬取页数的信息，不指定默认全部爬取，0代表不爬取
		HashMap<EntityType, Integer> configs = new HashMap<>();
		configs.put(EntityType.WEIBO, 5);
		//configs.put(EntityType.COMMENT, 2);
		//configs.put(EntityType.REPOST, 1);
		//configs.put(EntityType.ATTITUDE, 0);
		configs.put(EntityType.FOLLOW, 5);
		configs.put(EntityType.FANS, 5);
		UserAll userAll = new UserAllCrawler().crawlByUserId(userId, drivers, configs);

		// 输出
		PrintStream ps = Utils.openFilePS("C:/Users/PeterYuan/Desktop/" + userId + ".json");
		ps.println(JsonUtils.toJson(userAll));
		ps.close();
		System.out.println(userId + ".json写入完毕");

	}


}
