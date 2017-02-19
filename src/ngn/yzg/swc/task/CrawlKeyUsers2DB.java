package ngn.yzg.swc.task;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;

import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import ngn.yzg.swc.control.Console;
import ngn.yzg.swc.crawler.UserAllCrawler;
import ngn.yzg.swc.entity.CrawlerException;
import ngn.yzg.swc.entity.EntityType;
import ngn.yzg.swc.entity.Friend;
import ngn.yzg.swc.entity.UserAll;
import ngn.yzg.swc.entity.Weibo;
import ngn.yzg.swc.entity.WeiboAccount;
import ngn.yzg.swc.service.Message;
import ngn.yzg.swc.service.User;
import ngn.yzg.swc.service.WeiboUtils;
import ngn.yzg.swc.util.DriverUtils;
import ngn.yzg.swc.util.SqlUtils;
import ngn.yzg.swc.util.Utils;


public class CrawlKeyUsers2DB extends Observable implements Runnable {
	/////////////////////////////////////////////////////////////////////////
	private static boolean serverMode = true;
	/////////////////////////////////////////////////////////////////////////

	private static ArrayDeque<Long> seeds = new ArrayDeque<>(); // 待爬取队列
	private static HashSet<Long> badIdSet = new HashSet<>();

	private static int startSeedNum = 50;
	private static int fansNumThresholdOfVIP = 10000;


	private static List<Long> getUnCrawledKeyUserIds(Connection conn, int num) {
		if (num <= 0)
			return null;
		List<Long> ids = new ArrayList<>();
		String querySql = "SELECT WeiboUserTopic.userID FROM WeiboUserTopic LEFT JOIN UserFullInfo " +
				"ON (WeiboUserTopic.userID = UserFullInfo.userId) WHERE UserFullInfo.userId IS NULL " +
				"ORDER BY followersCount DESC LIMIT " + num + ";";
		ResultSet rs = SqlUtils.select(conn, querySql);
		try {
			while (rs.next()) {
				ids.add(rs.getLong("userID"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return ids;
	}



	/**
	 * 线程的run()方法，当调用该线程start()方法时，自动执行此方法。
	 * <p>
	 * 注意：不要人工调用此方法！
	 */
	@Override
	public void run() {
		try {
			this.runBody(); // 执行方法主体
		} catch (Exception e) {
			System.err.println("an unknown exception from CrawlUsers2DB, calling listener to restart...");
			e.printStackTrace();
			// 通知观察者
			super.setChanged();
			notifyObservers("CrawlUsers2DB");
		}
	}


	/**
	 * 程序运行主体
	 */
	@SuppressWarnings("deprecation")
	private void runBody() throws CrawlerException {
		// 配置数据库
		// TODO
		String dbPassword = serverMode ? "******" : "******";
		Connection conn = SqlUtils.getConnection("Weibo", dbPassword);
		// 读取account配置
		String username = null;
		String password = null;
		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream("account.properties"));
		} catch (Exception e) {
			System.err.println("load \"account.properties\" fail");
		}
		if (prop.containsKey("username") && prop.containsKey("password")) {
			System.err.println("load \"account.properties\" ok");
			username = prop.getProperty("username");
			password = prop.getProperty("password");
		} else {
			System.err.println("use default instead");
			username = "yuanzhigang10@163.com";
			password = "********";
		}
		// 配置爬取参数
		Console.debugMode = true;
		UserAllCrawler.debugMode = true;
		Console.waitTime = 8*1000L; // 单次访问后的等待时间，8s
		Console.waitTimeIfBanned = 60*60*1000L; // 被封号后的等待时间，1小时
		WeiboAccount account = new WeiboAccount(username, password);
		HtmlUnitDriver[] drivers = DriverUtils.createUsableDrivers(account, 2);
		HashMap<EntityType, Integer> configs = new HashMap<>();
		configs.put(EntityType.WEIBO, serverMode?50:1);
		configs.put(EntityType.COMMENT, 0);
		configs.put(EntityType.REPOST, 0);
		configs.put(EntityType.ATTITUDE, 0);
		if (!serverMode) {
			configs.put(EntityType.FOLLOW, 1); // 不设置表示最大
			configs.put(EntityType.FANS, 1);
		}
		configs.put(EntityType.DeepParseUserId, serverMode?1:0); // 要深度解析转发源


		// 初始化完毕，开始读取数据、爬取
		System.out.println("[init] fetching " + startSeedNum + " users to crawl from database...");
		List<Long> idList = getUnCrawledKeyUserIds(conn, startSeedNum);
		for (Long id : idList) {
			seeds.addLast(id); // 将数据库中粉丝数最高且未被爬取的账号加入种子
		}
		while (!seeds.isEmpty()) {
			// 检查剩余内存空间，低于10%时程序自动退出
			System.err.println(Utils.getMemoryInfo());
			if ((double)Runtime.getRuntime().freeMemory()/Runtime.getRuntime().totalMemory() < 0.1) {
				System.err.println("memory alert!");
				System.exit(0);
			}
			// 从队列头部取出待爬取id，开始新的爬取
			Long userId = seeds.removeFirst();
			if (badIdSet.contains(userId) || WeiboUtils.isExistedUserInFullInfoById(conn, userId)) {
				continue;
			}
			UserAll userAll = null;
			try {
				System.out.println("[crawl] start crawling user " + userId + " at [" + new Date().toLocaleString() + "]");
				userAll = new UserAllCrawler().crawlByUserId(userId.toString(), drivers, configs);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (userAll == null) {
				badIdSet.add(userId);
				continue;
			}
			// 此处获得爬取结果，转换后存入数据库
			System.out.println("adding " + userAll.userInfo.userID + " to full-info database...");
			WeiboUtils.updateUserAll2DB(conn, userAll);
			int newUserNum = 0, newWeiboNum = 0;
			// 保存人物基本信息
			if (userAll.userInfo != null) {
				User user = WeiboUtils.userInfo2User(userAll.userInfo);
				if (user != null) {
					try {
						if (WeiboUtils.addWeiboUser(conn, user)) {
							newUserNum++;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			// 保存微博到数据库
			if (userAll.weiboList != null) {
				for (Weibo weibo : userAll.weiboList) {
					Message message = WeiboUtils.weibo2Message(weibo);
					if (message != null) {
						try {
							if (WeiboUtils.addWeiboMessage(conn, message)) {
								newWeiboNum++;
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}

			System.out.println("[database] new user: " + newUserNum + ", new weibo: " + newWeiboNum);
			System.out.println("[seeds] current seeds: " + seeds.size() + "\n");

			// 将关注、粉丝中的高影响力用户加入队列
			List<Friend> friends = new ArrayList<>(userAll.fansList);
			if (userAll.followList != null) {
				friends.removeAll(userAll.followList); // requireNonNull
				friends.addAll(userAll.followList);
			}
			for (Friend friend : friends) {
				if (friend.fansNum > fansNumThresholdOfVIP && (!badIdSet.contains(friend.id))) {
					seeds.addLast(friend.id); // 加入爬取队列
				}
			}
		}

		System.out.println("seeds empty, crawl done!");
		DriverUtils.closeDrivers(drivers);
		SqlUtils.closeConnection(conn);
	} // end of method



	/**
	 * 主函数
	 * @param args
	 */
	public static void main(String[] args) {
		// 创建爬取线程与其监听对象
		CrawlKeyUsers2DB crawlThread = new CrawlKeyUsers2DB();
		crawlThread.addObserver(new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				System.out.println("thread dead: " + arg);
				try {
					Thread.sleep(5000L);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				CrawlKeyUsers2DB newCrawlThread = new CrawlKeyUsers2DB();
				newCrawlThread.addObserver(this);
				new Thread(newCrawlThread).start();
				System.out.println("thread restarted");
			}
		});

		// 开始运行
		new Thread(crawlThread).start();
	}

}
