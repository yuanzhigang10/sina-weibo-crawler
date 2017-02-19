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
import ngn.yzg.swc.crawler.FriendCrawler;
import ngn.yzg.swc.crawler.UserInfoCrawler;
import ngn.yzg.swc.entity.CrawlerException;
import ngn.yzg.swc.entity.EntityType;
import ngn.yzg.swc.entity.Friend;
import ngn.yzg.swc.entity.UserInfo;
import ngn.yzg.swc.entity.WeiboAccount;
import ngn.yzg.swc.util.DriverUtils;
import ngn.yzg.swc.util.JsonUtils;
import ngn.yzg.swc.util.Utils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

/**
 * 一个定制的任务：根据根据选取的若干账号作为种子，爬取其基本信息，再爬取粉丝关注中影响力较高的账号。
 * 如此递归爬取用户基本信息库。
 * 
 * <p>
 * 通过调用{@link ngn.yzg.swc.crawler.UserInfoCrawler UserInfoCrawler}实现所有人的爬取。<br>
 * 结果以<tt>json</tt>格式存储，每个用户信息的的数据对应一个文件。爬取前需要配置全局参数。
 * 
 * @author PeterYuan
 *
 */
public class CrawlBasicInfos extends Observable implements Runnable {
	public static int createdTimes;
	public CrawlBasicInfos() {
		createdTimes++;
	}

	public static String savepath;
	public static String username;
	public static String password;


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
			System.err.println("an unknown exception from CrawlBasicInfos, calling listener to restart...");
			// 通知观察者
			super.setChanged();
			notifyObservers("CrawlBasicInfos");
		}
	}



	/**
	 * 解析命令行参数
	 * @param args 命令行参数
	 */
	public void parseCommandLine(String[] args) {
		String clazzName = Thread.currentThread().getStackTrace()[1].getClassName();

		CommandLineParser parser = new DefaultParser();
		Options options = new Options( );
		// 添加需要解析的命令
		options.addOption("s", "savepath", true, "The save path for output files.");
		options.addOption("u", "username", true, "Username of Weibo account to login.");
		options.addOption("p", "password", true, "Password of Weibo account to login.");
		// 解析相应的命令行参数
		try {
			// 解析命令行参数
			CommandLine commandLine = parser.parse(options, args);
			// 获取需要的参数
			if (commandLine != null) {
				savepath = commandLine.getOptionValue("savepath", "");
				username = commandLine.getOptionValue("username", "");
				password = commandLine.getOptionValue("password", "");
			}
			// 参数检查
			if ("".equals(savepath) || "".equals(username) || "".equals(password)) {
				System.err.println("invalid command line parameters for "+ clazzName +".\n"
						+ "Please restart.\n");
				System.exit(0);
			} else {
				System.out.println("valid parameters for " + clazzName);
			}
		} catch (ParseException e) {
			System.err.println( "Parsing failed.  Reason: " + e.getMessage() );
		}
	}

	/**
	 * 程序运行主体
	 */
	public void runBody() throws CrawlerException {
		System.out.println("===== run CrawlBasicInfos =====\n");
		// 配置全局参数
		Console.debugMode = true; // 是否debug输出模式，不指定时默认false
		Console.waitTime = 10*1000L; // 单次访问后的等待时间，10s
		Console.waitTimeIfBanned = 60*60*1000L; // 被封号后的等待时间，1小时
		WeiboAccount account = new WeiboAccount(username, password);
		File dir = new File(savepath);
		if (!dir.exists()) {
			dir.mkdir();
			System.out.println("\"" + savepath + "\" created");
		}

		// 配置爬取参数
		HtmlUnitDriver driver = DriverUtils.createUsableDrivers(account, 1)[0];
		HashMap<String, String> data = new HashMap<>(); // 程序运行中数据库（内存）
		ArrayDeque<String> seeds = new ArrayDeque<String>(); // 待爬取队列
		HashSet<String> crawled = new HashSet<>(); // 已经爬取的账号
		// 从文件中读取crawled数据
		File[] files = new File(savepath).listFiles();
		for (File file : files) {
			String filename = file.getAbsolutePath();
			Matcher m = Pattern.compile("\\d{10}\\.json").matcher(filename);
			if (m.find()) {
				crawled.add(m.group().replaceAll(".json", ""));
			}
		}
		System.out.println("已爬取的账号数目：" + crawled.size());
		// 从文件中读取seeds数据，先加入数据库
		BufferedReader br = Utils.openFileBR(savepath + File.separator + "seeds.txt");
		if (br != null) {
			String line = null;
			try {
				while ((line=br.readLine()) != null) {
					String[] fields = line.split("\t");
					if (fields.length == 2) {
						data.put(fields[0], fields[1]);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		// 用数据库建立爬取种子
		for (String id : data.keySet()) {
			seeds.addLast(id); // 将种子加入爬取队列
		}
		System.out.println("初始种子账号数目：" + seeds.size() + "\n");


		// 开始爬取
		int successCount = 0;
		while (!seeds.isEmpty()) { // 队列只要非空就继续爬取
			String userId = seeds.removeFirst(); // 队列头部取出待爬取id
			if (!crawled.contains(userId)) { // 判断是否已经爬取
				System.out.println("开始爬取账号："+ userId + "（" + data.get(userId) +  "），使用" + account.getUsername());
				UserInfo userInfo = new UserInfoCrawler().crawlByUserId(userId, driver);
				crawled.add(userId);
				if (userInfo == null) {
					System.out.println("用户" + userId + "无法访问！\n");
				} else {
					// 保存该用户数据
					PrintStream ps = Utils.openFilePS(savepath + File.separator + userId + ".json");
					ps.println(JsonUtils.toJson(userInfo));
					ps.close();
					successCount++;
					System.out.println(userId + ".json写入完毕。（" + userInfo.name + "）\n"
							+ new Date().toString() + "，已爬取了" + successCount + "个用户。");
					// 当种子数目较少时，开始爬取用户的粉丝、关注，以实现递归爬取
					System.out.println("SeedsSize: " + seeds.size() + "\n");
					if (seeds.size() < 200) {
						// 爬取该用户粉丝、关注信息
						List<Friend> followList = new FriendCrawler().crawlByUserId(userId, driver, EntityType.FOLLOW, Integer.MAX_VALUE);
						List<Friend> fansList = new FriendCrawler().crawlByUserId(userId, driver, EntityType.FANS, Integer.MAX_VALUE);
						// 将关注、粉丝中的高影响力用户加入队列
						List<Friend> friends = new ArrayList<>();
						friends.addAll(fansList);
						if (followList != null) {
							friends.removeAll(followList); // requireNonNull
							friends.addAll(followList);
						}
						int newAdded = 0;
						for (Friend friend : friends) {
							if (friend.fansNum > 1000) {
								seeds.addLast(new Long(friend.id).toString()); // 加入爬取队列
								data.put(new Long(friend.id).toString(), friend.name); // 加入数据库
								newAdded++;
							}
						}
						System.out.println("新添加了" + newAdded + "个账号至爬取队列。\n"
								+ "DataSize: " + data.size() + "\n");
					}
				}
			} // 该用户爬取完毕
			// 保存当前队列，以便后续可能的断点爬取
			PrintStream psSeeds = Utils.openFilePS(savepath + File.separator + "seeds.txt");
			if (psSeeds != null) {
				for (String id : seeds) {
					psSeeds.println(id + "\t" + data.get(id));
				}
				psSeeds.close();
			}
		} // end of iteration
		System.out.println("\n===== 全部爬取完毕 =====\n");
		driver.close();
	} // end of method


	/**
	 * 主函数
	 * @param args
	 */
	public static void main(String[] args) {
		// 创建爬取线程与其监听对象
		CrawlBasicInfos crawlThread = new CrawlBasicInfos();
		crawlThread.addObserver(new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				System.out.println("thread dead: " + arg);
				CrawlBasicInfos newCrawlThread = new CrawlBasicInfos();
				newCrawlThread.addObserver(this);
				new Thread(newCrawlThread).start();
				System.out.println("thread restarted");
			}
		});

		// 解析命令行参数
		crawlThread.parseCommandLine(args);

		// 开始运行
		new Thread(crawlThread).start();
	}

}
