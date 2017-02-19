package ngn.yzg.swc.entity;

import java.util.Date;

/**
 * 用户基本信息类。
 * 
 * @author yzg
 *
 */
public class UserInfo {

	public Long userID = 0L; //must
	public String name = ""; //must
	public String screenName = "";
	
	public int province = 0;
	public int city = 0;
	public String location = ""; //must
	
	public String description = ""; //must
	public String url = "";
	public String userDomain = "";
	public String gender = ""; //must
	public int followersCount = 0; // fans
	public int friendsCount = 0; // follow
	public int statusesCount = 0; // weibo
	public int favouritesCount = 0; 
	@SuppressWarnings("deprecation")
	public Date createdTime = new Date(2000-1900, 0, 1, 0, 0, 0); //must
	public boolean verified = false; //must
	public int verifiedType = 0; 
	public boolean allowAllActMsg = false;
	public boolean allowAllComment = false;
	public int biFollowersCount = 0;
	public String verifiedReason = ""; //must
	public String imagelink = ""; //must


	public UserInfo() {}

}
