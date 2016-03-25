package logging;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Logs {

	public static void init() {
		try {
			Logger global = Logger.getLogger("");
			global.setLevel(Level.CONFIG);
			
//			FileHandler txt = new FileHandler(
//					"Provider_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + ".log");
//			txt.setFormatter(new SimpleFormatter());
//			global.addHandler(txt);

			new File("logs").mkdir(); // crea la cartella se non esiste
			FileHandler html = new FileHandler("logs/log_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + ".html");
			html.setFormatter(new HtmlFormatter());
			global.addHandler(html);
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
	}

}
