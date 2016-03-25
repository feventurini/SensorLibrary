package logging;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class HtmlFormatter extends Formatter {

	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	// this method is called for every log records
	public String format(LogRecord rec) {
		StringBuffer buf = new StringBuffer(100);
		buf.append("<tr>");
		// colorize WARNING in yellow, SEVERE in red
		if (rec.getLevel().intValue() == Level.SEVERE.intValue()) {
			buf.append("<td style=\"color:red; font-weight:bold;\">");
		} else if (rec.getLevel().intValue() == Level.WARNING.intValue()) {
			buf.append("<td style=\"color:yellow; font-weight:bold;\">");
		} else {
			buf.append("<td>");
		}
		buf.append(rec.getLevel());
		buf.append("</td><td>");
		buf.append(dateFormat.format(new Date(rec.getMillis())));
		buf.append("</td><td>");
		buf.append(formatMessage(rec));
		buf.append("</td></tr>");

		return buf.toString();
	}

	// this method is called just after the handler using this
	// formatter is created
	public String getHead(Handler h) {
		return "<!DOCTYPE html><head><style>\n" +"body { font: Arial, sans-serif}\n"+ "table { width: 100% }\n" + "th { font:bold 10pt; }\n"
				+ "td { font:normal 10pt; }\n" + "h1 {font:normal 11pt;}\n" + "</style>\n" + "</head>\n"
				+ "<body>\n" + "<h1>Log of " + dateFormat.format(new Date()) + "</h1>\n"
				+ "<table border=\"0\" cellpadding=\"5\" cellspacing=\"3\">\n" + "<tr align=\"left\">\n"
				+ "\t<th style=\"width:10%\">Loglevel</th>\n" + "\t<th style=\"width:15%\">Time</th>\n"
				+ "\t<th style=\"width:75%\">Log Message</th>\n" + "</tr>\n";
	}

	// this method is called just after the handler using this
	// formatter is closed
	public String getTail(Handler h) {
		return "</table></body></html>";
	}
}
