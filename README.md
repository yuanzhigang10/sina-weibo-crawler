# 新浪微博爬虫工具 - SinaWeiboCrawler

更新：似乎这个模拟登录的方法已经被限制了。。。


## SinaWeiboCrawler基本功能
提供了爬取新浪微博用户的基本信息、微博等内容的基本框架。
所有爬取均基于[@新浪微博移动端](http://weibo.cn/)实现。

目前主要实现的功能有：
> * 爬取指定用户的基本信息
> * 爬取指定用户的微博
    * 包含微博、评论、转发、赞等信息
> * 爬取指定用户的粉丝、关注用户
> * 模拟微博搜索，爬取与指定话题相关的微博
> * 基于上述内容的二次任务定制

## SinaWeiboCrawler程序说明
SinaWeiboCrawler基于Java实现，使用的库主要包括`selenium`（网页爬取）、 `jsoup`（网页解析）等。
其中，各个package说明如下：
> * ngn.yzg.sinaweibocrawler.control
    * 程序主控，设置一些全局的参数，如是否debug模式、爬取访问间隔时间等。
> * ngn.yzg.sinaweibocrawler.crawler
    * 爬取核心类，包含各个元模块的爬取实现，例如WeiboCrawler、TopicCrawler等。
    * 其他二次任务可以基于crawler包内的各个类实现进一步定制。
> * ngn.yzg.sinaweibocrawler.demo
    * 所有的示例程序，演示了crawler包内的各个类怎样调用。
> * ngn.yzg.sinaweibocrawler.entity
    * 各种实体类，如Weibo、UserInfo等。
> * ngn.yzg.sinaweibocrawler.task
    * 二次爬取任务定制。
    * 例如，CrawlUserIdList给出了批量爬取指定用户所有信息的实现样例。
> * ngn.yzg.sinaweibocrawler.util
    * 各种工具类，包括爬取工具类、Json工具类等。

各个类的详细功能与使用说明，请参考本工程`javadoc`文档。

## 使用示例
### 微博爬取（摘自于ngn.yzg.sinaweibocrawler.demo.DemoWeibo）

```java
public class DemoWeibo {
    public static void main(String[] args) {
		// 全局参数设置
		// 是否debug输出模式，不指定时默认false
		Console.debugMode = true;
		// 单次访问后的等待时间，单位ms，不指定时默认2500ms
		Console.waitTime = 2500L;
		// 被封号后的等待时间，单位ms，不指定时默认3600_000ms，即1小时
		Console.waitTimeIfBanned = 3600_000L;
		// 用于登录的微博账号。密码明文保存，请使用小号！
		WeiboAccount account = new WeiboAccount("*****@sina.com", "*****");

		// 根据userId爬取用户Weibo，需要创建长度至少为2的HtmlUnitDriver数组
		HtmlUnitDriver[] drivers = DriverUtils.createUsableDrivers(account, 2);

		// 配置各个对象爬取页数的信息，不指定默认全部爬取，0代表不爬取
		HashMap<EntityType, Integer> configs = new HashMap<>();
		// 只爬取1页微博
		configs.put(EntityType.WEIBO, 1);
		// 每条微博爬取2页评论
		configs.put(EntityType.COMMENT, 2);
		// 不添加下面这行代码，代表每条微博的REPOST全部爬取
		// configs.put(EntityType.REPOST, 1);
		// 不爬取微博的点赞信息
		configs.put(EntityType.ATTITUDE, 0);

		// 调用WeiboCrawler完成微博爬取
		List<Weibo> weibos = new WeiboCrawler().crawlByUserId("1635563814", drivers, configs);

		// 输出结果
		System.out.println(JsonUtils.toJson(weibos));
	}
}
```

其他demo均可以通过`ngn.yzg.sinaweibocrawler.demo`内的各个Demo类查看。
