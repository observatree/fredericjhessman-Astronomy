// ApertureRoi.java

package astroj;

import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;

/**
 * A ROI consisting of three concentric circles (inner for object, outer annulus for background).
 * The ROI keeps track both of it's double position and a boolean flag which determines whether
 * the backround aperture should be displayed or not.
 *
 * @version 1.1
 * @date 2012-SEP-20
 * @changes Added WCS attachments (FVH)
 */
public class ApertureRoi extends Roi
	{
	private static final long serialVersionUID = 1L;	// MINOR VERSION NUMBER

	protected String theText = null;
	protected Font font = null;
	protected double xPos,yPos,srcMinusBack;
	protected double r1,r2,r3;
	protected Color apColor;

	protected double xWCS,yWCS;
	protected String xUnits,yUnits;
	protected boolean hasWCS = false;

	protected boolean showAperture = true;
	protected boolean showSky = false;
	protected boolean showLabel = false;

	public ApertureRoi (double x, double y, double rad1, double rad2, double rad3, double src, Color col)
		{
		super ((int)x,(int)y,1,1);

		xPos = x;		// Roi CLASS HAS PRIVATE FLOATING POINT COORDINATES
		yPos = y;
		r1 = rad1;
		r2 = rad2;
		r3 = rad3;
		srcMinusBack = src;
		font = new Font ("SansSerif", Font.PLAIN, 16);
		apColor = col;
		hasWCS = false;
		}

	/**
	 * Sets the WCS coordinates.
	 */
	public void setWCS (double[] xy, String[] xyunits)
		{
		if (xy != null)
			{
			xWCS = xy[0];
			yWCS = xy[1];
			if (xyunits != null)
				{
				xUnits = xyunits[0];
				yUnits = xyunits[1];
				}
			hasWCS = true;
			}
		}

	/**
	 * Sets the appearance of the ROI when it is displayed.
	 */
	public void setAppearance (boolean aper, boolean sky, boolean label, String text)
		{
		showAperture = aper;
		showSky = sky;
		showLabel = label;
		theText = text;
		}

	/**
	 * Returns the aperture color.
	 */
	public Color getApColor()
		{
		return apColor;
		}

	/**
	 * Sets the aperture radii.
	 */
	public void setRadii(double[] r)
		{
		r1 = r[0];
		r2 = r[1];
		r3 = r[2];
		}

	/**
	 * Returns the aperture radii.
	 */
	public double[] getRadii()
		{
		double[] r = new double[3];
		r[0] = r1;
		r[1] = r2;
		r[2] = r3;
		return r;
		}

	/**
	 * Lists the ROI contents.
	 */
	void log()
		{
		IJ.log("ApertureRoi: x,y="+xPos+","+yPos+", src-back="+srcMinusBack+", r1="+r1+", r2="+r2+", r3="+r3+", show="+showAperture+", a,d="+xWCS+","+yWCS+", color="+apColor);
		}

	/**
	 * Returns an array with the double position.
	 */
	public double[] getCenter()
		{
		double[] d = new double[] {xPos,yPos};
		return d;
		}

	/**
	 * Accepts an array with the double position.
	 */
	public void setCenter (double[] pos)
		{
		if (pos.length >= 2)
			{
			xPos = pos[0];
			yPos = pos[1];
			}
		}

	/**
	 * Returns an array with the double position.
	 */
	public double[] getWCS()
		{
		if (!hasWCS) return null;
		double[] d = new double[] {xWCS,yWCS};
		return d;
		}

	/**
	 * Returns an array with the unit strings.
	 */
	public String[] getWCSUnits()
		{
		if (!hasWCS) return null;
		String xu = new String(xUnits);
		String yu = new String(yUnits);
		String[] s = new String[] {xu,yu};
		return s;
		}

	/**
	 * Returns the measurement value associated with the ROI.
	 */
	public double getMeasurement()
		{
		return srcMinusBack;
		}

	/**
	 * Returns true if the screen position (xs,ys) is within the currently displayed area of the ROI
	 * (i.e. inner circle if showSky==false).
	 */
	public boolean contains (int xs, int ys)
		{
		int xx,yy,ww,hh;
		if (showSky)
			{
			xx = (int)(xPos-r3);
			ww = (int)(xPos+r3)-xx;
			yy = (int)(yPos-r3);
			hh = (int)(yPos+r3)-yy;
			}
		else	{
			xx = (int)(xPos-r1);
			ww = (int)(xPos+r1)-xx;
			yy = (int)(yPos-r1);
			hh = (int)(yPos+r1)-yy;
			}
		if (xs < xx || xs > xx+ww) return false;
		if (ys < yy || ys > yy+hh) return false;
		return true;
		}

	/**
	 * Displays the aperture either as a simple circle (showSky false) or as three circles (showSky true).
	 */
	public void draw (Graphics g)
		{
		g.setColor (apColor);
		int sx = fscreenX (xPos);
		int sy = fscreenY (yPos);

		int x1 = fscreenX (xPos-r1);
		int w1 = fscreenX (xPos+r1)-x1;
		int y1 = fscreenY (yPos-r1);
		int h1 = fscreenY (yPos+r1)-y1;

		if (showAperture)
			g.drawOval (x1,y1,w1,h1);

		if (showSky)
			{
			int x2 = fscreenX (xPos-r2);
			int x3 = fscreenX (xPos-r3);
			int w2 = fscreenX (xPos+r2)-x2;
			int w3 = fscreenX (xPos+r3)-x3;
			int y2 = fscreenY (yPos-r2);
			int y3 = fscreenY (yPos-r3);
			int h2 = fscreenY (yPos+r2)-y2;
			int h3 = fscreenY (yPos+r3)-y3;
			g.drawOval (x2,y2,w2,h2);
			g.drawOval (x3,y3,w3,h3);
			}

		if (showLabel && theText != null && !theText.equals(""))
			{
			g.setFont (font);
			FontMetrics metrics = g.getFontMetrics (font);
			int h = metrics.getHeight();
			int w = metrics.stringWidth(theText)+3;
			// int descent = metrics.getDescent();
			int xl = sx+h;
			int yl = sy+h/3;
			g.drawString (theText, xl,yl);
			}
		}

	/**
	 * Calculates the CORRECT screen x-position of a given decimal pixel position.
	 */
	public int fscreenX (double x) { return ic.screenXD(x+0.5-Centroid.PIXELCENTER); }

	/**
	 * Calculates the CORRECT screen y-position of a given decimal pixel position.
	 */
	public int fscreenY (double y) { return ic.screenYD(y+0.5-Centroid.PIXELCENTER); }

	}

