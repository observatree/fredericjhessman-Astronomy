// Test_.java

import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;

public class Test_ implements PlugInFilter
	{
	ImagePlus img;

	public int setup (String arg, ImagePlus imp)
		{
		img = imp;
		return DOES_ALL;
		}

	public void run (ImageProcessor ip)
		{
		// GET HEADER
		String[] hdr = FitsJ.getHeader (img);

		// RESET FLOAT VALUE AND COMMENT
		hdr = FitsJ.setCard ("CHIPTEMP", -17.1717, "FAKE CCD TEMPERATURE!", hdr);

		// ADD COMMENT AFTER LAST CHANGED CARD
		hdr = FitsJ.addCommentAfter ("CHIPTEMP Butchered by FITS test!",hdr, FitsJ.findCardWithKey("CHIPTEMP", hdr));

		// RESET STRING VALUE BUT USE PRESENT COMMENT IF ANY
		hdr = FitsJ.setCard ("RDSPEED", "UltraFAST", null, hdr);

		// RESET INTEGER VALUE AND COMMENT
		hdr = FitsJ.setCard ("WCSDIM", 3, "FAKE WCSDIM!", hdr);

		// WRITE HEADER BACK TO IMAGE
		FitsJ.putHeader (img, hdr);
		}
	}
