package com.grameenphone.wipro.task_executor.util;

import com.ctc.wstx.api.WriterConfig;
import com.ctc.wstx.sw.BufferingXmlWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Calendar;

public class StringUtil {
	public static String escapeForXml(String source) throws IOException {
		try {
			StringWriter stringWriter = new StringWriter();
			BufferingXmlWriter writer = new BufferingXmlWriter(stringWriter, WriterConfig.createFullDefaults(), "UTF-8", false, null, 16);
			writer.writeCharacters(source);
			writer.close(true);
			return stringWriter.toString();
		} catch (IOException h) {
			if(h.getMessage().startsWith("Invalid white space character")) {
				return escapeForXml(source.replaceAll("\\p{C}", ""));
			}
			throw h;
		}
	}

	public static boolean isNullOrEmpty(String toCheck) {
		return toCheck == null || toCheck.trim().length() == 0;
	}

	public static boolean hasText(String toCheck) {
		return !isNullOrEmpty(toCheck);
	}

	/**
	 * @param msisdn
	 * @return 10 digit msisdn if format is valid
	 */
	public static String sanitizeMsisdn(String msisdn) {
		if (msisdn == null) {
			return null;
		}
		if (!msisdn.matches("^(\\+?880|0)?1\\d{9}$")) {
			return null;
		}
		return msisdn.substring(msisdn.length() - 10);
	}

	public static String escapeForLikeExpression(String exp) {
		if (exp == null) {
			return null;
		}
		return exp.replaceAll("([%\\\\_])", "\\\\$1");
	}

	public static String minSecMilli(long milli) {
		long millis = milli % 1000;
		long rem = (milli - millis) / 1000;
		long seconds = rem % 60;
		rem = (rem - seconds) / 60;
		String result = " ";
		if(rem != 0) {
			result += rem + " min, ";
		}
		if(rem != 0 || seconds != 0) {
			result += seconds + " sec, ";
		}
		result += millis + " ms";
		return result.substring(1);
	}

	public static int hmTos(String hm) {
		char lastChar = hm.charAt(hm.length() - 1);
		if(lastChar > 47 && lastChar < 58) {
			return Integer.parseInt(hm);
		}
		int refValue = Integer.parseInt(hm.substring(0, hm.length() - 1));
		if(lastChar == 'm') {
			return refValue * 60;
		}
		if(lastChar == 'h') {
			return refValue * 60 * 60;
		}
		return refValue;
	}

	public static long toMilli(String hourMinSec) {
		String[] parts = hourMinSec.trim().split("\\s+");
		long resultantMillis = 0;
		for(String a : parts) {
			char last = a.charAt(a.length() - 1);
			int number = Integer.parseInt(a.substring(0, a.length() - 1));
			long factor = 1;
			switch(last) {
				case 'h':
					factor *= 60;
				case 'm':
					factor *= 60;
				case 's':
					factor *= 1000;
					break;
				default:
					number = number * 10 + (last - 48);
			}
			resultantMillis += number * factor;
		}
		return resultantMillis;
	}

	public static Long toLong(String longString) {
		if(longString == null) {
			return null;
		}
		try {
			return Long.parseLong(longString);
		} catch (Exception e) {
			return null;
		}
	}

	public static String generateUniqueReference(String billType) {
		StringBuilder sb = new StringBuilder();
		sb.append(billType);
		sb.append(String.format("%02d", Calendar.getInstance().get(Calendar.DAY_OF_MONTH)));
		sb.append(("" + System.currentTimeMillis()).substring(3));
		sb.append(String.format("%03d", System.nanoTime() % 1000));
		return sb.toString();
	}

	public static String truncate(String text, int length) {
		if(text == null || text.length() <= length) {
			return text;
		}
		return text.substring(0, length-3) + "...";
	}
}