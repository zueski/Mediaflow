package house.neko.util;

import java.util.Date;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;

/**
 *
 * @author andy
 */
public class FastDateFormat
	extends DateFormat 
{
	private static final long serialVersionUID = 1L;

	DateFormat df;
	long lastSec = -1;
	StringBuffer sb = new StringBuffer();
	FieldPosition fp = new FieldPosition(DateFormat.MILLISECOND_FIELD);

	/**
	 *
	 * @param df
	 */
	public FastDateFormat(DateFormat df)
	{	this.df = df; }

	/**
	 *
	 * @param text
	 * @param pos
	 * @return
	 */
	public Date parse(String text, ParsePosition pos)
	{	return df.parse(text, pos); }

	/**
	 * Note: breaks functionality of fieldPosition param. Also:
	 * there's a bug in SimpleDateFormat with "S" and "SS", use "SSS"
	 * instead if you want a msec field.
	 *
	 * @param date
	 * @param toAppendTo
	 * @param fieldPosition
	 * @return
	 */
	public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) 
	{
		long dt = date.getTime();
		long ds = dt / 1000;
		if (ds != lastSec) 
		{
			sb.setLength(0);
			df.format(date, sb, fp);
			lastSec = ds;
		} else {
			// munge current msec into existing string
			int ms = (int)(dt % 1000);
			int pos = fp.getEndIndex();
			int begin = fp.getBeginIndex();
			if (pos > 0) 
			{
				if (pos > begin)
				{	sb.setCharAt(--pos, Character.forDigit(ms % 10, 10)); }
				ms /= 10;
				if (pos > begin)
				{	sb.setCharAt(--pos, Character.forDigit(ms % 10, 10)); }
				ms /= 10;
				if (pos > begin)
				{	sb.setCharAt(--pos, Character.forDigit(ms % 10, 10)); }
			}
		}
		toAppendTo.append(sb.toString());
		return toAppendTo;
	}
}
