package ngn.yzg.swc.entity;

/**
 * 实体类型。
 * 
 * <p>
 * 主要包括两类。<br>
 * 第一类指代微博中的各种实体，如微博、评论、转发、赞、粉丝、关注等。<br>
 * 第二类是用于配置参数的一些实体，如热度排序、深度解析<tt>UserId</tt>等。
 * 
 * @author yzg
 * @since 1.7
 *
 */
public enum EntityType {
	WEIBO, COMMENT, REPOST, ATTITUDE, FANS, FOLLOW,
	
	HotMode, DeepParseUserId
}
