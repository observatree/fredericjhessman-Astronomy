package astroj;

import java.awt.*;
import java.util.Properties;
import java.awt.image.*;
import ij.process.*;
import ij.measure.*;
import ij.plugin.WandToolOptions;
import ij.plugin.frame.Recorder;
import ij.plugin.frame.RoiManager;
import ij.macro.*;
import ij.gui.StackWindow;
import ij.gui.Roi;
import ij.*;
import ij.gui.ImageCanvas;
import ij.util.*;
import java.awt.event.*;
import java.util.*;
import java.awt.geom.*;

public class AstroCanvas extends OverlayCanvas {

        public static final int ROT_0 = 0;
        public static final int ROT_90 = -90;
        public static final int ROT_180 = -180;
        public static final int ROT_270 = -270;

        private boolean flipX;
        private boolean flipY;
        private int rotation;
        private boolean showZoom;
        private boolean showDir;
        private boolean showXY;

        //the internal states yielding proper orientation of a canvas given flipX, flipY, and rotation settings
        private boolean netRotate; // netRotate = rotate -90 degrees - other rotations are accomplished with netFlipX/Y
        private boolean netFlipX;  // netFlipX is the net required X-axis orientation
        private boolean netFlipY;  // netFlipY is the net required Y-axis orientation
        private boolean oldNetRotate;
        private int screenX;
        private int screenY;

        private boolean isTransformed;
        private AffineTransform canvTrans, invCanvTrans;

        int zoomIndicatorSize = 100, len = zoomIndicatorSize/2;
        double dirAngle = 0.0;
        double  sinl = 0, cosl = 0, cos7 = 0, sin7 = 0, cos4 = 0, sin4 = 0;
        int sinli = 0, cosli = 0;
        double dtr = Math.PI/180.0;
        double angr;
        double sina;
        double cosa;

        int[] xarrowheadX = new int[3];
        int[] yarrowheadX = new int[3];
        int[] xarrowheadY = new int[3];
        int[] yarrowheadY = new int[3];
        int[] xarrowheadE = new int[3];
        int[] yarrowheadE = new int[3];
        int[] xarrowheadN = new int[3];
        int[] yarrowheadN = new int[3];

		double aspectRatio;
		int h1, w1, h2, w2;
		int x1, y1,x2, y2, xc, yc;

        private Color zoomIndicatorColor = new Color(128, 128, 255);
        private Color axisIndicatorColor = new Color(0, 175, 0);
        private Color dirIndicatorColor = new Color(238, 0, 28);

        public AstroCanvas(ImagePlus imp) {
            super(imp);
        }


    public void setOrientation(boolean flipx, boolean flipy, int rot) {
        flipX = flipx;
        flipY = flipy;
        rotation = rot;
        setCanvasTransform();
    }

    public void setFlipX(boolean fx) {
        flipX = fx;
        setCanvasTransform();
    }

    public void setFlipY(boolean fy) {
        flipY = fy;
        setCanvasTransform();
    }

    public void setRotation(int rot) {
        rotation = rot;
        setCanvasTransform();
    }

    public void setShowZoom(boolean showZ) {
        showZoom = showZ;
    }
    public void setShowDir(boolean showD) {
        showDir = showD;
    }
    public void setShowXY(boolean showOrientation) {
        showXY = showOrientation;
    }

    public boolean getFlipX() {
        return flipX;
    }

    public boolean getFlipY() {
        return flipY;
    }

    public int getRotation() {
        return rotation;
    }


    void setCanvasTransform() {
        isTransformed = flipX || flipY || rotation != ROT_0;
        if ((rotation == ROT_0 && !flipX && !flipY) || (rotation == ROT_180 && flipX && flipY))
            {netRotate = false; netFlipX = false; netFlipY = false;}
        else if((rotation == ROT_0 && flipX && !flipY) || (rotation == ROT_180 && !flipX && flipY))
            {netRotate = false; netFlipX = true; netFlipY = false;}
        else if((rotation == ROT_0 && !flipX && flipY) || (rotation == ROT_180 && flipX && !flipY))
            {netRotate = false; netFlipX = false; netFlipY = true;}
        else if((rotation == ROT_0 && flipX && flipY) || (rotation == ROT_180 && !flipX && !flipY))
            {netRotate = false; netFlipX = true; netFlipY = true;}
        else if((rotation == ROT_90 && !flipX && !flipY) || (rotation == ROT_270 && flipX && flipY))
            {netRotate = true; netFlipX = false; netFlipY = false;}
        else if((rotation == ROT_90 && flipX && !flipY) || (rotation == ROT_270 && !flipX && flipY))
            {netRotate = true; netFlipX = true; netFlipY = false;}
        else if((rotation == ROT_90 && !flipX && flipY) || (rotation == ROT_270 && flipX && !flipY))
            {netRotate = true; netFlipX = false; netFlipY = true;}
        else if((rotation == ROT_90 && flipX && flipY) || (rotation == ROT_270 && !flipX && !flipY))
            {netRotate = true; netFlipX = true; netFlipY = true;}

        if (oldNetRotate != netRotate)
            {
//            int tempWidth = srcRect.width;
//            srcRect.x = (int)(srcRect.x + srcRect.width/2.0 - srcRect.height/2.0);
//            srcRect.y = (int)(srcRect.y + srcRect.height/2.0 - srcRect.width/2.0);
//            srcRect.width = srcRect.height;
//            srcRect.height = tempWidth;
//            if (netRotate)
//                magnification = magnification*getWidth()/getHeight();
//            else
//                magnification = magnification*getHeight()/getWidth();
            oldNetRotate = netRotate;
            }
    }

    public boolean[] getCanvasTransform() {
        return new boolean[] {netFlipX, netFlipY, netRotate};
    }

    public boolean getNetFlipX() {
        return netFlipX;
    }

    public boolean getNetFlipY() {
        return netFlipY;
    }

    public boolean getNetRotate() {
        return netRotate;
    }

    public boolean isTransformed() {
        return isTransformed;
    }

	/**Converts an offscreen x-coordinate to a screen x-coordinate.*/
    @Override
	public int screenX(int ox) {
        return        (int)((ox-srcRect.x)*magnification);
	}

	/**Converts an offscreen y-coordinate to a screen y-coordinate.*/
    @Override
	public int screenY(int oy) {
        return    (int)((oy-srcRect.y)*magnification);
	}

	/**Converts a floating-point offscreen x-coordinate to a screen x-coordinate.*/
    @Override
	public int screenXD(double ox) {
        return        (int)((ox-srcRect.x)*magnification);
	}

	/**Converts a floating-point offscreen x-coordinate to a screen x-coordinate.*/
    @Override
	public int screenYD(double oy) {
        return        (int)((oy-srcRect.y)*magnification);
	}

    public void setMouseScreenPosition(int sx, int sy) {
        screenX = sx;
        screenY = sy;
    }


	/**Converts screen x/y-coordinates to an offscreen x-coordinate (required for canvas rotation support).*/
    @Override
    public int offScreenX(int sx) {
        int sy = screenY;
        int aspectDelta = (imp.getWidth() - imp.getHeight())/2;
		if (!netFlipX && !netRotate) return srcRect.x + (int)(sx/magnification);
        else if (netFlipX && !netRotate) return srcRect.x + srcRect.width - (int)(sx/magnification) - 1;
        else if (!netFlipX && netRotate) return srcRect.x + aspectDelta + (int)((getHeight()-sy)/magnification);
        else return srcRect.x + aspectDelta + (int)(sy/magnification);    //(netFlipX && netRotate)
	}

	/**Converts screen x/y-coordinates to an offscreen y-coordinate (required for canvas rotation support).*/
	@Override
    public int offScreenY(int sy) {
        int sx = screenX;
        int aspectDelta = (imp.getWidth() - imp.getHeight())/2;
		if (!netFlipY && !netRotate) return srcRect.y + (int)(sy/magnification);
        else if (netFlipY && !netRotate) return srcRect.y + srcRect.height - (int)(sy/magnification) - 1;
        else if (!netFlipY && netRotate) return srcRect.y - aspectDelta + (int)(sx/magnification);
        else return srcRect.y - aspectDelta + (int)((getWidth()-sx)/magnification);    //(netFlipY && netRotate)
	}

	/**Converts screen x/y-coordinates to a floating-point offscreen x-coordinate (required for canvas rotation support).*/
	@Override
    public double offScreenXD(int sx) {
        int sy = screenY;
        double aspectDelta = (imp.getWidth() - imp.getHeight())/2.0;
        if (!netFlipX && !netRotate) return srcRect.x + (sx/magnification);
        else if (netFlipX && !netRotate) return srcRect.x + srcRect.width - (sx/magnification) - 1.0;
        else if (!netFlipX && netRotate) return srcRect.x + aspectDelta + ((getHeight()-sy)/magnification);
        else return srcRect.x + aspectDelta + (sy/magnification);    //(netFlipX && netRotate)
	}

	/**Converts screen x/y-coordinates to a floating-point offscreen y-coordinate (required for canvas rotation support).*/
	@Override
    public double offScreenYD(int sy) {
        int sx = screenX;
        double aspectDelta = (imp.getWidth() - imp.getHeight())/2.0;
		if (!netFlipY && !netRotate) return srcRect.y + (sy/magnification);
        else if (netFlipY && !netRotate) return srcRect.y + srcRect.height - (sy/magnification) - 1.0;
        else if (!netFlipY && netRotate) return srcRect.y - aspectDelta + (sx/magnification);
        else return srcRect.y - aspectDelta + ((getWidth()-sx)/magnification);    //(netFlipY && netRotate)
    }

//	/**Converts an offscreen x-coordinate to a screen x-coordinate.*/
//	public int screenXTN(int ox) {
//		return netFlipX ? (int)((ox - srcRect.x - srcRect.width)*magnification) :
//                (int)((ox-srcRect.x)*magnification);
//	}
//
//	/**Converts an offscreen y-coordinate to a screen y-coordinate.*/
//	public int screenYTN(int oy) {
//		return  netFlipY ? (int)((oy - srcRect.y - srcRect.height)*magnification) :
//                (int)((oy-srcRect.y)*magnification);
//	}
//
//
//	/**Converts a floating-point offscreen x-coordinate to a screen x-coordinate.*/
//	public int screenXDTN(double ox) {
//		return  netFlipX ? (int)((ox - srcRect.x - srcRect.width)*magnification) :
//                (int)((ox-srcRect.x)*magnification);
//	}
//
//	/**Converts a floating-point offscreen x-coordinate to a screen x-coordinate.*/
//	public int screenYDTN(double oy) {
//		return  netFlipY ? (int)((oy - srcRect.y - srcRect.height)*magnification) :
//                (int)((oy-srcRect.y)*magnification);
//	}
//
//	/**Converts an offscreen x-coordinate to a screen x-coordinate.*/
//	public int screenXT(int ox) {
//		return netFlipX ? (int)((-ox + srcRect.x + srcRect.width)*magnification) :
//                (int)((ox-srcRect.x)*magnification);
//	}
//
//	/**Converts an offscreen y-coordinate to a screen y-coordinate.*/
//	public int screenYT(int oy) {
//		return  netFlipY ? (int)((-oy + srcRect.y + srcRect.height)*magnification) :
//                (int)((oy-srcRect.y)*magnification);
//	}
//
//
//	/**Converts a floating-point offscreen x-coordinate to a screen x-coordinate.*/
//	public int screenXDT(double ox) {
//		return  netFlipX ? (int)((-ox + srcRect.x + srcRect.width)*magnification) :
//                (int)((ox-srcRect.x)*magnification);
//	}
//
//	/**Converts a floating-point offscreen x-coordinate to a screen x-coordinate.*/
//	public int screenYDT(double oy) {
//		return  netFlipY ? (int)((-oy + srcRect.y + srcRect.height)*magnification) :
//                (int)((oy-srcRect.y)*magnification);
//	}


    @Override
	    public void paint(Graphics g) {
        invCanvTrans = ((Graphics2D)g).getTransform();
		Roi roi = imp.getRoi();
		if (roi!=null || getShowAllROIs() || getOverlay()!=null) {
//			if (roi!=null) roi.updatePaste();
//            if (!IJ.isMacOSX() && imageWidth!=0) {
			if (imageWidth!=0) {
				paintDoubleBuffered(g);
				return;
			}
		}
		try {
			if (imageUpdated) {
				imageUpdated = false;
				imp.updateImage();
			}
			Java2.setBilinearInterpolation(g, Prefs.interpolateScaledImages);
			Image img = imp.getImage();
            if (isTransformed)
                flipAndRotateCanvas(g);
            if (!netRotate)
                {
                if (img!=null)
                    g.drawImage(img, 0, 0, (int)(srcRect.width*magnification), (int)(srcRect.height*magnification),
                    srcRect.x, srcRect.y, srcRect.x+srcRect.width, srcRect.y+srcRect.height, null);
                }
            else
                {
                if (img!=null)
                    g.drawImage(img, 0, 0, (int)(srcRect.height*magnification), (int)(srcRect.width*magnification),
                    (int)(srcRect.x+srcRect.width/2.0-srcRect.height/2.0),
                    (int)(srcRect.y+srcRect.height/2.0-srcRect.width/2.0),
                    (int)(srcRect.x+srcRect.width/2.0+srcRect.height/2.0),
                    (int)(srcRect.y+srcRect.height/2.0+srcRect.width/2.0), null);
                }
            OverlayCanvas oc = OverlayCanvas.getOverlayCanvas(imp);
            if (oc.numberOfRois() > 0) drawOverlayCanvas(g);
			if (getOverlay()!=null) ((ImageCanvas)this).drawOverlay(g);
			if (getShowAllROIs()) drawAllROIs(g);
			if (roi!=null) drawRoi(roi, g);

//			if (srcRect.width<imageWidth || srcRect.height<imageHeight)
            drawZoomIndicator(g);
		}
		catch(OutOfMemoryError e) {IJ.outOfMemory("Paint");}
    }

	// Use double buffer to reduce flicker when drawing complex ROIs.
	// Author: Erik Meijering
	void paintDoubleBuffered(Graphics g) {
		final int srcRectWidthMag = (int)(srcRect.width*magnification);
		final int srcRectHeightMag = (int)(srcRect.height*magnification);
		if (offScreenImage==null || offScreenWidth!=srcRectWidthMag || offScreenHeight!=srcRectHeightMag) {
			offScreenImage = createImage(srcRectWidthMag, srcRectHeightMag);
			offScreenWidth = srcRectWidthMag;
			offScreenHeight = srcRectHeightMag;
		}
		Roi roi = imp.getRoi();
		try {
			if (imageUpdated) {
				imageUpdated = false;
				imp.updateImage();
			}
			Graphics offScreenGraphics = offScreenImage.getGraphics();
			Java2.setBilinearInterpolation(offScreenGraphics, Prefs.interpolateScaledImages);
			Image img = imp.getImage();
            if (isTransformed)
                flipAndRotateCanvas(offScreenGraphics);
            if (!netRotate)
                {
                if (img!=null)
                    offScreenGraphics.drawImage(img, 0, 0, (int)(srcRect.width*magnification), (int)(srcRect.height*magnification),
                    srcRect.x, srcRect.y, srcRect.x+srcRect.width, srcRect.y+srcRect.height, null);
                }
            else
                {
                if (img!=null)
                    offScreenGraphics.drawImage(img, 0, 0, (int)(srcRect.height*magnification), (int)(srcRect.width*magnification),
                    (int)(srcRect.x+srcRect.width/2.0-srcRect.height/2.0),
                    (int)(srcRect.y+srcRect.height/2.0-srcRect.width/2.0),
                    (int)(srcRect.x+srcRect.width/2.0+srcRect.height/2.0),
                    (int)(srcRect.y+srcRect.height/2.0+srcRect.width/2.0), null);
                }
            OverlayCanvas oc = OverlayCanvas.getOverlayCanvas(imp);
            if (oc.numberOfRois() > 0) drawOverlayCanvas(offScreenGraphics);
			if (getOverlay()!=null) ((ImageCanvas)this).drawOverlay(offScreenGraphics);
			if (getShowAllROIs()) drawAllROIs(offScreenGraphics);
			if (roi!=null) drawRoi(roi, offScreenGraphics);

//			if (srcRect.width<imageWidth ||srcRect.height<imageHeight)
			drawZoomIndicator(offScreenGraphics);
			if (IJ.debugMode) showFrameRate(offScreenGraphics);
			g.drawImage(offScreenImage, 0, 0, null);
            
            
		}
		catch(OutOfMemoryError e) {IJ.outOfMemory("Paint");}
	}

    public void flipAndRotateCanvas(Graphics g) {

        Image img = imp.getImage();
        int aspectDelta = imp.getWidth() - imp.getHeight();
        if (!netRotate)
            ((Graphics2D)g).translate(srcRect.width*magnification/2.0, srcRect.height*magnification/2.0);
        else
            ((Graphics2D)g).translate((srcRect.height+aspectDelta)*magnification/2.0, srcRect.width*magnification/2.0);
        
        if (netRotate) ((Graphics2D)g).rotate(Math.toRadians(ROT_90));
        if (netFlipX) ((Graphics2D)g).scale(-1.0,1.0);
        if (netFlipY) ((Graphics2D)g).scale(1.0, -1.0);
        

        if (!netRotate)
            ((Graphics2D)g).translate(-srcRect.width*magnification/2.0, -srcRect.height*magnification/2.0);
        else
            {
            ((Graphics2D)g).translate((-srcRect.height+(netFlipX ? -aspectDelta : aspectDelta))*magnification/2.0, -srcRect.width*magnification/2.0);
            }
        canvTrans = ((Graphics2D)g).getTransform();
    }

	protected void drawZoomIndicator(Graphics g) {    //*** nothing specified
        ((Graphics2D)g).setTransform(invCanvTrans);
        g.setFont (new Font ("SansSerif", Font.PLAIN, 12));
		g.setColor(zoomIndicatorColor);
		((Graphics2D)g).setStroke(Roi.onePixelWide);
		w2 = (int)(w1*((double)srcRect.width/imageWidth));
		h2 = (int)(h1*((double)srcRect.height/imageHeight));
		if (w2<1) w2 = 1;
		if (h2<1) h2 = 1;
		x2 = (int)(w1*((double)srcRect.x/imageWidth));
		y2 = (int)(h1*((double)srcRect.y/imageHeight));
        if (showZoom)
            {
            g.drawRect(x1, y1, w1, h1);
            if (srcRect.width<imageWidth || srcRect.height<imageHeight)
                {
                int x3 = netFlipX ? x1+w1-x2-w2 : x1+x2;
                int y3 = netFlipY ? y1+h1-y2-h2 : y1+y2;
                if (w2*h2<=200 || w2<10 || h2<10)
                    g.fillRect(x3, y3, w2, h2);
                else
                    g.drawRect(x3, y3, w2, h2);
                }
            }
        
        if (showDir)
            {
            g.setColor(dirIndicatorColor);
            g.drawLine(xc, yc, xc-cosli, yc+sinli);
            g.fillPolygon(xarrowheadE, yarrowheadE, 3);
            g.drawString("E", xc-cosli+2, yc+sinli-5);
            g.drawLine(xc, yc, xc-sinli, yc-cosli);
            g.fillPolygon(xarrowheadN, yarrowheadN, 3);
            g.drawString("N", xc-sinli-13, yc-cosli+10);
            }

        if (showXY)
            {
            int len2 = len;
            if (showDir) len2 -= 10;
            if (len2 < 15) len2 = 15;
            g.setColor(axisIndicatorColor);
            if (!netFlipX)
                {
                g.drawLine(xc, yc, xc+len2, yc);
                g.drawString("X", xc+len2-8, yc-5);
                xarrowheadX[0] = xc+len2; xarrowheadX[1] = xc+len2-7; xarrowheadX[2] = xc+len2-7;
                yarrowheadX[0] = yc; yarrowheadX[1] = yc+4; yarrowheadX[2] = yc-4;
                }
            else
                {
                g.drawLine(xc, yc, xc-len2, yc);
                g.drawString("X", xc-len2+2, yc-5);
                xarrowheadX[0] = xc-len2; xarrowheadX[1] = xc-len2+7; xarrowheadX[2] = xc-len2+7;
                yarrowheadX[0] = yc; yarrowheadX[1] = yc+4; yarrowheadX[2] = yc-4;
                }
            if (!netFlipY)
                {
                g.drawLine(xc, yc, xc, yc-len2);
                g.drawString("Y", xc-12 , yc-len2+11);
                xarrowheadY[0] = xc; xarrowheadY[1] = xc-4; xarrowheadY[2] = xc+4;
                yarrowheadY[0] = yc-len2; yarrowheadY[1] = yc-len2+7; yarrowheadY[2] = yc-len2+7;
                }
            else
                {
                g.drawLine(xc, yc, xc, yc+len2);
                g.drawString("Y", xc-12 , yc+len2+1);
                xarrowheadY[0] = xc; xarrowheadY[1] = xc-4; xarrowheadY[2] = xc+4;
                yarrowheadY[0] = yc+len2; yarrowheadY[1] = yc+len2-7; yarrowheadY[2] = yc+len2-7;
                }
            g.fillPolygon(xarrowheadX, yarrowheadX, 3);
            g.fillPolygon(xarrowheadY, yarrowheadY, 3);

            }
       
	}

    void updateZoomBoxParameters()
        {
        angr = dirAngle*dtr;
        sina = Math.sin(angr);
        cosa = Math.cos(angr);
        imageHeight = imp.getHeight();
        imageWidth = imp.getWidth();
		aspectRatio = (double)imageHeight/(double)imageWidth;
		h1 = zoomIndicatorSize;
        w1 = (int)(h1/aspectRatio);
		if (w1<50) w1 = 50;
		if (zoomIndicatorColor==null)
			zoomIndicatorColor = new Color(128, 128, 255);
        x1 = 10;
        y1 = 10;
        xc = (2*x1 + w1)/2;
        yc = (2*y1 + h1)/2;
        len = w1 > h1 ? (h1/2) - 5 : (w1/2) - 5;
        sinl = Math.sin(dirAngle*dtr)*(double)len;
        cosl = Math.cos(dirAngle*dtr)*(double)len;
        sinli = (int)Math.round(sinl);
        cosli = (int)Math.round(cosl);

        xarrowheadE[0] = xc-cosli;
        yarrowheadE[0] = yc+sinli;
        xarrowheadE[1] = xc+(int)Math.round(-cosa*len+cosa*7.-sina*4.);
        yarrowheadE[1] = yc+(int)Math.round(sina*len-sina*7.-cosa*4.);
        xarrowheadE[2] = xc+(int)Math.round(-cosa*len+cosa*7.+sina*4.);
        yarrowheadE[2] = yc+(int)Math.round(sina*len-sina*7.+cosa*4.);
        xarrowheadN[0] = xc-sinli;
        yarrowheadN[0] = yc-cosli;
        xarrowheadN[1] = xc+(int)Math.round(-sina*len+sina*7.-cosa*4.);
        yarrowheadN[1] = yc+(int)Math.round(-cosa*len+cosa*7.+sina*4.);
        xarrowheadN[2] = xc+(int)Math.round(-sina*len+sina*7.+cosa*4.);
        yarrowheadN[2] = yc+(int)Math.round(-cosa*len+cosa*7.-sina*4.);
        }

    public void repaint(int x, int y, int width, int height) {
//        IJ.log("x="+x+"   newX="+(getFlipX() ? getWidth() - x : x));
        x = netFlipX ? getWidth() - x - width: x;
        y = netFlipY ? getHeight() - y - height: y;
        super.repaint(x, y, width, height);
    }

    } // AstroCanvas class