package ngn.yzg.swc.crawler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import ngn.yzg.swc.entity.EntityType;
import ngn.yzg.swc.entity.Weibo;
import ngn.yzg.swc.util.CrawlerUtils;
import ngn.yzg.swc.util.DriverUtils;
import ngn.yzg.swc.util.Utils;


/**
 * 微博（<tt>Weibo</tt>）爬取类。
 * 
 * <p>
 * 根据用户<tt>UserId</tt>，爬取该用户的微博（<tt>Weibo</tt>）。<br>
 * 通过类函数{@code crawlByUserId}调用，完成爬取过程。
 * 
 * @author PeterYuan
 *
 */
public class WeiboCrawler {
	public static boolean debugMode = false;
	
	private int weiboCount = 0;
	private List<Weibo> weibos = new ArrayList<>();

	
	/**
	 * 根据用户<tt>UserId</tt>爬取微博。
	 * 
	 * <p>
	 * 注：该函数不爬取微博对应的评论，但将评论<tt>url</tt>记录下来，供后续使用。
	 * @param userId 用户<tt>UserId</tt>
	 * @param drivers 用于爬取的{@code driver}对象
	 * @param configs 最大爬取数目的配置信息，{@code HashMap<EntityType, Integer>}类型，
	 * 配置说明可参考{@link ngn.yzg.swc.demo.DemoWeibo DemoWeibo}里的示例。
	 * @return 爬取得到的微博，存储在{@code List<Weibo>}中。
	 */
	public List<Weibo> crawlByUserId(String userId, HtmlUnitDriver[] drivers, HashMap<EntityType, Integer> configs) {
		if (drivers.length < 2) {
			System.err.println(this.getClass().getName() + ": need at least 2 drivers!");
			return null;
		}
		HtmlUnitDriver weiboDriver = drivers[0];
		HtmlUnitDriver idDriver = drivers[1];

		String seed = "http://weibo.cn/" + userId +  "?vt=4";
		DriverUtils.safeGet(weiboDriver, seed);
		if (weiboDriver.getCurrentUrl().contains("rand")) { // 待爬取的用户异常
			System.err.println(this.getClass().getName() + ": cannot access user " + userId);
			return null;
		}

		// 开始爬取微博界面
		int weiboPage = 1;
		if (debugMode) { System.out.println("weibo page: " + weiboPage); }
		boolean hasNextPage = parseWeiboPage(weiboDriver, idDriver, userId, configs);
		while (hasNextPage && (weiboPage < Utils.getOrDefault(configs, EntityType.WEIBO))) {
			WebElement next = weiboDriver.findElement(By.linkText("下页"));
			DriverUtils.safeClick(next); // 进入下一页
			weiboPage++;
			if (debugMode) { System.out.println("weibo page: " + weiboPage); }
			hasNextPage = parseWeiboPage(weiboDriver, idDriver, userId, configs); // 爬取该页
		}

		if (debugMode) {
			System.out.println("weibo all done");
			System.out.println("weibo count: " + weiboCount);
		}
		return weibos;
	}

	

	/**
	 * 解析一个微博界面
	 */
	private boolean parseWeiboPage(HtmlUnitDriver weiboDriver, HtmlUnitDriver auxDriver, String userId, HashMap<EntityType, Integer> configs) {
		Document doc = Jsoup.parse(weiboDriver.getPageSource());
		Elements contents = doc.select("div.c"); // 可能是微博的区域
		for (Element content : contents) {
			if (content.hasAttr("id")) { // 确实是一条微博
				this.weiboCount += 1;
				//if (Console.debugMode) { System.out.println("parse weibo " + this.weiboCount + " ..."); }
				Weibo weibo = CrawlerUtils.parseWeibo(content, auxDriver, true, userId, true); // UserId模式解析微博
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
