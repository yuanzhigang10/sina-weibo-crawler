package ngn.yzg.swc.service;

import java.util.Date;

public class User {

	public Long userID = 0l; //must
	public String name = ""; //must
	public String screenName = "";
	public int province = 0;
	public int city = 0;
	public String location = ""; //must
	public String description = ""; //must
	public String url = "";
	public String userDomain = "";
	public String gender = ""; //must
	public int followersCount = 0; //must
	public int friendsCount = 0; //must
	public int statusesCount = 0; //must
	public int favouritesCount = 0; 
	public Date createdTime = new Date(); //must
	public boolean verified = false; //must
	public int verifiedType = 0; 
	public boolean allowAllActMsg = false;
	public boolean allowAllComment = false;
	public int biFollowersCount = 0;
	public String verifiedReason = ""; //must
	public String imagelink = ""; //must


}
