// Overlay_Shifter.java

import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;

import astroj.*;

/**
 * Simply shifts the pixel positions stored in each ApertureRoi of an OverlayCanvas.
 *
 * 2012-NOV-20 (FVH)
 *
 * @revision FVH
 * @date 2012-DEC-26
 * @changes Added scaling of radii.
 */
public class Overlay_Shifter implements PlugInFilter
	{
	ImagePlus imp;

	public int setup(String arg, ImagePlus imp)
		{
		this.imp = imp;
		return DOES_ALL;
		}

	public void run(ImageProcessor ip)
		{
		OverlayCanvas ocanvas = OverlayCanvas.getOverlayCanvas(imp);
		int n = ocanvas.numberOfRois();
		if (n == 0)
			{
			IJ.error("No measurements in overlay canvas!");
			return;
			}
		Roi[] rois = ocanvas.getRois();

		double dx=0.0;
		double dy=0.0;
		double[] radii = null;
		double ratio=1.0;
		for (int i=0; i < n; i++)
			{
			if (rois[i] instanceof ApertureRoi)
				{
				ApertureRoi roi = (ApertureRoi)rois[i];
				radii=roi.getRadii();
				break;
				}
			}
		if (radii == null)
			{
			IJ.error("No ApertureRoi in overlay canvas!");
			return;
			}
		double radius = radii[0];
	
		GenericDialog gd = new GenericDialog("Modify Overlay");
		gd.addNumericField("X-shift (>0 to the right) : ",dx,1);
		gd.addNumericField("Y-shift (>0 downwards)    : ",dy,1);
		gd.addNumericField("Centroid Radius           : ",radius,1);
		gd.showDialog();
		if (gd.wasCanceled()) return;
		dx = gd.getNextNumber();
		dy = gd.getNextNumber();
		radius = gd.getNextNumber();
		ratio = radius/radii[0];

		for (int i=0; i < n; i++)
			{
			if (rois[i] instanceof ApertureRoi)
				{
				ApertureRoi roi = (ApertureRoi)rois[i];
				double[] pos = roi.getCenter();
				radii = roi.getRadii();
				for (int j=0; j < 3; j++)
					radii[j] *= ratio;
				pos[0] += dx;
				pos[1] += dy;
				roi.setCenter(pos);
				roi.setRadii(radii);
				}
			}

		imp.updateAndDraw();
		}
	}
