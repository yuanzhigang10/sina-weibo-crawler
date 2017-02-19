package ngn.yzg.swc.entity;

/**
 * 微博账号实体类。
 * 
 * <p>
 * 微博账号的用户名和密码。用于模拟登录，进行后续爬取。<br>
 * 注意：密码是明文保存，尽量使用小号！
 * 
 * @author yzg
 *
 */
public class WeiboAccount {
	
	
	public WeiboAccount(String username, String password) {
		this.setUsername(username);
		this.setPassword(password);
	}
	
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	private String username;
	private String password;
	
}
