// Read_MeasurementTable.java

import java.awt.*;

import ij.*;
import ij.gui.*;
import ij.io.*;
import ij.plugin.*;
import ij.process.*;

import astroj.MeasurementTable;

public class Read_MeasurementTable implements PlugIn
	{
	public void run(String arg)
		{
		OpenDialog od = new OpenDialog("Select Measurement Table to be opened",null);
		String dir = od.getDirectory();
		String filename = od.getFileName();
		// IJ.showMessage ("open "+dir+filename);
		MeasurementTable table = MeasurementTable.getTableFromFile (dir+filename);
		if (table == null)
			IJ.showMessage ("Unable to open MeasurementTable "+filename);
		else
			table.show();
		}
	}
