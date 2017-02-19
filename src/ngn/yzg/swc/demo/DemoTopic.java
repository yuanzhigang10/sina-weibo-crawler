package ngn.yzg.swc.demo;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;

import ngn.yzg.swc.control.Console;
import ngn.yzg.swc.crawler.TopicCrawler;
import ngn.yzg.swc.entity.CrawlerException;
import ngn.yzg.swc.entity.EntityType;
import ngn.yzg.swc.entity.Weibo;
import ngn.yzg.swc.entity.WeiboAccount;
import ngn.yzg.swc.util.DriverUtils;
import ngn.yzg.swc.util.JsonUtils;
import ngn.yzg.swc.util.Utils;

import org.openqa.selenium.htmlunit.HtmlUnitDriver;

/**
 * TopicCrawler使用demo
 * @author PeterYuan
 *
 */
public class DemoTopic {

	public static void main(String[] args) throws CrawlerException {
		// 全局参数设置
		Console.debugMode = true; // 是否debug输出模式，不指定时默认false
		Console.waitTime = 2500L; // 单次访问后的等待时间，单位ms，不指定时默认2500ms
		Console.waitTimeIfBanned = 3600_000L; // 被封号后的等待时间，单位ms，不指定时默认3600_000ms，即1小时
		WeiboAccount account = new WeiboAccount("yuanzhigang10@126.com", "********");
		
		// 按照话题关键词搜索相关微博
		String keyword = "北京大雨";
		// 模拟登录，创建可用的HtmlUnitDriver对象
		HtmlUnitDriver[] drivers = DriverUtils.createUsableDrivers(account, 2);
		
		HashMap<EntityType, Integer> configs = new HashMap<>();
		// 配置各个对象爬取页数的信息，不指定默认全部爬取，0代表不爬取
		configs.put(EntityType.WEIBO, 2);
		configs.put(EntityType.COMMENT, 2);
		configs.put(EntityType.REPOST, 2);
		configs.put(EntityType.ATTITUDE, 0);
		// 其他配置
		configs.put(EntityType.HotMode, 1); // 按照热度排序
		configs.put(EntityType.DeepParseUserId, 0); // 不用深度解析userId
		// 调用TopicCrawler，开始爬取
		List<Weibo> weibos = new TopicCrawler().crawlByKeyword(keyword, drivers, configs);
		DriverUtils.closeDrivers(drivers);

		// 将获取的微博List写入json
		System.out.println(weibos.size());
		PrintStream ps = Utils.openFilePS("C:/Users/PeterYuan/Desktop/" + keyword + ".json");
		ps.println(JsonUtils.toJson(weibos));
		ps.close();
		System.out.println(keyword + ".json写入完毕");
	}

}
