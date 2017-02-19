package ngn.yzg.swc.entity;

/**
 * 粉丝/关注实体类。
 * 
 * @author yzg
 *
 */
public class Friend {
	public EntityType friendType;
	
	public String url = "";
	public String imageLink = "";
	public String name = "";
	
	public long id = 0L;
	public int fansNum = 0;
	
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj != null && obj.getClass() == Friend.class) {
			Friend friend = (Friend)obj;
			return (new Long(this.id).equals(friend.id));
		}
		return false;
	}	
	
}
