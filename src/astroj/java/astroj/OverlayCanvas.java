// OverlayCanvas.java

package astroj;

import java.awt.*;
import java.util.*;

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;

/**
 * A special ImageCanvas which displays lists of ROIs in a non-destructive overlay.
 */
public class OverlayCanvas extends ImageCanvas
	{
	/** A Vector for storing the ROIs in the overlay. */
	Vector rois = null;

	/**
	 * Constructor for the overlay, attached to a particular ImagePlus.
	 */
	public OverlayCanvas (ImagePlus imp)
		{
		super (imp);
		rois = new Vector();
		}

	/**
	 * Number of rois attached to overlay.
	 */
	public void add (Roi roi)
		{
		rois.addElement (roi);
		}

	/**
	 * Removes a particular roi from the overlay list if it encloses the pixel position (x,y).
	 */
	public boolean removeRoi (int x, int y)
		{
		boolean removed = false;
		Enumeration e = rois.elements();
		while (e.hasMoreElements())
			{
			Roi roi = (Roi)e.nextElement();
			if (roi.contains (x,y))
				{
				rois.remove (roi);
				removed = true;
				}
			}
		return removed;
		}

	/**
	 * Deletes all of the current ROIs.
	 */
	public void clearRois ()
		{
		rois.clear();
		}

	/**
	 * Lists current ROIs.
	 */
	public void listRois()
		{
		int n = rois.size();
		if (n == 0) return;
		Enumeration e = rois.elements();
		for (int i=0; i < n && e.hasMoreElements(); i++)
			{
			Roi roi = (Roi)e.nextElement();
			if (roi instanceof ApertureRoi)
				{
				ApertureRoi aroi = (ApertureRoi)roi;
				aroi.log();
				}
			}
		}

	/**
	 * Returns an array of copies of the current ROIs.
	 */
	public Roi[] getRois()
		{
		int n = rois.size();
		if (n == 0) return null;

		Roi[] arr = new Roi[n];
		Enumeration e = rois.elements();
		for (int i=0; i < n; i++)
			{
			if (e.hasMoreElements())
				arr[i] = (Roi)e.nextElement();
			}
		return arr;
		}

	/**
	 * Returns the number of rois in the overlay's list.
	 */
	public int numberOfRois()
		{
		return rois.size();
		}

	/**
	 * Displays the overlay and it's ROIs.
	 */
	public void paint(Graphics g)
		{
		super.paint (g);
		drawOverlayCanvas (g);
		}

	public void drawOverlayCanvas (Graphics g)
		{
		Enumeration e = rois.elements();
		while (e.hasMoreElements())
			{
			Roi roi = (Roi)e.nextElement();
			roi.draw (g);
			}
		}

	/**
	 * A handy routine which checks if the image has an OverlayCanvas.
	 */
	public static boolean hasOverlayCanvas (ImagePlus imag)
		{
		// ImageCanvas canvas = imag.getCanvas();	    // ImageJ 1.38
		ImageCanvas canvas = null;
		ImageWindow win = imag.getWindow();
		if (win != null) canvas = win.getCanvas();
		if (canvas == null)
			return false;
		else if (canvas instanceof OverlayCanvas)
			return true;
		else
			return false;
		}

	/**
	 * A handy routine which returns an OverlayCanvas, creating one if necessary.
	 */
	public static OverlayCanvas getOverlayCanvas (ImagePlus imag)
		{
		ImageCanvas canvas = null;
		if (OverlayCanvas.hasOverlayCanvas(imag))
			{
			ImageWindow win = imag.getWindow();
			return (OverlayCanvas)win.getCanvas();
			}
		else	{
			// IJ.log("Creating new OverlayCanvas...");
			OverlayCanvas canv = new OverlayCanvas (imag);
			if (imag.getStackSize () > 1)
				new StackWindow (imag,canv);
			else
				new ImageWindow (imag,canv);
			return canv;
			}
		}
	}
