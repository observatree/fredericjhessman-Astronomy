// GraphCanvas.java

import ij.gui.*;
import graphj.*;

public class GraphCanvas extends ImageCanvas
	{
	public GraphCanvas (ImagePlus imp)
		{
		super(imp);
		addImageDataset (imp);
		}
	}
