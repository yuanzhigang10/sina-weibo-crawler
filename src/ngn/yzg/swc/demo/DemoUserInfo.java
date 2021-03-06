package ngn.yzg.swc.demo;

import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import ngn.yzg.swc.control.Console;
import ngn.yzg.swc.crawler.UserInfoCrawler;
import ngn.yzg.swc.entity.CrawlerException;
import ngn.yzg.swc.entity.UserInfo;
import ngn.yzg.swc.entity.WeiboAccount;
import ngn.yzg.swc.util.DriverUtils;
import ngn.yzg.swc.util.JsonUtils;

/**
 * UserInfoCrawler使用demo
 * @author PeterYuan
 *
 */
public class DemoUserInfo {

	public static void main(String[] args) throws CrawlerException {
		// 全局参数设置
		Console.debugMode = true; // 是否debug输出模式，不指定时默认false
		Console.waitTime = 2500L; // 单次访问后的等待时间，单位ms，不指定时默认2500ms
		Console.waitTimeIfBanned = 3600_000L; // 被封号后的等待时间，单位ms，不指定时默认3600_000ms，即1小时
		WeiboAccount account = new WeiboAccount("yuanzhigang10@126.com", "********");

		// 根据userId爬取用户信息
		HtmlUnitDriver driver = DriverUtils.createUsableDrivers(account, 1)[0];
		UserInfo userInfo = new UserInfoCrawler().crawlByUserId("1635563814", driver);
		driver.close();

		// 输出结果
		System.out.println(JsonUtils.toJson(userInfo));
	}


}
