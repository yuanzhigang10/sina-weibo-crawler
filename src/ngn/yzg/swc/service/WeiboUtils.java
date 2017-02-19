package ngn.yzg.swc.service;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import ngn.yzg.swc.entity.Friend;
import ngn.yzg.swc.entity.UserAll;
import ngn.yzg.swc.entity.UserInfo;
import ngn.yzg.swc.entity.Weibo;
import ngn.yzg.swc.util.SqlUtils;
import ngn.yzg.swc.util.Utils;

public class WeiboUtils {
	private static long countID = 0;
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private static String generalProcess(String text) {
		if (text == null)
			return "";
		return SqlUtils.filterStr(text).replaceAll("\n", "").replaceAll("\t", "");
	}

	
	public static void setAnalysisedTrue(Connection conn, Long userId) {
		String updateSql = "UPDATE UserFullInfo SET analysised = 1 WHERE userId = " + userId + ";";
		SqlUtils.conduct(conn, updateSql);
	}
	
	public static boolean isExistedUserInFullInfoById(Connection conn, long id) {
		String querySql = "SELECT COUNT(*) FROM UserFullInfo WHERE userId = " + id + ";";
		ResultSet rs = SqlUtils.select(conn, querySql);
		int count = 0;
		try {
			if (rs.next()) {
				count = rs.getInt(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return (count > 0);
	}
	
	
	public static void updateUserAll2DB(Connection conn, UserAll userAll) {
		if (userAll == null || userAll.userInfo == null || userAll.weiboList == null ||
				userAll.fansList == null || userAll.followList == null) {
			return;
		}

		long userId = userAll.userInfo.userID;

		List<Long> weiboIdList = new ArrayList<>();
		List<Long> fansIdList = new ArrayList<>();
		List<Long> followIdList = new ArrayList<>();

		for (Weibo weibo : userAll.weiboList) {
			weiboIdList.add(weibo.id);
		}
		String weiboIdStr = Utils.list2Str(weiboIdList, "##");
		for (Friend friend : userAll.fansList) {
			fansIdList.add(friend.id);
		}
		String fansIdStr = Utils.list2Str(fansIdList, "##");
		for (Friend friend : userAll.followList) {
			followIdList.add(friend.id);
		}
		String followIdStr = Utils.list2Str(followIdList, "##");

		int weiboNum = weiboIdList.size();
		int fansNum = fansIdList.size();
		int followNum = followIdList.size();
		weiboIdList = null;
		fansIdList = null;
		followIdList = null;

		// sql
		if (!isExistedUserInFullInfoById(conn, userId)) { // 不存在，新建
			String insertSql = "INSERT INTO UserFullInfo (userId, weiboNum, fansNum, followNum, weiboIdStr, fansIdStr, followIdStr) VALUES (" +
					userId + ", " + weiboNum + ", " + fansNum + ", " + followNum + ", " +
					"'" + weiboIdStr + "', '" + fansIdStr + "', '" + followIdStr + "');";
			SqlUtils.conduct(conn, insertSql);
			//System.out.println(insertSql);
		} else {
			String updateSql = "UPDATE UserFullInfo SET " +
					"weiboNum = " + weiboNum + ", " +
					"fansNum = " + fansNum + ", " +
					"followNum = " + followNum + ", " +
					"weiboIdStr = '" + weiboIdStr + "', " +
					"fansIdStr = '" + fansIdStr + "', " +
					"followIdStr = '" + followIdStr + "' " +
					"WHERE userId = " + userId + ";";
			SqlUtils.conduct(conn, updateSql);
			//System.out.println(updateSql);
		}

	}


	/**
	 * 将SinaWeiboCrawler中Weibo对象转为WeiboAPICrawler中的Message对象
	 * @param weibo
	 * @return
	 */
	public static Message weibo2Message(Weibo weibo) {
		if (weibo == null)
			return null;
		Message message = new Message();
		message.ID = weibo.id;
		message.userID = weibo.userId;
		message.userName = weibo.userName;
		message.createdTime = weibo.createdTime;
		message.text = generalProcess(weibo.text);
		message.likeNum = weibo.attitudeNum;
		message.commentsCount = weibo.commentNum;
		message.repostsCount = weibo.repostNum;

		message.retweetedStatus = weibo.postId;
		return message;
	}


	/**
	 * 将SinaWeiboCrawler中UserInfo对象转为WeiboAPICrawler中的User对象
	 * @param userInfo
	 * @return
	 */
	public static User userInfo2User(UserInfo userInfo) {
		if (userInfo == null)
			return null;
		User user = new User();
		user.userID = userInfo.userID;
		user.name = userInfo.name;
		user.screenName = userInfo.screenName;
		user.location = userInfo.location;
		user.description = generalProcess(userInfo.description);
		user.url = userInfo.url;
		user.gender = userInfo.gender;
		user.followersCount = userInfo.followersCount;
		user.friendsCount = userInfo.friendsCount;
		user.statusesCount = userInfo.statusesCount;
		user.createdTime = userInfo.createdTime;
		user.verified = userInfo.verified;
		user.verifiedReason = generalProcess(userInfo.verifiedReason);
		user.imagelink = userInfo.imagelink;
		return user;
	}

	public static int isExistCurrentMessage(Connection connection, String tableName, Long ID) {
		int num = 0;
		try{
			String sql = "select count(*) from "+tableName+" where ID="+ID+";";
			ResultSet result = SqlUtils.select(connection, sql);
			while(result.next())
				num = result.getInt(1);
		} catch (Exception e) {    
			e.printStackTrace();
		}  
		return num;
	}

	public static boolean addWeiboMessage(Connection connection, Message message){
		message.text = message.text.replaceAll("'", "");
		message.source = message.source.replaceAll("'", "");
		try {
			if (isExistCurrentMessage(connection, "WeiboMessageTopic", message.ID) > 0) {
				return false;
			}
			String sql = "insert into WeiboMessageTopic values (" +
					message.ID + "," +
					message.userID + "," +
					WeiboUtils.countID +"," +
					"'" + sdf.format(message.createdTime) + "'" + "," +
					"'" + message.text + "'" + "," +
					"'" + message.source + "'" + "," +
					"'" + message.location + "'" + "," +
					message.inReplyToStatusId + "," +
					message.inReplyToUserId + "," +
					message.retweetedStatus + "," +
					message.latitude + "," +
					message.longitude + "," +
					message.repostsCount + "," +
					message.sentiment + "," +
					message.commentsCount + ");";
			boolean flag = SqlUtils.conduct(connection, sql);
			if (flag==true) {
				WeiboUtils.countID++;
			}
		} catch (Exception e) {    
			e.printStackTrace();
			return false;
		}    
		return true;
	}


	public static int isExistCurrentUser(Connection connection, String tableName, Long userID){
		int num = 0;
		try {
			String sql = "select count(*) from "+tableName+" where userID="+userID+";";
			ResultSet result = SqlUtils.select(connection, sql);
			while (result.next())
				num = result.getInt(1);
		} catch (Exception e) {    
			e.printStackTrace();
		}  
		return num;
	}

	public static int getItemNum(Connection connection, String tableName){
		int num = 0;
		try{
			String sql = "select count(*) from "+tableName+";";
			ResultSet result = SqlUtils.select(connection, sql);
			while(result.next())
				num = result.getInt(1);
		} catch (Exception e) {    
			e.printStackTrace();
		}  
		return num;
	}

	public static boolean addWeiboUser (Connection connection, User user){
		user.name = SqlUtils.filterStr(user.name);
		user.screenName = SqlUtils.filterStr(user.screenName);
		user.description = SqlUtils.filterStr(user.description);
		try {
			if (isExistCurrentUser(connection, "WeiboUserTopic", user.userID) > 0) {
				return false;
			}
			String sql = "insert into WeiboUserTopic values (" +
					user.userID + "," +
					"'" + sdf.format(user.createdTime)+ "'," +
					"'" + user.name + "'," +
					"'" + user.screenName + "'," +
					user.province + ","+
					user.city + ","+
					"'" + user.location + "'," +
					"'" + user.description + "'," +
					"'" + user.url + "'," +
					"'" + user.userDomain + "'," +
					"'" + user.gender + "'," +
					user.followersCount + "," +
					user.friendsCount + "," +
					user.statusesCount + "," +
					user.favouritesCount + "," +
					"'"+ user.verified + "'," +
					user.verifiedType + "," +
					"'" + user.allowAllActMsg + "'," +
					"'" + user.allowAllComment+ "'," +
					user.biFollowersCount + "," +
					"'" + user.verifiedReason + "'," +
					"'" + user.imagelink + "');";
			SqlUtils.conduct(connection, sql);
		} catch (Exception e) {    
			System.err.println("an addWeiboUser expection");
			e.printStackTrace();
			return false;
		}    
		return true;
	}

	public static List<Message> queryWeibo(Connection connection, String sql) {
		List<Message> messageList = new ArrayList<Message>();
		try {
			ResultSet result = SqlUtils.select(connection, sql);
			while (result.next()) {
				Message message = new Message();
				message.ID = result.getLong("ID");
				message.userID = result.getLong("userID");
				message.countID = result.getLong("countID");
				message.createdTime = result.getDate("createdTime");
				message.text = result.getString("text");
				message.source = result.getString("source");
				message.location = result.getString("location");
				message.inReplyToStatusId = result.getLong("inReplyToStatusId");
				message.inReplyToUserId = result.getLong("inReplyToUserId");
				message.retweetedStatus = result.getLong("retweetedStatus");
				message.latitude = result.getDouble("latitude");
				message.longitude = result.getDouble("longitude");
				message.repostsCount = result.getInt("repostsCount");
				message.sentiment = result.getDouble("sentiment");
				message.commentsCount = result.getInt("commentsCount");
				messageList.add(message);	
			}
		} catch (Exception e) {    
			e.printStackTrace();
			return null;
		}
		return messageList;
	}

	public static int getMaxCountID(Connection connection, String tableName) {
		int num = 0;
		try {
			String sql = "select max(countID) from " + tableName + ";";
			ResultSet result = SqlUtils.select(connection, sql);
			while (result.next())
				num = result.getInt(1);
		} catch (Exception e) {
			e.printStackTrace();
		}  
		return num;
	}

	public static Message searchWeiboMessageViaID(Connection connection, Long weiboID) {
		String sql = "select * from WeiboMessageTopic where ID = " + weiboID + ";";
		List<Message> messageList = queryWeibo(connection, sql);
		if (messageList!=null && messageList.size()>0)
			return messageList.get(0);
		return null;
	}

	public static User searchWeiboUserViaID(Connection connection, Long userID) {
		String sql = "select * from WeiboUserTopic where userID = " + userID + ";";
		List<User> userList = queryWeiboUser(connection, sql);
		if (userList!=null && userList.size()>0)
			return userList.get(0);
		return null;
	}

	public static User searchWeiboUserViaName(Connection connection, String name){

		String sql = "select * from WeiboUserTopic where name = '" + name + "';";
		List<User> userList = queryWeiboUser(connection, sql);
		if(userList!=null && userList.size()>0)
			return userList.get(0);
		return null;
	}

	public static List<User> queryWeiboUser(Connection connection, String sql) {
		List<User> userList = new ArrayList<>();
		try {
			ResultSet result = SqlUtils.select(connection, sql);
			while (result.next()) {
				User user = new User();
				user.userID = result.getLong("userID");
				user.createdTime = result.getDate("createdTime");
				user.name = result.getString("name");
				user.screenName = result.getString("screenName");
				user.province = result.getInt("province");
				user.city = result.getInt("city");
				user.location = result.getString("location");
				user.description = result.getString("description");
				user.url = result.getString("url");
				user.userDomain = result.getString("userDomain");
				user.gender = result.getString("gender");
				user.followersCount = result.getInt("followersCount");
				user.friendsCount = result.getInt("friendsCount");
				user.statusesCount = result.getInt("statusesCount");
				user.favouritesCount = result.getInt("favouritesCount");
				user.verified = Boolean.parseBoolean(result.getString("verified"));
				user.verifiedType = result.getInt("verifiedType");
				user.allowAllActMsg = Boolean.parseBoolean(result.getString("allowAllActMsg"));
				user.allowAllComment = Boolean.parseBoolean(result.getString("allowAllComment"));
				user.biFollowersCount = result.getInt("biFollowersCount");
				user.verifiedReason = result.getString("verifiedReason");
				user.imagelink = result.getString("imagelink");
				userList.add(user);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}  
		return userList;
	}

	public static void main(String[] args) throws IOException, SQLException {
		Connection conn = SqlUtils.getConnection("Weibo", "ngnyzg");
		setAnalysisedTrue(conn, 1022377580L);

	}

}
