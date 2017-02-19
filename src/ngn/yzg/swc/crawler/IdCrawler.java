package ngn.yzg.swc.crawler;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import ngn.yzg.swc.util.DriverUtils;

/**
 * 爬取用户的<tt>UserId</tt>。
 * 
 * <p>
 * 通过用户主页的<tt>url</tt>，寻找用户基本信息，解析出<tt>UserId</tt>。<br>
 * 该类很少单独使用，主要被其他类调用，完成<tt>UserId</tt>的获取。<br>
 * 通过类函数{@code crawlByUserUrl}调用，完成爬取过程。
 * 
 * @author PeterYuan
 *
 */
public class IdCrawler {
	private static boolean debugMode = false;

	private static HashMap<String, String> domainMap = new HashMap<>();
	public static void clearDomainMap() {
		domainMap.clear();
	}

	/**
	 * 根据用户的<tt>url</tt>，登录其主页，获取该用户<tt>UserId</tt>。
	 * @param userUrl 待爬取用户的<tt>url</tt>，注意必须是主页，形如“http://weibo.cn/*****”的形式。
	 * @param driver 用于爬取用户主页的{@code driver}对象。
	 * @return 解析出的用户<tt>UserId</tt>，如果没有成功解析，默认返回0（{@code String}形式）。
	 */
	public String crawlByUserUrl(String userUrl, HtmlUnitDriver driver) {
		// 处理userUrl
		if (userUrl.startsWith("/")) {
			userUrl = "http://weibo.cn" + userUrl;
		}
		String domain = null;
		Matcher m = Pattern.compile("cn/.+\\?vt=4").matcher(userUrl);
		if (m.find()) {
			domain =  m.group().replace("cn/", "").replace("?vt=4", "");
		}

		// 先查询domainMap库中是否有记录，有的话直接读取，无需再爬取
		if (domain != null) {
			if (domainMap.containsKey(domain)) {
				if (debugMode) { System.out.println(domain + " --> " + domainMap.get(domain)); }
				return domainMap.get(domain);
			}
		}

		// 没有查询到，需要爬取
		DriverUtils.safeGet(driver, userUrl);
		Document doc = Jsoup.parse(driver.getPageSource());
		String text = "0";
		Elements fields = doc.select("div.ut");
		if (!fields.isEmpty()) {
			for (Element field : fields) {
				Elements links = field.select("a[href]");
				if (!links.isEmpty()) {
					for (Element link : links) {
						if (link.text().equals("资料")) {
							text = link.attr("href");
						}
					}
				}
			}
		}
		m = Pattern.compile("/\\d{10}/").matcher(text);
		if (m.find()) { // 通过爬取找到了10位的userId
			String userId = m.group().replaceAll("/", ""); // /3272550787/ --> 3272550787
			if (domain != null) {
				domainMap.put(domain, userId);
			}
			return userId;
		} else {
			System.err.println(this.getClass().getName() + ": fail, use \"0\" instead");
			return "0";
		}
	}

	public static void main(String[] args) {
		Matcher m = Pattern.compile("cn/.+\\?vt=4").matcher("http://weibo.cn/yuanzhigang10?vt=4");
		if (m.find()) {
			System.out.println(m.group().replace("cn/", "").replace("?vt=4", ""));
		}
	}

}
