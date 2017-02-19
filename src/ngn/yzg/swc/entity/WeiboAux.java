package ngn.yzg.swc.entity;

import java.util.Date;

/**
 * 代表某条微博的“评论/转发/赞”的类。
 * 
 * <p>
 * 该类一般不单独使用，主要服务于{@link ngn.yzg.swc.crawler.WeiboCrawler WeiboCrawler}
 * 等需要爬取微博（<tt>Weibo</tt>）的类的实现。
 * 
 * @author yzg
 *
 */
public class WeiboAux {
	public EntityType weiboAuxType;
	
	// comment, repost, attitude
	public String userName = "";
	public String userUrl = "";
	public long userId = 0;
	
	public String text = "";
	public Date createdTime = new Date();
	
	// comment, repost
	public int likeNum = 0;
	
	// comment
	public String replyName = "";
	public String replyUrl = "";
	public long replyId = 0;
	
}
