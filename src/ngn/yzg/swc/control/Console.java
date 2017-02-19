package ngn.yzg.swc.control;

import java.util.Observable;
import java.util.Observer;

import ngn.yzg.swc.task.CrawlBasicInfos;
import ngn.yzg.swc.task.CrawlUserIdList;
import ngn.yzg.swc.task.CrawlWithSeeds;

/**
 *<tt>SinaWeiboCrawler</tt>主控类。
 *
 *<p>
 * 该类包含了一些主要的全局配置，如是否显示调试输出、爬取等待间隔等参数。<br>
 * 参数均为{@code public static}类型，其他调用类可直接访问修改。如不做修改，所有参数均为默认值。
 * 
 * @author PeterYuan
 */
@SuppressWarnings("unused")
public class Console {

	/**
	 * {@code boolean}变量，代表“是否<tt>debug</tt>模式”。<br>
	 * <tt>debug</tt>模式显示爬取过程<tt>log</tt>；默认{@code false}，即不输出中间过程。
	 */
	public static boolean debugMode = false;

	/**
	 * {@code long}变量，代表“单次访问后的等待时间”，单位毫秒。<br>
	 * 默认8*1000L，即8s，建议不小于此值，减少被封号的风险。
	 */
	public static long waitTime = 8*1000L;

	/**
	 * {@code long}变量，代表“账号被封之后的等待时间”，单位毫秒。<br>
	 * 默认60*60*1000L，即1h。
	 */
	public static long waitTimeIfBanned = 60*60*1000L;
	
	
	
	public static void main(String[] args) {
		System.out.println(Long.MAX_VALUE);
	}

}
