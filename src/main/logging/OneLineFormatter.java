package logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class OneLineFormatter extends Formatter {

	Date dat = new Date();
	private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	// Line separator string. This is the value of the line.separator
	// property at the moment that the SimpleFormatter was created.
	// private String lineSeparator = (String)
	// java.security.AccessController.doPrivileged(
	// new sun.security.action.GetPropertyAction("line.separator"));
	private String lineSeparator = "\n";

	/**
	 * Format the given LogRecord.
	 * 
	 * @param record
	 *            the log record to be formatted.
	 * @return a formatted log record
	 */
	public synchronized String format(LogRecord record) {
		StringBuilder sb = new StringBuilder();
		
		// Time
		dat.setTime(record.getMillis()); // Minimize memory allocations here.
		sb.append(dateFormat.format(dat))
		.append(" ")

		// Level
		.append(record.getLevel())
		.append("\t");

		// Class name
		if (record.getSourceClassName() != null) {
			sb.append(record.getSourceClassName());
		} else {
			sb.append(record.getLoggerName());
		}

		// Method name
		if (record.getSourceMethodName() != null) {
			sb.append(" ")
			.append(record.getSourceMethodName());
		}
		
		sb.append(": ");
		
		String message = formatMessage(record);
		sb.append(message);
		sb.append(lineSeparator);
		if (record.getThrown() != null) {
			try {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				record.getThrown().printStackTrace(pw);
				pw.close();
				sb.append(sw.toString());
			} catch (Exception ex) {
			}
		}
		return sb.toString();
	}

}
