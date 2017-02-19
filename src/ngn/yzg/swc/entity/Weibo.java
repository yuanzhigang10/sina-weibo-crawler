package ngn.yzg.swc.entity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 微博实体类。
 * 
 * @author yzg
 *
 */
public class Weibo {
	public long id = 0L;
	public String userName = "";
	public String userUrl = "";
	public long userId = 0;

	public String postName = "";
	public String postUrl = "";
	public long postId = 0;

	public String commentUrl = "";

	public String text = "";
	public Date createdTime = new Date();


	public int repostNum = 0;
	public int commentNum = 0;
	public int attitudeNum = 0;
	
	public List<WeiboAux> commentList = new ArrayList<WeiboAux>();
	public List<WeiboAux> repostList = new ArrayList<WeiboAux>();
	public List<WeiboAux> attitudeList = new ArrayList<WeiboAux>();
	

	public Weibo() {}
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Weibo other = (Weibo) obj;
		if (id != other.id)
			return false;
		return true;
	}



	/**
	 * 获取用户<tt>UserId</tt>和微博创建时间后，应用这两个信息人为构建微博的唯一标识<tt>id</tt>。
	 */
	public void refreshId() {
		this.id = Long.parseLong(this.userId + new SimpleDateFormat("MMddHHmm").format(this.createdTime));
	}


}
