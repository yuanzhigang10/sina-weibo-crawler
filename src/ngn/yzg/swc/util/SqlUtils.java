package ngn.yzg.swc.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.lang3.StringUtils;

/**
 * MySQL数据库基本工具函数
 * @author PeterYuan
 *
 */
public final class SqlUtils {
	
	private SqlUtils() {}
	

	/**
	 * 连接到数据库
	 * @param databaseName 数据库名称
	 * @param password 密码
	 * @return 数据库连接对象
	 */
	public static Connection getConnection(String databaseName, String password) {
		String driver = "com.mysql.jdbc.Driver";
		String url = "jdbc:mysql://localhost:3306/" + databaseName;
		String user = "root";

		Connection conn = null;
		try {    
			Class.forName(driver);
			conn = DriverManager.getConnection(url, user, password);
			if(!conn.isClosed()) {
				System.out.println("succeeded connecting to the database: " + databaseName);
			}
		} catch (Exception e) {    
			e.printStackTrace();
		}    
		return conn;
	}
	

	/**
	 * 关闭数据库连接
	 * @param conn
	 */
	public static void closeConnection(Connection conn) {
		try {
			conn.close();
		} catch (SQLException e) {
			System.out.println("sorry, can't find the driver!");
		}
	}
	

	/**
	 * 数据库查询操作
	 * @param conn
	 * @param sql
	 * @return
	 */
	public static ResultSet select(Connection conn, String sql) {
		Statement statement;
		ResultSet rs = null;
		try {
			statement = conn.createStatement();
			rs = statement.executeQuery(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rs;
	}
	

	/**
	 * 数据库执行操作
	 * @param conn
	 * @param sql
	 * @return
	 */
	public static boolean conduct(Connection conn, String sql) {
		Statement statement;
		try {
			statement = conn.createStatement();
			statement.executeUpdate(sql);
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}


	/**
	 * 过滤掉“数据库危险字符”
	 * @param str
	 * @return
	 */
	public static final String filterStr(String str) {
		if (str == null) {
			return null;
		}
		str = str.replaceAll(";","");
		str = str.replaceAll("&","&amp;");
		str = str.replaceAll("<","&lt;");
		str = str.replaceAll(">","&gt;");
		str = str.replaceAll("'","");
		str = str.replaceAll("--"," ");
		str = str.replaceAll("/","");
		str = str.replaceAll("%","");
		if (StringUtils.isNotBlank(str)) {
			str = str.replaceAll("[\\ud800\\udc00-\\udbff\\udfff\\ud800-\\udfff]", "*");
		}
		return str;
	}


}
