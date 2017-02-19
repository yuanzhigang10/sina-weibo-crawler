package ngn.yzg.swc.task;

import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;

import ngn.yzg.swc.control.Console;
import ngn.yzg.swc.crawler.UserAllCrawler;
import ngn.yzg.swc.entity.CrawlerException;
import ngn.yzg.swc.entity.EntityType;
import ngn.yzg.swc.entity.UserAll;
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
 * 一个定制的任务：根据一系列<tt>UserId</tt>，爬取所有人的信息。
 * 
 * <p>
 * 通过调用{@link ngn.yzg.swc.crawler.UserAllCrawler UserAllCrawler}实现所有人的爬取。<br>
 * 结果以<tt>json</tt>格式存储，爬取前需要配置全局参数。
 * 
 * @author yzg
 *
 */
public class CrawlUserIdList {
	public static String savepath;
	public static String username;
	public static String password;
	public static String useridlistfile;
	
	/**
	 * 解析命令行参数
	 * @param args 命令行参数
	 */
	public static void parseCommandLine(String[] args) {
		String clazzName = Thread.currentThread().getStackTrace()[1].getClassName();
		
		CommandLineParser parser = new DefaultParser();
		Options options = new Options( );
		// 添加需要解析的命令
		options.addOption("s", "savepath", true, "The save path for output files.");
		options.addOption("u", "username", true, "Username of Weibo account to login.");
		options.addOption("p", "password", true, "Password of Weibo account to login.");
		options.addOption("f", "useridlistfile", true, "The useridlistfile to be crawled.");
		// 解析相应的命令行参数
		try {
			// 解析命令行参数
			CommandLine commandLine = parser.parse(options, args);
			// 获取需要的参数
			if (commandLine != null) {
				savepath = commandLine.getOptionValue("savepath", "");
				username = commandLine.getOptionValue("username", "");
				password = commandLine.getOptionValue("password", "");
				useridlistfile = commandLine.getOptionValue("useridlistfile", "");
			}
			// 参数检查
			if ("".equals(savepath) || "".equals(username) || "".equals(password) || "".equals(useridlistfile)) {
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
	public static void runBody(String[] args) throws CrawlerException {
		// 配置全局参数
		Console.debugMode = true; // 是否debug输出模式，不指定时默认false
		Console.waitTime = 2500L; // 单次访问后的等待时间，单位ms，不指定时默认2500ms
		Console.waitTimeIfBanned = 3600_000L; // 被封号后的等待时间，单位ms，不指定时默认3600_000ms，即1小时
		WeiboAccount account = new WeiboAccount("yuanzhigang10@126.com", "********");
		File dir = new File(savepath);
		if (!dir.exists()) {
			dir.mkdir();
			System.out.println("\"" + savepath + "\" created");
		}

		// 配置爬取参数
		HtmlUnitDriver[] drivers = DriverUtils.createUsableDrivers(account, 2);
		HashMap<EntityType, Integer> configs = new HashMap<>();
		configs.put(EntityType.WEIBO, 200);
		//configs.put(EntityType.COMMENT, 100);
		//configs.put(EntityType.REPOST, 100);
		//configs.put(EntityType.ATTITUDE, 100);
		//configs.put(EntityType.FOLLOW, 5);
		//configs.put(EntityType.FANS, 5);

		int successCount = 0;
		// 开始爬取
		System.out.println("run CrawlUserIdList");
		List<String> userIdList = Utils.getUserIdListFromFile(useridlistfile);
		for (String userId : userIdList) {
			System.out.println("\n\n===== 开始爬取用户 =====");
			System.out.println("userId: " + userId + ", account: " + account.getUsername());
			UserAll userAll = new UserAllCrawler().crawlByUserId(userId, drivers, configs);
			if (userAll == null) {
				System.out.println("用户" + userId + "无法访问！");
			} else {
				PrintStream ps = Utils.openFilePS(savepath + File.separator + userId + ".json");
				ps.println(JsonUtils.toJson(userAll));
				ps.close();
				successCount++;
				System.out.println(userId + ".json写入完毕，已爬取了" + successCount + "个用户。");
			}
		}
		System.out.println("\n\n===== 所有用户已经爬取完毕！ =====");
	}

}
