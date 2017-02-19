package ngn.yzg.swc.crawler;

import java.util.HashMap;
import java.util.List;

import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import ngn.yzg.swc.entity.EntityType;
import ngn.yzg.swc.entity.Friend;
import ngn.yzg.swc.entity.UserAll;
import ngn.yzg.swc.entity.UserInfo;
import ngn.yzg.swc.entity.Weibo;
import ngn.yzg.swc.util.Utils;


/**
 * 用户所有信息爬取类。
 * 
 * <p>
 * 通过调用
 * {@link ngn.yzg.swc.crawler.UserInfoCrawler UserInfoCrawler}，
 * {@link ngn.yzg.swc.crawler.WeiboCrawler WeiboCrawler}和
 * {@link ngn.yzg.swc.crawler.FriendCrawler FriendCrawler}实现。<br>
 * 通过类函数{@code crawlByUserId}调用，完成爬取过程。
 * 
 * @author PeterYuan
 *
 */
public class UserAllCrawler {
	public static boolean debugMode = false;
	
	private int weiboNum = 0;
	private int fansNum = 0;
	private int followNum = 0;
	
	private UserAll userAll = new UserAll();
	
	/**
	 * 根据用户<tt>UserId</tt>爬取用户所有信息（基本信息、微博、粉丝、关注）。
	 * @param userId 用户<tt>UserId</tt>。
	 * @param drivers 用于爬取的{@code driver}对象，需保证至少有2个可用。
	 * @param configs 最大爬取数目的配置信息，{@code HashMap<EntityType, Integer>}类型，
	 * 配置说明可参考{@link ngn.yzg.swc.demo.DemoUserAll DemoUserAll}里的示例。
	 * @return 用户所有信息，存储在{@link ngn.yzg.swc.entity.UserAll UserAll}对象中。
	 */
	public UserAll crawlByUserId(String userId, HtmlUnitDriver[] drivers, HashMap<EntityType, Integer> configs) {
		if (drivers.length < 2) {
			System.err.println(this.getClass().getName() + ": need at least 2 drivers!");
			return null;
		}
		
		// 首先调用UserInfo类，爬取基本信息，同时判断账号是否正常
		UserInfo userInfo = new UserInfoCrawler().crawlByUserId(userId, drivers[0]);
		if (userInfo == null) {
			System.err.println(this.getClass().getName() + ": cannot access user " + userId);
			return null;
		}
		
		// 爬取微博
		List<Weibo> weiboList = new WeiboCrawler().crawlByUserId(userId, drivers, configs);
		weiboNum = weiboList.size();
		
		// 爬取粉丝
		int maxFansPage = Utils.getOrDefault(configs, EntityType.FANS);
		List<Friend> fansList = new FriendCrawler().crawlByUserId(userId, drivers[0], EntityType.FANS, maxFansPage);
		fansNum = fansList.size();
		
		// 爬取关注
		int maxFollowPage = Utils.getOrDefault(configs, EntityType.FOLLOW);
		List<Friend> followList = new FriendCrawler().crawlByUserId(userId, drivers[0], EntityType.FOLLOW, maxFollowPage);
		followNum = followList.size();
		
		// 汇总信息
		userAll.userInfo = userInfo;
		if (debugMode) { System.out.println("user: " + userId + ", " + userInfo.name); }
		if (weiboNum != 0) {
			userAll.weiboList = weiboList;
			// 根据UserInfo信息，补全微博内的空缺信息
			for (Weibo weibo : userAll.weiboList) {
				weibo.userName = userInfo.name;
			}
			if (debugMode) { System.out.println("weibo num: " + weiboNum); }
		}
		if (fansNum != 0) {
			userAll.fansList = fansList;
			if (debugMode) { System.out.println("fans num: " + fansNum); }
		}
		if (followNum != 0) {
			userAll.followList = followList;
			if (debugMode) { System.out.println("follow num: " + followNum); }
		}
		
		// 返回结果
		return userAll;
	}
		
}
