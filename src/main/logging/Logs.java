package logging;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Logs {

	public static void createLogFor(String program) {
		try {
			Logger global = Logger.getLogger("");
			global.setLevel(Level.CONFIG);
			new File("logs").mkdir(); // crea la cartella se non esiste

			// se c'è handler[0] è la console, in questo modo imposto il formato
			// su una riga sola
			if (global.getHandlers().length > 0) {
				global.getHandlers()[0].setFormatter(new OneLineFormatter());
			}

			// FileHandler txt = new FileHandler(
			// "logs/" + program + "_" + new
			// SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) +
			// ".html");
			// txt.setFormatter(new SimpleFormatter());
			// global.addHandler(txt);

			FileHandler html = new FileHandler(
					"logs/" + program + "_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + ".html");
			html.setFormatter(new HtmlFormatter(program));
			global.addHandler(html);
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
	}

}
