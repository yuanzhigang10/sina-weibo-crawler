package ngn.yzg.swc.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ngn.yzg.swc.crawler.IdCrawler;
import ngn.yzg.swc.entity.Weibo;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

/**
 * 爬取工具类。
 * 
 * <p>
 * 该类提供了一系列用于爬取的工具函数。
 * 
 * @author yzg
 *
 */
public final class CrawlerUtils {
	public static boolean debugMode4DeepParse = false;

	private CrawlerUtils() {}

	/**
	 * 判断网页中是否包含“下页”的链接。
	 * @param doc 网页<tt>html</tt>字段对应的{@code Document}对象。
	 * @return 该网页页面是否含有“下页”按钮。
	 */
	public static boolean hasNextPage(Document doc) {
		boolean hasNext = false;
		if (doc != null) {
			Elements links = doc.select("a[href]");
			if (!links.isEmpty()) {
				for (Element link : links) {
					if (link.text().equals("下页")) {
						hasNext = true;
						break;
					}
				}
			}
		}
		return hasNext;
	}



	/**
	 * 根据<tt>url</tt>解析出<tt>UserId</tt>，浅解析。
	 * 
	 * <p>
	 * 浅解析，即仅从<tt>url</tt>中寻找可能包含的<tt>UserId</tt>。
	 * @param userUrl 用户<tt>url</tt>。
	 * @return 解析出的<tt>UserId</tt>，没有解析出则为0L。
	 */
	public static long parseIdByUserUrl(String userUrl) {
		Matcher m = Pattern.compile("/\\d{10}?").matcher(userUrl);
		if (m.find()) {
			String userId = m.group().substring(1, m.group().length());
			return Long.parseLong(userId);
		}
		return 0L;
	}

	/**
	 * {@code parseIdByUserUrl}重载版本，深度解析<tt>UserId</tt>。
	 * 
	 * <p>
	 * 即，如果<tt>url</tt>中不包含<tt>UserId</tt>信息，则访问其主页，爬取<tt>UserId</tt>。
	 * 爬取时，需要调用类{@link ngn.yzg.swc.crawler.IdCrawler IdCrawler}的{code parseIdByUserUrl}函数。
	 * @param userUrl 待解析用户的<tt>url</tt>。
	 * @param idDriver 用于爬取的{@code driver}对象。
	 * @return 解析得到的<tt>UserId</tt>，如果通过爬取仍未找到，则返回0L。
	 */
	public static long parseIdByUserUrl(String userUrl, HtmlUnitDriver idDriver) {
		Matcher m = Pattern.compile("/\\d{10}?").matcher(userUrl);
		if (m.find()) {
			String userId = m.group().substring(1, m.group().length());
			return Long.parseLong(userId);
		} else {
			if (debugMode4DeepParse) { System.out.println("deep parse id: " + userUrl); }
			String newId = new IdCrawler().crawlByUserUrl(userUrl, idDriver);
			return Long.parseLong(newId);
		}
	}


	/**
	 * 根据用户的头像链接解析出<tt>UserId</tt>。
	 * 
	 * <p>
	 * 这种解析方法不用再次访问用户主页，当有头像链接时，建议采用该方法。
	 * @param imgLink 头像链接。
	 * @return 解析出的用户<tt>UserId</tt>。
	 */
	public static long parseIdByImgLink(String imgLink) {
		// example: "http://tp3.sinaimg.cn/5131929066/50/22874183761/1"
		Matcher m = Pattern.compile("sinaimg\\.cn/\\d{10}/").matcher(imgLink);
		Pattern nonDigit = Pattern.compile("[^0-9]");
		if (m.find()) {
			return Long.parseLong(nonDigit.matcher(m.group()).replaceAll(""));
		} else {
			return 0L;
		}
	}



	/**
	 * 从微博文本中解析出微博时间。
	 * @param text 微博文本。
	 * @return 解析出的{@link java.util.Date Date}对象。
	 */
	public static Date parseTime(String text) {
		Date result = new Date();
		// 形如“05月21日 18:17”
		Matcher m = Pattern.compile("\\d{2}月\\d{2}日 \\d{2}:\\d{2}").matcher(text);
		if (m.find()) {
			String timeText = m.group();
			Calendar c = Calendar.getInstance();
			try {
				Date d = new SimpleDateFormat("MM月dd日 HH:mm").parse(timeText);
				c.setTime(d);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			c.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
			result = c.getTime();
		}
		// 形如“26分钟前”
		m = Pattern.compile("\\d+分钟前").matcher(text);
		if (m.find()) {
			String timeText = m.group().replaceAll("分钟前", "");
			int dist = Integer.parseInt(timeText);
			Calendar c = Calendar.getInstance();
			c.add(Calendar.MINUTE, 0-dist);
			result = c.getTime();
		}
		// 形如“今天 09:27”
		m = Pattern.compile("今天 \\d{2}:\\d{2}").matcher(text);
		if (m.find()) {
			String timeText = m.group();
			Calendar c = Calendar.getInstance();
			try {
				Date d = new SimpleDateFormat("今天 HH:mm").parse(timeText);
				c.setTime(d);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			Calendar now = Calendar.getInstance();
			c.set(Calendar.YEAR, now.get(Calendar.YEAR));
			c.set(Calendar.MONTH, now.get(Calendar.MONTH));
			c.set(Calendar.DATE, now.get(Calendar.DATE));
			result = c.getTime();
		}
		return result;
	}


	/**
	 * 将微博对应<tt>html</tt>字段(<tt>div.c</tt>)解析为{@link ngn.yzg.swc.entity.Weibo Weibo}对象。
	 * 
	 * <p>
	 * 注：该函数仅存储评论<tt>url</tt>，不进一步爬取“评论/转发/赞”页面。
	 * @param content 微博对应的<tt>html</tt>字段。
	 * @param idDriver 用于爬取解析微博相关的<tt>UserId</tt>信息。
	 * @param hasId 爬取之前是否已知发微用户<tt>UserId</tt>，用于区分不同模式：<tt>WeiboCrawler/TopicCrawler</tt>。
	 * @param userId 发微用户<tt>UserId</tt>，当{@code hasId}为{@code false}时，该参数无意义。
	 * @return 解析得到的{@link ngn.yzg.swc.entity.Weibo Weibo}对象。
	 */
	public static Weibo parseWeibo(Element content, HtmlUnitDriver idDriver, boolean hasId, String userId, boolean deepParseId) {
		Weibo weibo = new Weibo();
		// 个人信息
		if (hasId) { // WeiboCrawler模式，已知id
			weibo.userId = Long.parseLong(userId);
			weibo.userUrl = "http://weibo.cn/" + userId;
		} else { // TopicCrawler模式，id在a.nk字段内
			Elements userInfos = content.select("a.nk");
			if (!userInfos.isEmpty()) {
				Element userInfo = userInfos.first();
				weibo.userName = userInfo.text();
				weibo.userUrl = userInfo.attr("href");
			}
		}
		// 转发信息
		Elements postInfos = content.select("span.cmt");
		if (!postInfos.isEmpty()) {
			for (Element postInfo : postInfos) {
				String postText = postInfo.text();
				if (postText.contains("转发了") && postText.contains("微博")) {
					// 此时才确定有转发源
					Elements postLinks = postInfo.select("a[href]");
					if (!postLinks.isEmpty()) {
						Element postLink = postLinks.first();
						weibo.postName = postLink.text();
						weibo.postUrl = postLink.attr("href");
					}
				}
			}
		}
		// “转发、评论、赞”等内容的解析
		Elements links = content.select("a");
		if (!links.isEmpty()) {
			for (Element link : links) {
				String linkText = link.text();
				Matcher matcher = Pattern.compile("[^0-9]").matcher(linkText);
				String num = matcher.replaceAll(""); // 抽取数字
				if (linkText.matches("评论\\[\\d+\\]")) {
					weibo.commentUrl = link.attr("href");
					weibo.commentNum = Integer.parseInt(num);
				}
				if (linkText.matches("转发\\[\\d+\\]")) {
					weibo.repostNum = Integer.parseInt(num);
				}
				if (linkText.matches("赞\\[\\d+\\]")) {
					weibo.attitudeNum = Integer.parseInt(num);
				}
			}
		}
		// 微博文本解析
		weibo.text = content.text();
		// 时间解析
		weibo.createdTime = CrawlerUtils.parseTime(weibo.text);
		// userId解析
		if (!hasId) { // 没有userId（Topic模式），但构建微博id时需要，因此需要深度解析
			weibo.userId = CrawlerUtils.parseIdByUserUrl(weibo.userUrl, idDriver);
		}
		if (!weibo.postUrl.trim().equals("")) { // 解析post得到转发源的userId
			if (deepParseId) {
				weibo.postId = CrawlerUtils.parseIdByUserUrl(weibo.postUrl, idDriver);
			} else {
				weibo.postId = CrawlerUtils.parseIdByUserUrl(weibo.postUrl);
			}
		}
		weibo.refreshId();

		return weibo;
	}



}
