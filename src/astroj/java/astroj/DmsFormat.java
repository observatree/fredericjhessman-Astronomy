// DmsFormat.java

package astroj;

import java.text.*;

/**
 * Primitive extension of NumberFormat to produce a "HH:MM:SS.SS" format for a double.
 *
 * @version 1.1
 * @date 2012-SEP-19
 * @changes Bug in unformatting negative numbers! (FVH)
 */
public class DmsFormat extends DecimalFormat
	{
	NumberFormat nf = null;

	public DmsFormat (int places)
		{
		String s = "00.";
		for (int i=0; i < places; i++) s += "0";
		super.applyPattern (s);
		nf = NumberFormat.getInstance();
		nf.setMinimumIntegerDigits(2);
		}

	public String dms (double d)
		{
		double dd = Math.abs(d);
		int h = (int)dd;
		int m = (int)(60.0*(dd-(double)h));
		double s = 3600.0*(dd-(double)h-(double)m/60.0);

		String str = "";
		if (d < 0.0) str = "-";
		str += ""+nf.format(h)+":"+nf.format(m)+":"+super.format(s);
		return str;
		}

	public static double unformat(String str)
		{
		double dd = Double.NaN;
		double sgn = 1.0;
		if (str.trim().startsWith("-")) sgn = -1.0;
		try	{
			String[] parts = str.trim().split(":");
			if (parts.length < 2)	// AT LEAST dd:mm.mm IF NOT dd:mm:ss.ss
				parts = str.trim().split(" ");
			if (parts.length > 1)	// AT LEAST dd:mm.mm IF NOT dd:mm:ss.ss
				{
				double d = Math.abs(Double.parseDouble(parts[0]));
				double m = Double.parseDouble(parts[1])/60.0;
				double s = 0.0;
				if (parts.length > 2)
					s = Double.parseDouble(parts[2])/3600.0;
				dd = sgn*(d+m+s);
				}
			}
		catch (NumberFormatException e)
			{
			dd = Double.NaN;
			}
		return dd;
		}

	public static void main (String[] args)
		{
		double d;
		DmsFormat f = new DmsFormat(2);
		d = -123.45;
		System.out.println(""+d+" = "+f.dms(d));
		d = 12.345;
		System.out.println(""+d+" = "+f.dms(d));
		d = -1.2345;
		System.out.println(""+d+" = "+f.dms(d));
		d = 0.12345;
		System.out.println(""+d+" = "+f.dms(d));
		d = -0.012345;
		System.out.println(""+d+" = "+f.dms(d));
		d = 0.0012345;
		System.out.println(""+d+" = "+f.dms(d));
		}
	}

