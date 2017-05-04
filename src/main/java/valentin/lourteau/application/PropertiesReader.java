package valentin.lourteau.application;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class PropertiesReader {
	
	public final static String PROPERTIE_FILE_NAME = "configuration/config.properties";

	@SuppressWarnings("finally")
	public static String getPropertie(String key) {
		Properties prop = null;
		try {
				prop = load(PROPERTIE_FILE_NAME);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			return prop != null ? prop.getProperty(key) : null;
		}

	}

	public static Properties load(String fileName) throws IOException, FileNotFoundException {
		Properties properties = new Properties();
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		FileInputStream input = (FileInputStream) classLoader.getResourceAsStream(fileName);
		try {
			properties.load(input);
			return properties;
		} finally {
			input.close();
		}
	}


}
