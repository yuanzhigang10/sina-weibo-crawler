package ngn.yzg.swc.util;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import ngn.yzg.swc.control.Console;
import ngn.yzg.swc.entity.CrawlerException;
import ngn.yzg.swc.entity.WeiboAccount;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

/**
 * {@code driver}工具类。
 * 
 * <p>
 * 该类提供了一系列服务于{@code HtmlUnitDriver}的工具函数。
 * 
 * @author yzg
 *
 */
public final class DriverUtils {
	
	private DriverUtils() {}
	
	/**
	 * 模拟微博登录，以获取新浪微博<tt>cookie</tt>。
	 * @param account 微博账号类对象。
	 * @param driver 用于模拟登录的{@code driver}对象。
	 * @throws InterruptedException 
	 */
	public static void weiboLogin(WeiboAccount account, HtmlUnitDriver driver) throws CrawlerException {
		driver.setJavascriptEnabled(true);
		driver.get("http://login.weibo.cn/login/");
		try {
			WebElement username = driver.findElement(By.name("mobile"));
			username.sendKeys(account.getUsername());
			WebElement password = driver.findElementByCssSelector("input[name^=password]");
			password.sendKeys(account.getPassword());
			WebElement remember = driver.findElement(By.name("remember"));
			remember.click();
			WebElement submit = driver.findElement(By.name("submit"));
			submit.click();
		} catch (IllegalStateException ise) {
			System.err.println("网络连接异常！");
			try {
				Thread.sleep(10*60*1000L); // 网络连接异常时，等待10分钟，再尝试重新启动程序
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			throw new CrawlerException("An exception occurred, try to exit...");
		}
		
		try { // 休眠
			Thread.sleep(Console.waitTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		StringBuilder sb = new StringBuilder();
		Set<Cookie> cookies = driver.manage().getCookies();
		for (Cookie cookie : cookies) {
			sb.append(new StringBuilder().append(cookie.getName()).append("=").append(cookie.getValue()).append(";").toString());
		}
		String result = sb.toString();
		if (result.contains("gsid_CTandWM")) {
			if (Console.debugMode) { System.out.println("weibo login done, account = " + account.getUsername()); }
		} else {
			System.err.println("weibo login failed");
			throw new CrawlerException("An exception occurred, try to exit...");
		}
	}
	
	
	/**
	 * 用{@code WeiboAccount}登录微博，获取含有<tt>cookie</tt>的{code driver}爬取对象。
	 * @param account 微博账号。
	 * @param num 需要创建的{@code driver}对象的个数。
	 * @return 创建的{@code driver}对象数组。
	 */
	public static HtmlUnitDriver[] createUsableDrivers(WeiboAccount account, int num) throws CrawlerException {
		HtmlUnitDriver[] drivers = new HtmlUnitDriver[num];
		for (int i = 0; i < num; i++) {
			drivers[i] = new HtmlUnitDriver(true); // 允许执行Javascript代码
			drivers[i].manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS); // 最多等待响应时间10s
			DriverUtils.weiboLogin(account, drivers[i]); // 登录账号
		}
		if (Console.debugMode) { System.out.println("created " + num + " driver(s)"); }
		return drivers;
	}


	/**
	 * 关闭{@code drivers}对象数组。
	 * @param drivers 待关闭的{@code drivers}对象数组。
	 */
	public static void closeDrivers(HtmlUnitDriver[] drivers) {
		for (HtmlUnitDriver driver : drivers) {
			driver.close();
		}
	}
	
	/**
	 * 对{@code driver}的{@code get}函数进行封装，处理账号被封的情况。
	 * 如果账号被封，则长期休眠后再次尝试。
	 * @param driver 被封装的{@code driver}对象。
	 * @param url 待访问的网址。
	 */
	public static void safeGet(HtmlUnitDriver driver, String url) {
		driver.get(url);
		try {
			Thread.sleep(Console.waitTime);
		} catch (InterruptedException e1) {
			System.err.println("safeGet: Thread.sleep fail");
			e1.printStackTrace();
		}
		if (driver.getCurrentUrl().contains("pub") && !url.contains("pub")) {
			// 可能账号已经被封，再次访问某账户，确认账号状态
			driver.get("http://weibo.cn/yuanzhigang10");
			if (driver.getCurrentUrl().contains("pub")) {
				// 又回到pub界面，说明账号确实被封，等一小时后再次爬取
				try {
					System.err.println("用于爬取的账号已经被封！将于" + (double)Console.waitTimeIfBanned/60_000 + "分钟后再次尝试访问。");
					Thread.sleep(Console.waitTimeIfBanned);
					// 休眠结束，再次访问
					safeGet(driver, url);
				} catch (InterruptedException e2) {
					System.err.println("safeGet: Thread.sleep fail");
					e2.printStackTrace();
				}
			}
		}
		
	}
	
	
	/**
	 * 对{@code WebElement}的{@code click}函数进行封装，加入等待。
	 * @param we 被封装的{@code WebElement}对象。
	 */
	public static void safeClick(WebElement we) {
		we.click();
		try {
			Thread.sleep(Console.waitTime);
		} catch (InterruptedException e) {
			System.err.println("safeClick: Thread.sleep fail");
		}
	}
	
	
}
