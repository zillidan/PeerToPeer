package p2pBLL;

import java.io.*;
import java.util.*;

public class ConfigManager {

	public static Properties _configManager = null;

	public static void init() {
		try {
			_configManager = new Properties();
			_configManager.load(new FileInputStream("../config.properties"));
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	public static String getProperty(String key) {
		return _configManager.getProperty(key);
	}
}
