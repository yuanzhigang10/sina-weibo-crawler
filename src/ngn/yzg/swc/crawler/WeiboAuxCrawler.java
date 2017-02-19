package ngn.yzg.swc.crawler;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ngn.yzg.swc.entity.EntityType;
import ngn.yzg.swc.entity.Weibo;
import ngn.yzg.swc.entity.WeiboAux;
import ngn.yzg.swc.util.CrawlerUtils;
import ngn.yzg.swc.util.DriverUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

/**
 * 爬取“评论/转发/赞”类
 * 
 * <p>
 * package访问权限，一般不单独调用；主要供爬取微博的类调用。
 * 
 * @author PeterYuan
 */
class WeiboAuxCrawler {
	public static boolean debugMode = false;

	private int count = 0;


	/**
	 * 爬取某条微博对应的评论/转发/赞
	 * @param weibo 待Weibo对象，含有评论url和待写入的list
	 * @param driver 用于爬取的driver对象
	 */
	public void crawlWeiboAux(Weibo weibo, HtmlUnitDriver driver, EntityType type, int maxPage) {
		// 根据commentUrl解析出评论/转发/赞的地址
		String url = weibo.commentUrl.split("\\?")[0];
		if (type.equals(EntityType.REPOST)) {
			url = url.replace("comment", "repost");
		}
		if (type.equals(EntityType.ATTITUDE)) {
			url = url.replace("comment", "attitude");
		}
		DriverUtils.safeGet(driver, url); // 访问

		// 爬取所有页面
		int page = 1;
		if (debugMode) { System.out.println(type + " page: " + page); }
		boolean hasNextPage = parsePage(weibo, driver, type);
		while (hasNextPage && (page < maxPage)) {
			WebElement next = driver.findElement(By.linkText("下页"));
			DriverUtils.safeClick(next); // 进入下一页
			page++;
			if (debugMode) { System.out.println(type + " page: " + page); }
			hasNextPage = parsePage(weibo, driver, type);
		}
	}



	/**
	 * 解析一个界面
	 */
	private boolean parsePage(Weibo weibo, HtmlUnitDriver driver, EntityType type) {
		Document doc = Jsoup.parse(driver.getPageSource()); // 得到当前界面内容
		List<Element> divcs = doc.select("div.c");
		if (divcs.size() > 4) {
			divcs = divcs.subList(3, divcs.size()-1); // 去除前3个和最后1个div.c
		}
		for (Element divc : divcs) {
			this.count += 1;
			if (debugMode) { System.out.println("parse " + type + " " + this.count + " ..."); }
			WeiboAux weiboAux = parseWeiboAux(divc, type);
			// 选择待添加的list
			List<WeiboAux> list = (type.equals(EntityType.COMMENT)) ? (weibo.commentList) : (
					(type.equals(EntityType.REPOST)) ? (weibo.repostList) : (weibo.attitudeList));
			list.add(weiboAux);
		}
		return CrawlerUtils.hasNextPage(doc);
	}



	/**
	 * 将div.c字段解析为WeiboAux对象
	 */
	private WeiboAux parseWeiboAux(Element divc, EntityType type) {
		WeiboAux weiboAux = new WeiboAux();

		// 个人信息（全部需要）
		Elements links = divc.select("a");
		if (!links.isEmpty()) {
			Element userInfo = links.first();
			weiboAux.userName = userInfo.text();
			weiboAux.userUrl = userInfo.attr("href");
		}
		// 回复信息 （仅回复需要）
		if (type.equals(EntityType.COMMENT)) {
			Elements replyInfos = divc.select("span.ctt");
			if (!replyInfos.isEmpty()) {
				for (Element replyInfo : replyInfos) {
					String replyText = replyInfo.text();
					if (replyText.startsWith("回复")) {
						// 此时才确定该评论是回复某人
						Elements replyLinks = replyInfo.select("a[href]");
						if (!replyLinks.isEmpty()) {
							Element replyLink = replyLinks.first();
							weiboAux.replyName = replyLink.text().substring(1); //去掉@
							weiboAux.replyUrl = replyLink.attr("href");
						}
					}
				}
			}
		}
		// 赞的数目（评论、转发）
		if (type.equals(EntityType.COMMENT) || type.equals(EntityType.REPOST)) {
			if (!links.isEmpty()) {
				for (Element link : links) {
					String linkText = link.text();
					if (linkText.matches("赞\\[\\d+\\]")) {
						Matcher matcher = Pattern.compile("[^0-9]").matcher(linkText);
						String num = matcher.replaceAll(""); // 抽取数字
						weiboAux.likeNum = Integer.parseInt(num);
					}
				}
			}
		}
		// 后处理
		weiboAux.text = divc.text();
		weiboAux.createdTime = CrawlerUtils.parseTime(weiboAux.text);
		weiboAux.userId = CrawlerUtils.parseIdByUserUrl(weiboAux.userUrl);
		if (type.equals(EntityType.COMMENT)) {
			weiboAux.replyId = CrawlerUtils.parseIdByUserUrl(weiboAux.replyUrl);
		}

		return weiboAux;
	}



}
