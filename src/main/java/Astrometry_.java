// Astrometry_.java

import ij.*;
import ij.gui.*;
import ij.measure.*;
import ij.plugin.filter.*;
import ij.process.*;

import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;
import java.util.*;

import astroj.*;

/**
 * @author F.V. Hessman, Georg-August-Universitaet Goettingen
 * @version 1.0
 * @date 2007-Jan-28
 */
public class Astrometry_ extends MultiAperture_
	{
	protected boolean prepare ()
		{
		firstSlice = img.getCurrentSlice();
		lastSlice = firstSlice;
		doStack = false;
		return super.prepare();
		}

	/**
	 * Define the apertures and decide on the sub-stack if appropriate.
	 */
	protected boolean setUpApertures ()
		{
		// RUN DIALOGUE

		if (! IJ.showMessageWithCancel ("Astrometry Tool","Select the two stars of known position to be measured."))
			{
			cancelled = true;
			return false;
			}

		// GET NUMBER OF ALIGNMENT OBJECTS

		nApertures = 2;
		xPos = new double[nApertures];
		yPos = new double[nApertures];
		ngot = 0;
		return true;
		}

	/**
	 * Finishes whole process by computing separation, angle, and scale from the pixel
	 * positions and input R.A. and Decl. for both objects.
	 */
	protected void shutDown()
		{
		super.shutDown();
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits (3);

		boolean saving = true;

		GenericDialog gd = new GenericDialog ("Input R.A. and Declination");

		gd.addMessage    ("OBJECT #1:");
		gd.addMessage    ("X="+nf.format(xPos[0])+" pixels");
		gd.addMessage    ("Y="+nf.format(yPos[0])+" pixels");
		gd.addStringField ("R.A.","0 0 0.0");
		gd.addStringField ("Decl.","-0 0 0.0");

		gd.addMessage (" ");

		gd.addMessage    ("OBJECT #2:");
		gd.addMessage    ("X="+nf.format(xPos[1])+" pixels");
		gd.addMessage    ("Y="+nf.format(yPos[1])+" pixels");
		gd.addStringField ("R.A.","0 0 0.0");
		gd.addStringField ("Decl.","+0 0 0.0");

		gd.addCheckbox ("Save results in FITS header",saving);

		gd.showDialog();
		if ( gd.wasCanceled() ) return;

		String ss = gd.getNextString().trim();

		String[] sra1 = ss.split(" ");

		ss = gd.getNextString().trim();
		String[] sdec1 = ss.split(" ");

		ss = gd.getNextString().trim();
		String[] sra2=ss.split(" ");

		ss = gd.getNextString().trim();
		String[] sdec2 = ss.split(" ");

		saving = gd.getNextBoolean();

		if (	sra1.length   < 1 || sra1.length > 3 ||
			sdec1.length < 1 || sdec1.length > 3 ||
			sra2.length   < 1 || sra2.length    > 3 ||
			sdec2.length < 1 || sdec2.length  > 3)
			{
			IJ.error ("R.A. and/or Decl. syntax error!");
			return;
			}

		double ra1=0.0;
		double dec1=0.0;
		double ra2=0.0;
		double dec2=0.0;

		try	{
			for (int i=0; i < sra1.length; i++)
				ra1 = 60.0*ra1+Double.parseDouble(sra1[i]);
			ra1 /= Math.pow(60.0,sra1.length-1);
			ra1 *= 15.0*Math.PI/180.0;

			double dsgn=1.0;
			for (int i=0; i < sdec1.length; i++)
				{
				dec1 = dec1*60.0+Math.abs(Double.parseDouble(sdec1[i]));
				if (sdec1[i].startsWith("-")) dsgn= -1.0;
				}
			dec1 /= Math.pow(60.0,sdec1.length-1);
			dec1 *= dsgn*Math.PI/180.0;

			for (int i=0; i < sra2.length; i++)
				ra2 = ra2*60.0+Double.parseDouble(sra2[i]);
			ra2 /= Math.pow(60.0,sra2.length-1);   // HOURS
			ra2 *= 15.0*Math.PI/180.0;  // RADIANS

			dsgn=1.0;
			for (int i=0; i < sdec2.length; i++)
				{
				dec2 = dec2*60.0+Math.abs(Double.parseDouble(sdec2[i]));
				if (sdec2[i].startsWith("-")) dsgn= -1.0;
				}
			dec2 /= Math.pow(60.0,sdec2.length-1);
			dec2 *= dsgn*Math.PI/180.0;
			}
		catch (NumberFormatException e)
			{
			IJ.error ("Cannot parse R.A. and/or Declinations");
			return;
			}

		// USE HAVERSINE FORMULA TO GET SEPARATION

		double sindd = Math.sin(0.5*(dec1-dec2));
		double sinda = Math.sin(0.5*(ra1-ra2));
		double haver = sindd*sindd+Math.cos(dec1)*Math.cos(dec2)*sinda*sinda;
		if (haver > 1.0) haver=1.0;
		double separ = 2.0*Math.asin(Math.sqrt(haver));
		double s = separ*3600.0*180.0/Math.PI;	   // IN ARCSECONDS

		// USE TRIG TO GET POSITION ANGLE (MEASURED FROM NORTH

		double pa = Math.asin(Math.cos(dec2)*Math.sin(ra2-ra1)/Math.sin(separ));

		// ASSUMING separ << PI, |ra2-ra1| << PI AND KNOWING cos(dec2) > 0, 

		if (dec2 < dec1)
			pa = Math.PI-pa;
		else if (pa < 0.0)
			pa += 2.0*Math.PI;
		double posang = pa*180.0/Math.PI;

		// DERIVE PLATESCALE

		double spix = Math.sqrt(
			(xPos[1]-xPos[0])*(xPos[1]-xPos[0]) +
			(yPos[1]-yPos[0])*(yPos[1]-yPos[0])
								);
		double sangle = Math.atan2(yPos[1]-yPos[0], xPos[1]-xPos[0])*180.0/Math.PI;
		double platescale = s/spix;	// ARCSECS/PIXEL

		// SHOW RESULTS

		IJ.log ("Separation: "+nf.format(s)+"\" = "+nf.format(spix)+" pixels\n"+
			"\tPosition angle: "+nf.format(posang)+" degrees ("+nf.format(sangle)+" in image)\n"+
			"\tLocal scale: "+nf.format(platescale)+ "\"/pixel");
		IJ.showMessage ("Results",
			"Separation: "+nf.format(s)+"\" = "+nf.format(spix)+" pixels, "+
			"\tPosition angle: "+nf.format(posang)+" degrees ("+nf.format(sangle)+" in image)");

		// SHOW ANGLES AS A POLYLINE ROI

		int xpos[] = new int[] { (int)xPos[0], (int)xPos[1]};
		int ypos[] = new int[] { (int)yPos[0], (int)yPos[1]};
		PolygonRoi roi = new PolygonRoi (xpos,ypos,2,Roi.POLYLINE);

		// SAVE RESULTS IN FITS HEADER?

		if (saving)
			{
			String[] hdr = FitsJ.getHeader (img);
			int    CRPIX1 = 0.5*(1.0+(double)img.getWidth());
			int    CRPIX2 = 0.5*(1.0+(double)img.getHeight());
			double CDELT1 = -platescale;
			double CDELT2 = platescale;
			double[] PC = new double[2][2];
			PC[0][0] = Math.cos(pa);
			PC[0][1] = Math.sin(pa);
			PC[1][0] = -PC[0][1];
			PC[1][1] =  PC[0][0];
			double CRVAL1 = 1.23456;
			double CRVAL2 = 9.87654;
			IJ.log ("New FITS Header Entries:");
			IJ.log ("\tCTYPE1  = 'RA---TAN'");
			IJ.log ("\tCTYPE2  = 'DEC--TAN'");
			IJ.log ("\tCRVAL1  = ...");
			}
		img.unlock();
		}
	}
