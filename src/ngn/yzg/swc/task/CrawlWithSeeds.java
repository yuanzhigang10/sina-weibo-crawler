package ngn.yzg.swc.task;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ngn.yzg.swc.control.Console;
import ngn.yzg.swc.crawler.IdCrawler;
import ngn.yzg.swc.crawler.UserAllCrawler;
import ngn.yzg.swc.entity.CrawlerException;
import ngn.yzg.swc.entity.EntityType;
import ngn.yzg.swc.entity.Friend;
import ngn.yzg.swc.entity.UserAll;
import ngn.yzg.swc.entity.WeiboAccount;
import ngn.yzg.swc.util.DriverUtils;
import ngn.yzg.swc.util.JsonUtils;
import ngn.yzg.swc.util.Utils;

import org.openqa.selenium.htmlunit.HtmlUnitDriver;

/**
 * 一个定制的任务：根据根据选取的若干账号作为种子，爬取其粉丝、关注，再爬取粉丝关注中影响力较高的账号。
 * 
 * <p>
 * 通过调用{@link ngn.yzg.swc.crawler.UserAllCrawler UserAllCrawler}实现所有人的爬取。<br>
 * 结果以<tt>json</tt>格式存储，每个账号的数据对应一个文件。爬取前需要配置全局参数。
 * 
 * @author PeterYuan
 *
 */
public class CrawlWithSeeds extends Observable implements Runnable {
	private static boolean serverMode = false;

	private static String path = serverMode ? "." : "D:/data/weiborelation/";
	private static String username = "yuanzhigang10@163.com";
	private static String password = "********";

	private static HashMap<String, String> userMap = new HashMap<>(); // 程序运行中数据库
	private static ArrayDeque<String> seeds = new ArrayDeque<String>(); // 待爬取队列
	private static HashSet<String> crawledSet = new HashSet<>(); // 已经爬取的账号

	private static int fansNumThresholdOfVIP = 10000;


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
			System.err.println("an unknown exception from CrawlWithSeeds, calling listener to restart...");
			// 通知观察者
			super.setChanged();
			notifyObservers("CrawlWithSeeds");
		}
	}


	/**
	 * 根据输出文件夹内的文件，更新已经爬取的用户集合
	 * @param savepath 输出文件夹的路径
	 */
	private static void updateCrawledSet(String savepath) {
		File[] files = new File(savepath).listFiles();
		for (File file : files) {
			String filename = file.getAbsolutePath();
			Matcher m = Pattern.compile("\\d{10}\\.json").matcher(filename);
			if (m.find()) {
				crawledSet.add(m.group().replaceAll(".json", ""));
			}
		}
	}


	/**
	 * 程序运行主体
	 */
	@SuppressWarnings("deprecation")
	private void runBody() throws CrawlerException {
		System.out.println("===== run CrawlWithSeeds =====");
		// 配置全局参数
		String savepath = path + File.separator + "users";
		Console.debugMode = true; // 是否debug输出模式，不指定时默认false
		Console.waitTime = 8*1000L; // 单次访问后的等待时间，8s
		Console.waitTimeIfBanned = 60*60*1000L; // 被封号后的等待时间，1小时
		WeiboAccount account = new WeiboAccount(username, password);
		File dir = new File(savepath);
		if (!dir.exists()) {
			dir.mkdir();
			System.out.println(savepath + " created");
		}

		// 配置爬取参数
		HtmlUnitDriver[] drivers = DriverUtils.createUsableDrivers(account, 2);
		HashMap<EntityType, Integer> configs = new HashMap<>();
		configs.put(EntityType.WEIBO, 50);
		configs.put(EntityType.COMMENT, 0);
		configs.put(EntityType.REPOST, 0);
		configs.put(EntityType.ATTITUDE, 0);
		//configs.put(EntityType.FOLLOW, 1); // 不添加表示最大
		//configs.put(EntityType.FANS, 1);
		configs.put(EntityType.DeepParseUserId, 1); // 要深度解析转发源

		// 从文件中读取seeds数据，先加入数据库
		try {
			BufferedReader br = Utils.openFileBR(path + File.separator + "seeds.txt");
			String line = null;
			while ((line=br.readLine()) != null) {
				String[] fields = line.split("\t");
				if (fields.length == 2)
					userMap.put(fields[0], fields[1]);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// 用数据库建立爬取种子
		for (String id : userMap.keySet()) {
			seeds.addLast(id); // 将种子加入爬取队列
		}
		System.out.println("initial seeds size: " + seeds.size());

		// 开始爬取
		while (!seeds.isEmpty()) { // 队列只要非空就继续爬取
			System.out.println("\n[" + new Date().toLocaleString() + "]");
			updateCrawledSet(savepath);
			System.out.println("crawled num: " + crawledSet.size());
			
			// 从队列头部取出待爬取id，开始新的爬取
			String userId = seeds.removeFirst();
			if (!crawledSet.contains(userId)) { // 判断是否已经爬取
				System.out.println("start crawling: "+ userId + " (" + userMap.get(userId) +  ")");
				IdCrawler.clearDomainMap(); // 查询domainMap只对单个用户的朋友圈有意义，因此爬取前可以清空
				UserAll userAll = new UserAllCrawler().crawlByUserId(userId, drivers, configs);
				crawledSet.add(userId);
				if (userAll == null) {
					System.out.println("cannot access user " + userId);
					continue;
				} else {
					// 保存该用户数据
					PrintStream ps = Utils.openFilePS(savepath + File.separator + userId + ".json");
					ps.println(JsonUtils.toJson(userAll));
					ps.close();
					System.out.println(userId + " data saved");
					// 将关注、粉丝中的高影响力用户加入队列
					List<Friend> friends = new ArrayList<>(userAll.fansList);
					if (userAll.followList != null) {
						friends.removeAll(userAll.followList); // requireNonNull
						friends.addAll(userAll.followList);
					}
					int newAdded = 0;
					for (Friend friend : friends) {
						if (friend.fansNum > fansNumThresholdOfVIP) {
							seeds.addLast(new Long(friend.id).toString()); // 加入爬取队列
							userMap.put(new Long(friend.id).toString(), friend.name); // 加入数据库
							newAdded++;
						}
					}
					System.out.println("add " + newAdded + " new id(s) into seeds\n"
							+ "userMap size: " + userMap.size() + "\n"
							+ "seeds size: " + seeds.size() + "\n");
				}
			} // 该用户爬取完毕
			// 保存当前队列，以便后续可能的断点爬取
			PrintStream psSeeds = Utils.openFilePS(path + File.separator + "seeds.txt");
			if (psSeeds != null) {
				for (String id : seeds) {
					psSeeds.println(id + "\t" + userMap.get(id));
				}
				psSeeds.close();
			}
		} // end of iteration
		System.out.println("\n===== all done =====");
		DriverUtils.closeDrivers(drivers);
	} // end of method


	/**
	 * 主函数
	 * @param args
	 */
	public static void main(String[] args) {
		// 创建爬取线程与其监听对象
		CrawlWithSeeds crawlThread = new CrawlWithSeeds();
		crawlThread.addObserver(new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				System.out.println("thread dead: " + arg);
				CrawlWithSeeds newCrawlThread = new CrawlWithSeeds();
				newCrawlThread.addObserver(this);
				new Thread(newCrawlThread).start();
				System.out.println("thread restarted");
			}
		});

		// 开始运行
		new Thread(crawlThread).start();
	}

}
