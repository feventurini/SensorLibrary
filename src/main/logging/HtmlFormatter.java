package logging;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class HtmlFormatter extends Formatter {

	private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private String title;

	public HtmlFormatter(String title) {
		this.title = "Log of " + title + " from " + dateFormat.format(new Date());
	}

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
		buf.append(rec.getSourceClassName() + " " + rec.getSourceMethodName());
		buf.append("</td><td>");
		buf.append(formatMessage(rec));
		buf.append("</td></tr>");

		return buf.toString();
	}

	// this method is called just after the handler using this
	// formatter is created
	public String getHead(Handler h) {
		return "<!DOCTYPE html><head><title>" + title
				+ "</title><style>body {font: Arial, sans-serif} table {width: 100%} th {font:bold 9pt}"
				+ "td {font:normal 8pt} h1 {font:normal 11pt}</style></head>" + "<body><h1>" + title + "</h1>"
				+ "<table border=\"0\" cellpadding=\"5\" cellspacing=\"3\"><tr align=\"left\">"
				+ "<th style=\"width:10%\">Loglevel</th><th style=\"width:15%\">Time</th><th style=\"width:15%\">Origin</th><th style=\"width:60%\">Log Message</th></tr>";
	}

	// this method is called just after the handler using this
	// formatter is closed
	public String getTail(Handler h) {
		return "</table></body></html>";
	}
}
