package ngn.yzg.swc.service;

import java.util.Date;

public class Message implements Comparable<Message> {
	
	/**
	 * @param args
	 */
	
	public long ID = 0L; //must userID+time
	public long userID = 0L; //must
	public String userName = ""; //must
	public Date createdTime = new Date(); //must
	public String text = ""; //must
	public String source = "";
	public String location = "";
	public long inReplyToStatusId = 0L;
	public long inReplyToUserId = 0L;
	public long retweetedStatus = 0L;
	public double latitude = 0;
	public double longitude = 0;
	public int repostsCount = 0; //must
	public int commentsCount = 0; //must
	public int likeNum = 0; //must
	public double sentiment = 0;
	
	public long countID = 0L;
	
	public Message() {
	}

	@Override
	public int compareTo(Message o) {
		return (o.createdTime.compareTo(this.createdTime));
	}
	
	
	
	
}
