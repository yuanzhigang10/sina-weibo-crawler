package ngn.yzg.swc.crawler;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import ngn.yzg.swc.entity.EntityType;
import ngn.yzg.swc.entity.Friend;
import ngn.yzg.swc.util.CrawlerUtils;
import ngn.yzg.swc.util.DriverUtils;

/**
 * 爬取用户<tt>Fans</tt>或<tt>Follow</tt>。
 * 
 * <p>
 * 根据<tt>UserId</tt>爬取某个用户的粉丝（<tt>Fans</tt>）或关注（<tt>Follow</tt>）好友信息。<br>
 * 爬取结果存储在{@link ngn.yzg.swc.entity.Friend Friend}对象中。<br>
 * 通过类函数{@code crawlByUserId}调用，完成爬取过程。
 * 
 * @author PeterYuan
 *
 */
public class FriendCrawler {
	public static boolean debugMode = false;
	
	private List<Friend> friends = new ArrayList<>();
	private int friendCount = 0;


	/**
	 * 根据用户<tt>UserId</tt>爬取其<tt>Fans/Follow</tt>信息。
	 * @param userId 待爬取用户的<tt>UserId</tt>，{@code String}类型
	 * @param driver 用于爬取的{@code driver}对象
	 * @param type 待爬取类型，<tt>Fans</tt>或<tt>Follow</tt>，均为{@link ngn.yzg.swc.entity.Friend Friend}类型的对象。
	 * @param maxPage 最大爬取页数
	 * @return 爬取的<tt>Fans/Follow</tt>结果，以{@code List<Friend>}形式保存
	 */
	public List<Friend> crawlByUserId(String userId, HtmlUnitDriver driver, EntityType type, int maxPage) {
		// 尝试访问主页，判断是否正常账号
		try {
			Long.parseLong(userId);
		} catch (Exception e) {
			System.err.println(this.getClass().getName() + ": please input userId");
			return null;
		}
		String url = "http://weibo.cn/" + userId;
		DriverUtils.safeGet(driver, url);
		if (driver.getCurrentUrl().contains("rand")) { // 待爬取的用户异常
			System.err.println(this.getClass().getName() + ": cannot access user " + userId);
			return null;
		}

		// 判断爬取类型，fans或者follow
		if (type.equals(EntityType.FANS)) {
			url = "http://weibo.cn/" + userId + "/fans";
		} else if (type.equals(EntityType.FOLLOW)) {
			url = "http://weibo.cn/" + userId + "/follow";
		} else {
			System.err.println(this.getClass().getName() + ": unkown FriendType");
			return null;
		}

		// 开始爬取fans/follow界面
		int page = 1;
		if (debugMode) { System.out.println(type + " page: " + page); }
		DriverUtils.safeGet(driver, url);
		boolean hasNextPage = parseFriendPage(driver, type); // 爬取该页
		while (hasNextPage && (page < maxPage)) {
			WebElement next = driver.findElement(By.linkText("下页"));
			DriverUtils.safeClick(next); // 进入下一页
			page++;
			if (debugMode) { System.out.println(type + " page: " + page); }
			hasNextPage = parseFriendPage(driver, type); // 爬取该页
		}

		if (debugMode) {
			System.out.println(type + " all done");
			System.out.println(type + " count: " + friendCount + "\n");
		}
		return friends;
	}



	/**
	 * 解析一个fans/follow界面
	 * 该函数中不断创建新的Friend对象，并添加到List中
	 */
	private boolean parseFriendPage(HtmlUnitDriver driver, EntityType type) {
		Document doc = Jsoup.parse(driver.getPageSource());
		Elements tables = doc.select("table");
		if (!tables.isEmpty()) {
			for (Element table : tables) {
				// 开始解析某一个用户
				Friend friend = new Friend();
				// 设置Friend类型： fans或follow
				friend.friendType = (type.equals(EntityType.FANS))? EntityType.FANS : EntityType.FOLLOW;
				friendCount++;
				// 寻找用户头像
				Elements imgs = table.select("img");
				if (!imgs.isEmpty()) {
					for (Element img : imgs) {
						if (img.attr("alt").equals("pic")) {
							friend.imageLink = img.attr("src");
							friend.id = CrawlerUtils.parseIdByImgLink(friend.imageLink);
							Element a = img.parent(); // img的父结点，即url所在处
							if (a != null) {
								friend.url = a.attr("href");
							}
						}
					}
				}
				// 解析用户信息
				parseInfo(table.text(), friend);
				// 此时得到了完整的friend对象，加入list
				friends.add(friend);
				if (debugMode) { System.out.println(friend.id + ": " + friend.name); }
			}
		}
		return CrawlerUtils.hasNextPage(doc);
	}


	/**
	 * 根据文本解析用户的基本信息
	 */
	private void parseInfo(String text, Friend friend) {
		String[] fields = text.split(" "); // 空格区分不同字段
		if (fields.length > 0) {
			friend.name = fields[0];
		}
		Pattern nonDigit = Pattern.compile("[^0-9]");
		Matcher m = Pattern.compile("粉丝\\d+人").matcher(text);
		if (m.find()) {
			friend.fansNum = Integer.parseInt(nonDigit.matcher(m.group()).replaceAll(""));
		}
	}


}
