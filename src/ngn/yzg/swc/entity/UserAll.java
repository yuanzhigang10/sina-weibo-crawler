package ngn.yzg.swc.entity;

import java.util.List;

/**
 * 用户完整信息类。
 * 
 * <p>
 * 组合了
 * {@link ngn.yzg.swc.entity.UserInfo UserInfo}，
 * {@link ngn.yzg.swc.entity.Weibo Weibo}和
 * {@link ngn.yzg.swc.entity.Friend Friend}的信息，构成一个完整的<tt>User</tt>信息。
 * 
 * @author yzg
 */
public class UserAll {

	/**
	 * 用户基本信息
	 */
	public UserInfo userInfo;
	
	/**
	 * 用户的微博列表
	 */
	public List<Weibo> weiboList;
	
	/**
	 * 用户的粉丝列表
	 */
	public List<Friend> fansList;
	
	/**
	 * 用户的关注列表
	 */
	public List<Friend> followList;
	
}
