package ngn.yzg.swc.task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.openqa.selenium.htmlunit.HtmlUnitDriver;





import com.google.gson.Gson;

import ngn.yzg.swc.control.Console;
import ngn.yzg.swc.control.RunConfig;
import ngn.yzg.swc.crawler.WeiboCrawler;
import ngn.yzg.swc.entity.CrawlerException;
import ngn.yzg.swc.entity.EntityType;
import ngn.yzg.swc.entity.Weibo;
import ngn.yzg.swc.entity.WeiboAccount;
import ngn.yzg.swc.util.DriverUtils;
import ngn.yzg.swc.util.SqlUtils;

public class CrawlWeibo {

	public static Connection connection;
	public static String password = RunConfig.serverMode ? "***" : "***"; // TODO

	public static WeiboAccount account;
	public static HtmlUnitDriver[] drivers;
	public static HashMap<EntityType, Integer> configs;

	public static String path = RunConfig.serverMode ? "." : "D:/data/weibocrawler";

	public static ArrayDeque<String> latestedCrawled = new ArrayDeque<String>();
	public static int latestNum = 30;

	/**
	 * 从SQL数据库中找出待爬取的用户
	 * @param connection
	 * @param num
	 * @return
	 */
	public static List<String> getMostPopularIds(Connection connection, int num) {
		List<String> popularIds = new ArrayList<String>();
		String sqlQuery = "SELECT userID,name FROM WeiboUserTopic WHERE name LIKE '%新闻%' OR name LIKE '%网' OR name LIKE '%报' ORDER BY followersCount DESC LIMIT " + num + ";";
		ResultSet results = SqlUtils.select(connection, sqlQuery);
		try {
			while (results.next()) {
				popularIds.add(results.getString(1));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return popularIds;
	}


	/**
	 * 初始化函数s
	 * @throws CrawlerException
	 */
	public static void init() throws CrawlerException {
		connection = SqlUtils.getConnection("Weibo", password);

		account = new WeiboAccount("yuanzhigang10@163.com", "********");
		drivers = DriverUtils.createUsableDrivers(account, 2);
		Console.debugMode = false;
		configs = new HashMap<>();
		configs.put(EntityType.WEIBO, 1);
		configs.put(EntityType.COMMENT, 0);
		configs.put(EntityType.REPOST, 0);
		configs.put(EntityType.ATTITUDE, 0);
		configs.put(EntityType.DeepParseUserId, 0);
	}


	public static void run() {
		try {
			init();
		} catch (CrawlerException e) {
			e.printStackTrace();
		}
		List<String> popularIds = getMostPopularIds(connection, 1000);
		int totalNum = popularIds.size();
		System.out.println(totalNum + " users found in database");
		Random random = new Random();
		Gson gson = new Gson();

		while (true) {
			try {
				int index = random.nextInt(totalNum);
				String userId = popularIds.get(index);
				if (latestedCrawled.contains(userId)) {
					System.out.println("\n" + userId + " has been crawled recently");
					continue;
				}

				System.out.println("\nstart crawling: " + userId);
				List<Weibo> weibos = new WeiboCrawler().crawlByUserId(userId, drivers, configs);
				if (weibos!=null && weibos.size()!=0) {
					latestedCrawled.addLast(userId);
					if (latestedCrawled.size() > 20) {
						latestedCrawled.removeFirst();
					}
					System.out.println("weibos num: " + weibos.size());
					PrintStream ps = new PrintStream(new FileOutputStream(path+File.separator+"weibo.json", true), true, "UTF-8");
					PrintStream psBak = new PrintStream(new FileOutputStream(path+File.separator+"weibo_bak.json", true), true, "UTF-8");
					for (Weibo weibo : weibos) {
						ps.println(gson.toJson(weibo));
						psBak.println(gson.toJson(weibo));
					}
					ps.close();
					psBak.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		run();
	}


}
