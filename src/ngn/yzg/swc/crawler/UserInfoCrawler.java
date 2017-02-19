package ngn.yzg.swc.crawler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import ngn.yzg.swc.entity.UserInfo;
import ngn.yzg.swc.util.DriverUtils;
import ngn.yzg.swc.util.Utils;

/**
 * 用户基本信息（<tt>UserInfo</tt>）爬取类。
 * 
 * <p>
 * 根据用户<tt>UserId</tt>，爬取该用户主要基本信息。<br>
 * 通过类函数{@code crawlByUserId}调用，完成爬取过程。
 * 
 * @author PeterYuan
 *
 */
public class UserInfoCrawler {
	public static boolean debugMode = false;
	
	private UserInfo userInfo = new UserInfo();

	/**
	 * 根据用户userId，爬取该用户的信息
	 * @param driver 用于爬取的driver对象，请确保其已经可用
	 * @param userId 待爬取的用户的id
	 * @return 爬取得到的User对象
	 */
	public UserInfo crawlByUserId(String userId, HtmlUnitDriver driver) {
		// 需要确保driver可用
		userInfo.url = "http://weibo.cn/" + userId;
		try {
			userInfo.userID = Long.parseLong(userId);
		} catch (Exception e) {
			System.err.println(this.getClass().getName() + ": please input userId");
			return null;
		}
		
		// 访问主页，获取微博、关注、粉丝数量
		DriverUtils.safeGet(driver, userInfo.url);
		if (driver.getCurrentUrl().contains("rand")) { // 待爬取的用户异常
			System.err.println(this.getClass().getName() + ": cannot access user " + userId);
			return null;
		}
		
		Document doc = Jsoup.parse(driver.getPageSource());
		Elements divtip2s = doc.select("div.tip2");
		if (!divtip2s.isEmpty()) {
			for (Element divtip2 : divtip2s) {
				String numText = divtip2.text();
				if (numText.contains("微博") && numText.contains("关注") && numText.contains("粉丝")) {
					parseNumInfo(numText, userInfo);
				}
			}
		}
		// 访问info信息页，获取基本信息
		DriverUtils.safeGet(driver, "http://weibo.cn/" + userId + "/info");
		doc = Jsoup.parse(driver.getPageSource());
		Elements divcs = doc.select("div.c");
		if (!divcs.isEmpty()) {
			for (Element divc : divcs) {
				// 寻找用户头像
				Elements imgs = divc.select("img");
				if (!imgs.isEmpty()) {
					Element img = imgs.first();
					if (img.attr("alt").equals("头像")) {
						userInfo.imagelink = img.attr("src");
					}
				}
				// 寻找基本信息
				Element tip = divc.previousElementSibling();
				if (tip.text().equals("基本信息")) {
					divc.select("br").append("===");
					parseBasicInfo(divc.text(), userInfo);
				}
			}
		}
		if (debugMode) { System.out.println(userId + " basic info done"); }
		// 返回爬取获得的User对象
		return userInfo;
	}


	/**
	 * 根据数字信息对应的文本解析出相应的User字段
	 * @param numText
	 * @param user
	 */
	private void parseNumInfo(String numText, UserInfo userInfo) {
		Pattern nonDigit = Pattern.compile("[^0-9]");
		Matcher m1 = Pattern.compile("微博\\[\\d+\\]").matcher(numText);
		Matcher m2 = Pattern.compile("关注\\[\\d+\\]").matcher(numText);
		Matcher m3 = Pattern.compile("粉丝\\[\\d+\\]").matcher(numText);
		if (m1.find()) {
			userInfo.statusesCount = Integer.parseInt(nonDigit.matcher(m1.group()).replaceAll(""));
		}
		if (m2.find()) {
			userInfo.friendsCount = Integer.parseInt(nonDigit.matcher(m2.group()).replaceAll(""));
		}
		if (m3.find()) {
			userInfo.followersCount = Integer.parseInt(nonDigit.matcher(m3.group()).replaceAll(""));
		}
	}


	/**
	 * 根据基本信息对应的文本解析出相应的User字段
	 * @param basicInfo
	 * @param user
	 */
	private void parseBasicInfo(String basicInfo, UserInfo userInfo) {
		String[] fields = basicInfo.split("===");
		for (String field : fields) {
			String text = field.trim();
			if (text.startsWith("昵称:")) {
				userInfo.name = text.replace("昵称:", "");
				userInfo.screenName = userInfo.name;
			} else if (text.startsWith("性别:")) {
				userInfo.gender = text.replace("性别:", "");
			} else if (text.startsWith("地区:")) {
				userInfo.location = text.replace("地区:", "");
			} else if (text.startsWith("简介:")) {
				userInfo.description = Utils.filterEmoji(text.replace("简介:", ""));
			} else if (text.startsWith("认证:")) {
				userInfo.verified = true;
				userInfo.verifiedReason = Utils.filterEmoji(text.replace("认证:", ""));
			}
		}
	}

}
