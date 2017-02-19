package ngn.yzg.swc.task;

import java.io.File;
import java.sql.Connection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;

import ngn.yzg.swc.entity.UserAll;
import ngn.yzg.swc.service.WeiboUtils;
import ngn.yzg.swc.util.SqlUtils;
import ngn.yzg.swc.util.Utils;

public class AddCrawled2DB {
	///////////////////////////////////////////////////////////////////////////
	private static boolean serverMode = false;
	///////////////////////////////////////////////////////////////////////////

	private static String inputPath = serverMode ? "/cngi1/zhigang/relation/users/":"C:/Users/PeterYuan/Desktop/newoutput/";
	private static Connection connection;
	private static String password = serverMode ? "***" : "***"; // TODO
	private static Pattern pattern = Pattern.compile("\\d{10}");

	private static void run() {
		connection = SqlUtils.getConnection("Weibo", password);

		File dir = new File(inputPath);
		String[] filenames = dir.list();
		Matcher m = null;
		for (String filename : filenames) {
			m = pattern.matcher(filename);
			if (m.find()) {
				try {
					System.out.println("adding " + m.group() + " to database...");
					String jsonStr = Utils.readToString(new File(inputPath + File.separator + m.group() + ".json"));
					UserAll userAll = new Gson().fromJson(jsonStr, UserAll.class);
					WeiboUtils.updateUserAll2DB(connection, userAll);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			try {
				Thread.sleep(500L);
			} catch (InterruptedException e) {
				e.printStackTrace();
			};
		}
		System.out.println("\nall done");
	}





	public static void main(String[] args) {
		run();
	}

}
