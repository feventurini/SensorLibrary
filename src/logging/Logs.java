package logging;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Logs {

	public static void init() {
		try {
			Logger global = Logger.getLogger("");
			FileHandler txt = new FileHandler(
					"Provider_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + ".log");
			txt.setFormatter(new SimpleFormatter());
			FileHandler html = new FileHandler(
					"Provider_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + ".html");
			html.setFormatter(new HtmlFormatter());
			global.addHandler(txt);
			global.addHandler(html);
			global.setLevel(Level.CONFIG);
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
	}

}
