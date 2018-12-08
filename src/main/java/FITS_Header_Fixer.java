// FITS_Header_Fixer.java

import ij.*;
import ij.gui.*;
import ij.plugin.filter.*;
import ij.process.*;

import java.awt.*;

import astroj.*;

public class FITS_Header_Fixer implements PlugInFilter
	{
	ImagePlus img;
	boolean correct;
	String[] header;

	public int setup(String arg, ImagePlus img)
		{
		IJ.register(FITS_Header_Fixer.class);
		this.img = img;
		return DOES_ALL;
		}

	public void run (ImageProcessor ip)
		{
		ImageStack stack = img.getStack ();
		int nslices = stack.getSize ();
		if (nslices == 1)
			{
			header = FitsJ.getHeader (img);
			if (header == null)
				IJ.error("Cannot access FITS header!");
			else
				fix (img);
			}
		else	{
			for (int slice=1; slice <= nslices; slice++)
				{
				IJ.showStatus (" "+slice+"/"+nslices);
				img.setSlice (slice);
				ImagePlus imag = IJ.getImage ();
				fix (imag);
				}
			img.setSlice (1);
			}
		}

	protected void fix (ImagePlus imag)
		{
		// GET HEADER
		header = FitsJ.getHeader (imag);

		// FIX DOUBLE AND STRING CARDS
		for (int i=0; i < header.length; i++)
			{
			String card = header[i];
			String typ = FitsJ.getCardType (card);
			String key = FitsJ.getCardKey (card);
			String comment = FitsJ.getCardComment (card);

			// IJ.log (typ+": "+card);
			if (typ == "R")		// GETS RID OF UNNECESSARY "+" AT THE BEGINNING OF NUMBERS
				{
				double d = FitsJ.getCardDoubleValue (card);
				FitsJ.setCard (key,d,comment, header);
				}
			else if (typ == "S")	// GET RID OF NON-ASCII CHARACTERS
				{
				String s = FitsJ.getCardStringValue (card);
				String ascii = s.replaceAll ("[^\\p{ASCII}]","*");
				FitsJ.setCard (key,ascii,comment, header);
				}
			}

		// MAKE SURE MINIMUM WCS INFO IS PRESENT

		int i = FitsJ.findCardWithKey ("CRPIX1",header);
		if (i == -1)
			{
			double x = (double)img.getWidth()/2.;
			double y = (double)img.getHeight()/2.;
			header = FitsJ.setCard ("CRPIX1",x,"ASSUMED WCS REFERENCE POINT ONLY !!!",header);
			header = FitsJ.setCard ("CRPIX2",y,"ASSUMED WCS REFERENCE POINT ONLY !!!",header);
			}

		i = FitsJ.findCardWithKey ("CTYPE1",header);
		if (i == -1)
			{
			header = FitsJ.setCard ("CTYPE1","RA---TAN","ASSUMED WCS TYPE ONLY !!!",header);
			header = FitsJ.setCard ("CTYPE2","DEC--TAN","ASSUMED WCS TYPE ONLY !!!",header);
			}

		i = FitsJ.findCardWithKey ("CRVAL1",header);
		if (i == -1)
			{
			i = FitsJ.findCardWithKey ("RA",header);
			if (i >= 0)
				{
				String sra = FitsJ.getCardStringValue (header[i]);
				double ra = MeasurementTable.hms(sra)*15.0;	// HOURS TO DEGREES
				header = FitsJ.setCard ("CRVAL1",ra,"FROM OLD-RA [deg]",header);
				header = FitsJ.renameCardWithKey ("RA","OLD-RA",header);
				}
			}
		i = FitsJ.findCardWithKey ("CRVAL2",header);
		if (i == -1)
			{
			i = FitsJ.findCardWithKey ("DEC",header);
			if (i >= 0)
				{
				String sdec = FitsJ.getCardStringValue (header[i]);
				double dec = MeasurementTable.hms(sdec);
				header = FitsJ.setCard ("CRVAL2",dec,"FROM OLD-DEC [deg]",header);
				header = FitsJ.renameCardWithKey ("DEC","OLD-DEC",header);
				}
			}

		// SAVE CORRECTED HEADER
		FitsJ.putHeader (imag,header);
		}
	}
