package ngn.yzg.swc.crawler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ngn.yzg.swc.entity.EntityType;
import ngn.yzg.swc.entity.Weibo;
import ngn.yzg.swc.util.CrawlerUtils;
import ngn.yzg.swc.util.DriverUtils;
import ngn.yzg.swc.util.Utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

/**
 * 模拟微博话题搜索，爬取相关的微博。
 * 
 * <p>
 * 根据提供的关键词<tt>Keyword</tt>，搜索相关的微博。每条得到的微博存储在{@link ngn.yzg.swc.entity.Weibo Weibo}对象中。<br>
 * 通过类函数{@code crawlByKeyword}调用，完成爬取过程。
 * 
 * @author PeterYuan
 *
 */
public class TopicCrawler {
	public static boolean debugMode = false;

	private int weiboCount = 0;
	private List<Weibo> weibos = new ArrayList<>();
	

	/**
	 * 根据关键词，爬取得到<tt>WeiboList</tt>。<br>
	 * 注意：{@code HtmlUnitDriver}需要提供至少2个可用的{@code drivers}。
	 * @param keyword 搜索关键词。
	 * @param drivers 用于爬取的{@code driver}对象。
	 * @param configs 最大爬取数目的配置信息，{@code HashMap<EntityType, Integer>}类型，
	 * 配置说明可参考{@link ngn.yzg.swc.demo.DemoTopic DemoTopic}里的示例。
	 * @return 爬取获得的<tt>WeiboList</tt>，{@code List<Weibo>}类型。
	 */
	public List<Weibo> crawlByKeyword(String keyword, HtmlUnitDriver[] drivers, HashMap<EntityType, Integer> configs) {
		if (drivers.length < 2) {
			System.err.println(this.getClass().getName() + ": need at least 2 drivers!");
			System.exit(0);
		}
		HtmlUnitDriver topicDriver = drivers[0];
		HtmlUnitDriver auxDriver = drivers[1];
		boolean hot = (Utils.getOrDefault(configs, EntityType.HotMode)) != 0; // 是否按照热度搜索，没有配置时默认是
		
		// 进入微博搜索界面
		DriverUtils.safeGet(topicDriver, "http://weibo.cn/search/?tf=5_012&vt=4&PHPSESSID=");
		WebElement keywordBlock = topicDriver.findElement(By.name("keyword"));
		keywordBlock.sendKeys(Utils.parseKeyword(keyword));
		WebElement smblog = topicDriver.findElement(By.name("smblog"));
		DriverUtils.safeClick(smblog); // 点击搜索
		
		// 是否按照“热门”显示
		if (hot) {
			WebElement byHot = topicDriver.findElement(By.linkText("热门"));
			DriverUtils.safeClick(byHot);
			if (debugMode) { System.out.println("topic search: hot mode"); }
		}

		// 开始爬取所有页面
		int weiboPage = 1;
		if (debugMode) { System.out.println("weibo page: " + weiboPage); }
		boolean hasNextPage = parseWeiboPage(topicDriver, auxDriver, configs);
		while (hasNextPage && (weiboPage < Utils.getOrDefault(configs, EntityType.WEIBO))) {
			WebElement next = topicDriver.findElement(By.linkText("下页"));
			DriverUtils.safeClick(next); // 进入下一页
			weiboPage++;
			if (debugMode) { System.out.println("weibo page: " + weiboPage); }
			hasNextPage = parseWeiboPage(topicDriver, auxDriver, configs); // 爬取该页
		}

		if (debugMode) { System.out.println("this topic search done"); }
		return weibos;
	}
	
	

	/**
	 * 解析一个微博页面
	 * 需要借助parseWeibo实现，以获取微博的各种信息
	 */
	private boolean parseWeiboPage(HtmlUnitDriver topicDriver, HtmlUnitDriver auxDriver, HashMap<EntityType, Integer> configs) {
		boolean deepParseId = (Utils.getOrDefault(configs, EntityType.DeepParseUserId)) != 0; // 是否深度解析userId，默认是
		Document doc = Jsoup.parse(topicDriver.getPageSource());
		Elements contents = doc.select("div.c"); // 可能是微博的区域
		for (Element content : contents) {
			if (content.hasAttr("id")) { // 确实是一条微博
				this.weiboCount += 1;
				if (debugMode) { System.out.println("parse weibo " + this.weiboCount + " ..."); }
				Weibo weibo = CrawlerUtils.parseWeibo(content, auxDriver, false, "", deepParseId); // Topic模式解析微博
				// 爬取评论
				int maxCommentPage = Utils.getOrDefault(configs, EntityType.COMMENT);
				if (maxCommentPage != 0 && weibo.commentNum != 0) {
					new WeiboAuxCrawler().crawlWeiboAux(weibo, auxDriver, EntityType.COMMENT, maxCommentPage);
				}
				// 爬取转发
				int maxRepostPage = Utils.getOrDefault(configs, EntityType.REPOST);
				if (maxRepostPage != 0 && weibo.repostNum != 0) {
					new WeiboAuxCrawler().crawlWeiboAux(weibo, auxDriver, EntityType.REPOST, maxRepostPage);
				}
				// 爬取赞
				int maxAttitudePage = Utils.getOrDefault(configs, EntityType.ATTITUDE);
				if (maxAttitudePage != 0 && weibo.attitudeNum != 0) {
					new WeiboAuxCrawler().crawlWeiboAux(weibo, auxDriver, EntityType.ATTITUDE, maxAttitudePage);
				}
				// 此处获得了一个完整的Weibo对象
				weibos.add(weibo);
			}
		}
		return CrawlerUtils.hasNextPage(doc);
	}


}

