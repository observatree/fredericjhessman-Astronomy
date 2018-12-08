package astroj;

import ij.*;
import ij.io.*;
import ij.gui.*;
import ij.process.*;

import ij.macro.Interpreter;
//import ij.measure.*;
import ij.measure.Calibration;
import ij.plugin.FolderOpener;
//import ij.plugin.filter.*;
//import ij.plugin.frame.*;
//import ij.macro.Interpreter;

import java.awt.*;
import java.awt.Graphics.*;
import java.awt.event.*;
import java.awt.event.MouseEvent.*;
//import java.awt.image.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.event.*;
import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;
//import javax.swing.border.TitledBorder;
//import javax.swing.event.*;

import java.util.*;
//import java.util.Properties;
//import java.util.EventListener;

import java.lang.Math.*;
import java.text.*;

import bislider.com.visutools.nav.bislider.*;
//import astroj.*;



/**
 *
 * @author Karen
 */
public class AstroStackWindow extends StackWindow implements LayoutManager, ActionListener,
                                   MouseListener, MouseMotionListener, MouseWheelListener,
                                   KeyListener, ItemListener {

            AstroCanvas ac;
            Component[] stackSliders;
            static final int MIN_WIDTH = 128;
            static final int MIN_HEIGHT = 32;
            static final int MIN_FRAME_WIDTH = 500;
            static final int MAX_FRAME_HEIGHT_PADDING = 300;
            static final boolean DRAGGING = true;
            static final boolean NOT_DRAGGING = false;
            static final boolean REFRESH = true;
            static final boolean NEW = false;
            static final boolean RESIZE = true;
            static final boolean NORESIZE = false;
            static final boolean MOUSECLICK = true;
            static final boolean WHEEL = false;
            static final int BISLISER_SEGMENTS = 255;
            static final boolean IMAGE_UPDATE = true;
            static final boolean NO_IMAGE_UPDATE = false;

            int slice=0;
            int stackSize=0;
            double oldX;
            double oldY;
            double magnification = 0.5;
            Rectangle rct;
            DmsFormat form;
            DecimalFormat fourPlaces = new DecimalFormat("###,###,##0.0000");
            DecimalFormat threePlaces = new DecimalFormat("0.000");
            DecimalFormat noPlaces = new DecimalFormat("###,###,##0");
            DecimalFormat sixPlaces = new DecimalFormat("0.000000");
            DecimalFormat uptoSixPlaces = new DecimalFormat("0.######");
            ImageStatistics stats, stats2;
            Dimension screenDim;

            int startDragX, startDragY, endDragX, endDragY;
            int origCanvasHeight, origCanvasWidth;
            int startDragSubImageX, startDragSubImageY;
            int startDragScreenX, startDragScreenY;
            int lastScreenX, lastScreenY;
            int lastImageX, lastImageY;
            int frameLocationX, frameLocationY;
            int newPositionX, newPositionY;
            int icHeight, icWidth, ipWidth, ipHeight;
            int astronomyToolId, apertureToolId, zoomToolId, panToolId, currentToolId;
            int ocanvasHeight, ocanvasWidth;
            int imageHeight, imageWidth, winWidth, winHeight;
            int otherPanelsHeight;
            int sliderScale;
            int count = 0;
            int newicWidth;
            int currentSlice;
            int oldICHeight=0;
            int oldICWidth=0;

            int radius = 20, rBack1 = 30, rBack2 = 40;


            int hgap = 0;
            int vgap = 0;
            int[] histogram;
            double histMax = 0.0;
            double[] logHistogram;
            boolean maxBoundsReset = false;
            int resetMaxBoundsCount;
            double dstWidth;
            double dstHeight;
            double prevBarHeight = 0.0;
            Rectangle srcRect;
            int imageEdgeX1, imageEdgeX2, imageEdgeY1, imageEdgeY2;
            long setMaxBoundsTime;

            double sliderMultiplier, sliderShift;
            double prevMag;
            double startMinDisplayValue, startMaxDisplayValue;
            double imageMedian, min, max, minValue, maxValue, meanValue, stdDevValue;
            double scaleMin, scaleMax, fixedMinValue, fixedMaxValue;
            double brightness, contrast, brightstepsize, contrastStepSize;

            double savedMag = 1.0;
            double savedMin = 0.0;
            double savedMax = 255.0;
            double pixelScale = 0.0;
            int savedICHeight = 500;
            int savedICWidth = 500;
            int savedPanX = 0;
            int savedPanY = 0;
            int savedPanHeight = 500;
            int savedPanWidth = 500;
            int savedIpHeight = 0;
            int savedIpWidth = 0;

            public boolean minMaxChanged = false;
            boolean newClick;
            boolean button2Drag;
            boolean alreadyCustomStackWindow = false;
            boolean goodWCS = false;
            boolean firstClick = true;
            boolean useSexagesimal = true;
            boolean startupPrevSize = true;
            boolean showPhotometer = true;
            boolean prevShiftDownState = false;
            public boolean startupAutoLevel = true;
            public boolean isReady = false;
            boolean startupPrevPan = false;
            boolean startupPrevZoom = false;
            boolean startupPrevLevels = false;
            boolean rememberWindowLocation = true;
            boolean writeMiddleClickValues = true;
            boolean writeMiddleDragValues = true;
            boolean astronomyMode = true;
            boolean autoConvert = true;
            boolean firstTime = true;
            boolean mouseDown = false;
            boolean stackRotated = false;
            boolean refresh2 = false;
            boolean fillNotFit = false;
            boolean useInvertingLut = false;
            boolean redrawing = false;
            boolean removeBackStars = true;
            boolean apertureChanged = false;
            boolean showZoom = true;
            boolean showDir = true;
            boolean showXY = true;
            boolean useFixedMinMaxValues = false;
            boolean dataRotated = false;
            boolean shiftAndControlWasDown = false;

            boolean invertX = false;
            boolean invertY = false;
//            boolean prevInvertX = false;
//            boolean prevInvertY = false;
            int rotation = AstroCanvas.ROT_0;
//            int prevRotation = 0;
            boolean netFlipX, netFlipY, netRotate;
            boolean flipDataX, flipDataY, rotateDataCW, rotateDataCCW;

            String IJVersion = IJ.getVersion();
            String impTitle;

            TimerTask rotateTask = null, photometerTask = null;
            java.util.Timer rotateTaskTimer = null, photometerTaskTimer = null;

            JScrollBar channelSelector, sliceSelector, frameSelector;
            Thread thread;
            volatile boolean done;
            boolean hyperStack;
            int nChannels=1, nSlices=1, nFrames=1;
            int c=1, z=1, t=1;
            int scrollBarTotal;

            MouseWheelListener[] mwl;
            MouseWheelListener[] icmwl;
            MouseMotionListener[] mml;
            MouseListener[] ml;
            Toolbar toolbar;

            double[] radec, startRadec;

            double[] xy = new double[2];
            WCS wcs;
            Photometer photom;
            Overlay apertureOverlay = new Overlay();
            Roi radiusRoi;
            Roi rBack1Roi;
            Roi rBack2Roi;

            double[] crpix = null;
            double[][] cd = null;
            int[] npix = null;

            Font p12;
            Font p13;
            Font b12;

            Color mouseApertureColor = new Color(128, 128, 255);

            MenuBar mainMenuBar = new MenuBar();
            Panel mainPanel;
            JPanel infoPanel;
            JPanel topPanelA;
            JPanel zoomPanel;
            JPanel topPanelB, topPanelBC;
            JPanel bottomPanelB;
            JPanel canvasPanel;
            JTextField lengthLabel, peakLabel, infoTextField;

            Menu fileMenu, preferencesMenu, viewMenu, editMenu, processMenu, analyzeMenu;

            MenuItem exitMenuItem, flipDataXMenuItem, flipDataYMenuItem, rotateDataCWMenuItem, rotateDataCCWMenuItem;
            MenuItem openMenuItem, openInNewWindowMenuItem, openSeqMenuItem,openSeqInNewWindowMenuItem;
            MenuItem saveMenuItem, saveFitsMenuItem, saveStackSequenceMenuItem, clearOverlayMenuItem;
            MenuItem saveTiffMenuItem, saveJpegMenuItem, savePngMenuItem, saveBmpMenuItem, saveGifMenuItem, saveAviMenuItem;
            MenuItem dirAngleMenuItem;

            MenuItem stackSorterMenuItem, alignStackMenuItem, imageStabilizerMenuItem, imageStabilizerApplyMenuItem;
            MenuItem normalizeStackMenuItem, shiftImageMenuItem, editFitsHeaderMenuItem, staticProfilerMenuItem;
            MenuItem apertureSettingsMenuItem, multiApertureMenuItem, multiPlotMenuItem, openMeasurementsTableMenuItem, threeDSurfacePlotMenuItem;
            MenuItem bestEdgesMenuItem, imageCalcMenuItem, seeingProfileMenuItem, dynamicProfilerMenuItem, azimuthalAverageMenuItem;
            MenuItem measurementSettingsMenuItem, measurementMenuItem, smoothMenuItem, sharpenMenuItem, removeOutliersMenuItem;
            MenuItem dataReducerMenuItem, selectBestFramesMenuItem, setPixelScaleMenuItem, setZoomIndicatorSizeMenuItem;

            CheckboxMenuItem startupAutoLevelRB, usePreviousLevelsRB, useFullRangeRB,negativeDisplayRB;
            CheckboxMenuItem invertNoneRB, invertXRB, invertYRB, invertXYRB;
            CheckboxMenuItem rotate0RB, rotate90RB, rotate180RB, rotate270RB;
            CheckboxMenuItem showZoomCB, showDirCB, showXYCB, useFixedMinMaxValuesCB;
            ButtonGroup contrastGroup, invertGroup, rotationGroup;
            CheckboxMenuItem autoConvertCB, usePreviousSizeCB, usePreviousPanCB, usePreviousZoomCB,
                    rememberWindowLocationCB, useSexagesimalCB, writeMiddleClickValuesCB,
                    writeMiddleDragValuesCB, showPhotometerCB, removeBackStarsCB;

            JButton buttonAdd32768, buttonSub32768, buttonFit, buttonHeader, buttonLUT, buttonZoomInFast, buttonZoomIn, buttonZoomOut;
            JButton buttonNegative, buttonFlipX, buttonFlipY, buttonRotCCW, buttonRotCW, buttonAutoLevels;
            JSlider minSlider, maxSlider;
            JTextField minValueTextField, maxValueTextField, minTextField, maxTextField, meanTextField;
            JTextField valueTextField, RATextField, DecTextField, peakTextField;
            JTextField fitsXTextField, fitsYTextField, lengthTextField;
            JTextField ijXTextField, ijYTextField;
            BiSlider minMaxBiSlider;


     public AstroStackWindow(ImagePlus imp, AstroCanvas ac, boolean refresh, boolean resize) {

                super(imp, ac);

//                SET DEFAULT SYSTEM LOOK AND FEEL
//                UIManager.LookAndFeelInfo[] laf = UIManager.getInstalledLookAndFeels();
//                for (int i = 0 ; i < laf.length; i++)
//                IJ.write(""+laf[i]);
//                System.setProperty("com.apple.laf.useScreenMenuBar", "false");
//                System.setProperty("apple.awt.graphics.UseQuartz","false");
                if (IJ.isWindows())
                        {
                        try {UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");}
//                        try {UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");}
//                        try {UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}
                        catch (Exception e) { }
                        }
                else if (IJ.isLinux())
                        {
                        try {UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");}
                        catch (Exception e) { }
                        }
//                    try {UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");}
//                    try {UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");}
//                    try {UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");}
                else if (IJ.isMacOSX())
                        {

                        try {UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");}
//                        try {UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}
                        catch (Exception e) { }
                        }
//                System.setProperty("apple.laf.useScreenMenuBar", "false");
//                System.setProperty("com.apple.macos.useScreenMenuBar", "false");
                this.imp = imp;
                this.ac = ac;

                super.hasMenus = true;

//                super.ic.setBackground(Color.WHITE);

                getStatistics();
                minValue = stats.min;
                maxValue = stats.max;
                min = minValue;
                max = maxValue;
                ImageProcessor ip = imp.getProcessor();

                if (imp.getType()==ImagePlus.COLOR_RGB)
                    {
                    ip.snapshot();
                    }
                stackSize = imp.getStackSize();

                getPrefs();

                if (imp.getType() == ImagePlus.COLOR_256 || imp.getType() == ImagePlus.COLOR_RGB || imp.getType() == ImagePlus.GRAY8)
                    {
                    useFixedMinMaxValues = false;
                    minValue = 0;
                    maxValue = 255;
                    }
                else
                    {
                    maxValue = useFixedMinMaxValues ? fixedMaxValue : stats.max;
                    minValue = useFixedMinMaxValues ? fixedMinValue : stats.min;
                    if (imp.getType() == ImagePlus.GRAY16 && maxValue - minValue < 256)
                         maxValue = minValue + 255;
                    }
                impTitle = imp.getTitle();

                ac.setOrientation(invertX, invertY, rotation);
                netFlipX = ac.getNetFlipX();
                netFlipY = ac.getNetFlipY();
                netRotate = ac.getNetRotate();
                ac.setShowZoom(showZoom);
                ac.setShowDir(showDir);
                ac.setShowXY(showXY);


//                adjustImageRotation(NO_IMAGE_UPDATE);
                
                wcs = new WCS(imp);
                goodWCS = wcs.hasWCS();

                photom = new Photometer (imp.getCalibration());
                photom.setSourceApertureRadius (radius);
                photom.setBackgroundApertureRadii (rBack1,rBack2);
                photom.setRemoveBackStars(removeBackStars);

                imp.setOverlay(apertureOverlay);
                

                if (IJ.isWindows())
                    {
                    p12 = new Font("Dialog",Font.PLAIN,12);
                    p13 = new Font("Dialog",Font.PLAIN,13);
                    b12 = new Font("Dialog",Font.BOLD,12);
                    }
                else
                    {
                    p12 = new Font("Dialog",Font.PLAIN,11);
                    p13 = new Font("Dialog",Font.PLAIN,12);
                    b12 = new Font("Dialog",Font.BOLD,11);
                    }

                winWidth = this.getWidth();
                winHeight = this.getHeight();

                if(!startupPrevSize && resize && IJVersion.compareTo("1.42q") > 0)
                    {
                    ac.setDrawingSize((int)(ac.getWidth()*0.9), (int)(ac.getHeight()*0.9));
                    ac.setMagnification(ac.getMagnification()*0.9);
                    }
                else if(startupPrevSize)
                    {
                    ac.setDrawingSize((int)((double)savedICHeight*(double)imp.getWidth()/(double)imp.getHeight()),savedICHeight);
                    ac.setMagnification((double)savedICHeight/(double)imp.getHeight());
                    }

                magnification = ac.getMagnification();
                icWidth = ac.getWidth();
                icHeight = ac.getHeight();
                ipWidth = ip.getWidth();
                ipHeight = ip.getHeight();

                if (icWidth < MIN_FRAME_WIDTH)
                    {
                    newicWidth = MIN_FRAME_WIDTH;
                    double mag = Math.max((double)newicWidth/(double)ipWidth, (double)icHeight/(double)ipHeight);
                    ac.setDrawingSize((int)(ipWidth*mag), (int)(ipHeight*mag));
                    ac.setMagnification(mag);
                    }
                screenDim = Toolkit.getDefaultToolkit().getScreenSize();
                if (ac.getHeight() > screenDim.height - MAX_FRAME_HEIGHT_PADDING)
                    {
                    ac.setMagnification((double)(screenDim.height - MAX_FRAME_HEIGHT_PADDING)/(double)ip.getHeight());
                    ac.setDrawingSize((int)Math.max(((screenDim.height - MAX_FRAME_HEIGHT_PADDING)*(double)ip.getWidth()/
                                           (double)ip.getHeight()), MIN_FRAME_WIDTH), screenDim.height - MAX_FRAME_HEIGHT_PADDING);
                    }

                magnification = ac.getMagnification();
                icWidth = ac.getWidth();
                icHeight = ac.getHeight();
                ipWidth = ip.getWidth();
                ipHeight = ip.getHeight();

                ac.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

                buildAstroWindow();

                if (startupPrevPan && ipWidth == savedIpWidth && ipHeight == savedIpHeight )
                    {
                    double w = (double)icWidth/magnification;
                    if (w*magnification<icWidth) w++;
                    double h = (double)icHeight/magnification;
                    if (h*magnification<icHeight) h++;
                    Rectangle rect = new Rectangle(savedPanX, savedPanY, savedPanWidth, savedPanHeight);
                    ac.setSourceRect(rect);
                    ac.setMagnification((double)ac.getHeight()/(double)rect.height);
                    ac.setDrawingSize((int)((double)ac.getHeight()*(double)rect.width/(double)rect.height), ac.getHeight());
                    if (rect.x<0 || rect.y<0 || rect.x+w>ipWidth || rect.y+h>ipHeight)
                        {
                        clearAndPaint();
                        }
                    }

                setupListeners();
                setImageEdges();
                addWindowListener(new WindowAdapter(){
                    @Override
                    public void windowClosing(WindowEvent e){
                    saveAndClose(false);}});


                setLayout(this);
                IJ.wait(200); //an attempt to work around window non-display
                
//                doLayout();
                isReady = true;
                startDragScreenX = (int)(ac.getX() + ac.getWidth()/2.0);
                startDragScreenY = (int)(ac.getY() + ac.getHeight()/2.0);
//                IJ.error("pause");
                if (startupAutoLevel)
                    {
                    setAutoLevels(null);
                    }
                else if (startupPrevLevels)
                    {
                    min = savedMin;
                    max = savedMax;
                    updatePanelValues();
                    }
                else
                    {
                    min = minValue;
                    max = maxValue;
                    updatePanelValues();
                    }
                ac.updateZoomBoxParameters();
                setVisible(true);
                
    }

        void saveAndClose(boolean cleanWindow)
                {
                savePrefs();
                toolbar.removeMouseListener(toolbarMouseListener);
                ac.removeMouseWheelListener(this);
                ac.removeMouseListener(this);
                ac.removeMouseMotionListener(this);
                ac.removeKeyListener(this);
                imp.changes = false;
                imp.unlock();
                wcs = null;
                photom = null;
                if (cleanWindow)
                    {
                    imp.close();
                    WindowManager.removeWindow(this);
                    }
                }

        void updatePhotometerOverlay()
                {
                apertureChanged = Prefs.get("setaperture.aperturechanged", apertureChanged);

                if ((apertureOverlay.size() == 3) && !apertureChanged)
                    {
                    apertureOverlay.setStrokeColor(mouseApertureColor);
                    radiusRoi.setLocation(lastImageX-(int)photom.radius, lastImageY-(int)photom.radius);
                    rBack1Roi.setLocation(lastImageX-(int)photom.rBack1, lastImageY-(int)photom.rBack1);
                    rBack2Roi.setLocation(lastImageX-(int)photom.rBack2, lastImageY-(int)photom.rBack2);
                    Graphics g = ac.getGraphics();
                    ac.paint(g);
                    }
                else
                    {
                    Prefs.set("setaperture.aperturechanged", false);
                    apertureOverlay.clear();
                    radius = (int)Prefs.get("aperture.radius", radius);
                    rBack1 = (int)Prefs.get("aperture.rback1",rBack1);
                    rBack2 = (int)Prefs.get("aperture.rback2",rBack2);
                    removeBackStars = Prefs.get("Astronomy_Tool.removeBackStars", removeBackStars);
                    photom.setSourceApertureRadius(radius);
                    photom.setBackgroundApertureRadii(rBack1,rBack2);
                    photom.setRemoveBackStars(removeBackStars);
                    apertureOverlay.setStrokeColor(mouseApertureColor);
                    radiusRoi = new ij.gui.OvalRoi(lastImageX-(int)photom.radius, (int)(lastImageY-photom.radius), (int)photom.radius*2, (int)photom.radius*2);
                    rBack1Roi = new ij.gui.OvalRoi(lastImageX-(int)photom.rBack1, (int)(lastImageY-photom.rBack1), (int)photom.rBack1*2, (int)photom.rBack1*2);
                    rBack2Roi = new ij.gui.OvalRoi(lastImageX-(int)photom.rBack2, (int)(lastImageY-photom.rBack2), (int)photom.rBack2*2, (int)photom.rBack2*2);
                    apertureOverlay.add(radiusRoi);
                    apertureOverlay.add(rBack1Roi);
                    apertureOverlay.add(rBack2Roi);
                    imp.setOverlay(apertureOverlay);
                    }
                }

            void buildAstroWindow() {
                mainMenuBar = new MenuBar();
//                JPopupMenu.setDefaultLightWeightPopupEnabled(false);

//------FILE menu---------------------------------------------------------------------

                fileMenu = new Menu("   File");

                openMenuItem = new MenuItem("Open file...");
                openMenuItem.addActionListener(this);
                if (stackSize != 1) openMenuItem.setEnabled(false);
                fileMenu.add(openMenuItem);

                openInNewWindowMenuItem = new MenuItem("Open file in new window...");
                openInNewWindowMenuItem.addActionListener(this);
                fileMenu.add(openInNewWindowMenuItem); 

//                openSeqMenuItem = new MenuItem("Open sequence...");
//                openSeqMenuItem.addActionListener(this);
//                fileMenu.add(openSeqMenuItem);

                openSeqInNewWindowMenuItem = new MenuItem("Open sequence in new window...");
                openSeqInNewWindowMenuItem.addActionListener(this);
                fileMenu.add(openSeqInNewWindowMenuItem);

                openMeasurementsTableMenuItem = new MenuItem("Open measurements table...");
                openMeasurementsTableMenuItem.addActionListener(this);
                fileMenu.add(openMeasurementsTableMenuItem);

                fileMenu.addSeparator();

                saveMenuItem = new MenuItem("Save");
                saveMenuItem.addActionListener(this);
                fileMenu.add(saveMenuItem);

                saveFitsMenuItem = new MenuItem("Save image/slice as FITS...");
                saveFitsMenuItem.addActionListener(this);
                fileMenu.add(saveFitsMenuItem);

                saveTiffMenuItem = new MenuItem("Save image/stack as TIFF...");
                saveTiffMenuItem.addActionListener(this);
                fileMenu.add(saveTiffMenuItem);

                saveJpegMenuItem = new MenuItem("Save image/slice as JPEG...");
                saveJpegMenuItem.addActionListener(this);
                fileMenu.add(saveJpegMenuItem);

                saveGifMenuItem = new MenuItem("Save image/stack as GIF...");
                saveGifMenuItem.addActionListener(this);
                fileMenu.add(saveGifMenuItem);

                savePngMenuItem = new MenuItem("Save image/slice as PNG...");
                savePngMenuItem.addActionListener(this);
                fileMenu.add(savePngMenuItem);

                saveBmpMenuItem = new MenuItem("Save image/slice as BMP...");
                saveBmpMenuItem.addActionListener(this);
                fileMenu.add(saveBmpMenuItem);

                saveAviMenuItem = new MenuItem("Save image/stack as AVI...");
                saveAviMenuItem.addActionListener(this);
                fileMenu.add(saveAviMenuItem);

                saveStackSequenceMenuItem = new MenuItem("Save stack as sequence...");
                saveStackSequenceMenuItem.addActionListener(this);
                fileMenu.add(saveStackSequenceMenuItem);

                fileMenu.addSeparator();

                exitMenuItem = new MenuItem("Close Window");
                exitMenuItem.addActionListener(this);
                fileMenu.add(exitMenuItem);

                mainMenuBar.add(fileMenu);

//------Preferences menu---------------------------------------------------------------------

                preferencesMenu = new Menu ("Preferences");

                autoConvertCB = new CheckboxMenuItem("Use astro-window when images are opened", autoConvert);
                autoConvertCB.addItemListener(this);
                preferencesMenu.add(autoConvertCB);

                preferencesMenu.addSeparator();

                preferencesMenu.add("--When an image is opened or modified use:--");

                startupAutoLevelRB = new CheckboxMenuItem("auto brightness & contrast", startupAutoLevel);
                startupAutoLevelRB.addItemListener(this);
                preferencesMenu.add(startupAutoLevelRB);

                usePreviousLevelsRB = new CheckboxMenuItem("fixed brightness & contrast", !startupAutoLevel && startupPrevLevels);
                usePreviousLevelsRB.addItemListener(this);
                preferencesMenu.add(usePreviousLevelsRB);

                useFullRangeRB = new CheckboxMenuItem("full dynamic range", !startupAutoLevel && !startupPrevLevels);
                useFullRangeRB.addItemListener(this);
                preferencesMenu.add(useFullRangeRB);

//                contrastGroup = new ButtonGroup();
//                contrastGroup.add(startupAutoLevelRB);
//                contrastGroup.add(usePreviousLevelsRB);
//                contrastGroup.add(useFullRangeRB);

                preferencesMenu.addSeparator();

                useFixedMinMaxValuesCB = new CheckboxMenuItem("Use fixed min and max histogram values", useFixedMinMaxValues);
                useFixedMinMaxValuesCB.addItemListener(this);
                preferencesMenu.add(useFixedMinMaxValuesCB);

                preferencesMenu.addSeparator();

                usePreviousSizeCB = new CheckboxMenuItem("Use previous window size", startupPrevSize);
                usePreviousSizeCB.addItemListener(this);
                preferencesMenu.add(usePreviousSizeCB);

                usePreviousPanCB = new CheckboxMenuItem("Use previous pan position", startupPrevPan);
                usePreviousPanCB.addItemListener(this);
                preferencesMenu.add(usePreviousPanCB);

                usePreviousZoomCB = new CheckboxMenuItem("Use previous zoom setting", startupPrevZoom);
                usePreviousZoomCB.addItemListener(this);
//                preferencesMenu.add(usePreviousZoomCB);

                rememberWindowLocationCB = new CheckboxMenuItem("Use previous window location", rememberWindowLocation);
                rememberWindowLocationCB.addItemListener(this);
                preferencesMenu.add(rememberWindowLocationCB);

                preferencesMenu.addSeparator();

                useSexagesimalCB = new CheckboxMenuItem("Display WCS in sexagesimal format", useSexagesimal);
                useSexagesimalCB.addItemListener(this);
                preferencesMenu.add(useSexagesimalCB);

                writeMiddleClickValuesCB = new CheckboxMenuItem("Middle click writes image data to results window", writeMiddleClickValues);
                writeMiddleClickValuesCB.addItemListener(this);
                preferencesMenu.add(writeMiddleClickValuesCB);

                writeMiddleDragValuesCB = new CheckboxMenuItem("Middle drag writes image data to results window", writeMiddleDragValues);
                writeMiddleDragValuesCB.addItemListener(this);
                preferencesMenu.add(writeMiddleDragValuesCB);

                preferencesMenu.addSeparator();

                showPhotometerCB = new CheckboxMenuItem("Show photometer regions at mouse cursor", showPhotometer);
                showPhotometerCB.addItemListener(this);
                preferencesMenu.add(showPhotometerCB);

                removeBackStarsCB = new CheckboxMenuItem("Ignore stars in photometer background region", removeBackStars);
                removeBackStarsCB.addItemListener(this);
                preferencesMenu.add(removeBackStarsCB);
                
                preferencesMenu.addSeparator();

                setPixelScaleMenuItem = new MenuItem("Set pixel scale for images without WCS information...");
                setPixelScaleMenuItem.addActionListener(this);
                preferencesMenu.add(setPixelScaleMenuItem);

                dirAngleMenuItem = new MenuItem("Set direction angle for images without WCS information...");
                dirAngleMenuItem.addActionListener(this);
                preferencesMenu.add(dirAngleMenuItem);

                setZoomIndicatorSizeMenuItem = new MenuItem("Set zoom indicator size...");
                setZoomIndicatorSizeMenuItem.addActionListener(this);
                preferencesMenu.add(setZoomIndicatorSizeMenuItem);

                mainMenuBar.add(preferencesMenu);

//------VIEW menu---------------------------------------------------------------------

                viewMenu = new Menu ("View");
                
                clearOverlayMenuItem = new MenuItem("Clear Overlay");
                clearOverlayMenuItem.addActionListener(this);
                viewMenu.add(clearOverlayMenuItem);

                viewMenu.addSeparator();

                invertNoneRB = new CheckboxMenuItem("Invert None", !invertX && !invertY);
                invertNoneRB.addItemListener(this);
                viewMenu.add(invertNoneRB);

                invertXRB = new CheckboxMenuItem("Invert X", invertX && !invertY);
                invertXRB.addItemListener(this);
                viewMenu.add(invertXRB);

                invertYRB = new CheckboxMenuItem("Invert Y", !invertX && invertY);
                invertYRB.addItemListener(this);
                viewMenu.add(invertYRB);

                invertXYRB = new CheckboxMenuItem("Invert X and Y", invertX && invertY);
                invertXYRB.addItemListener(this);
                viewMenu.add(invertXYRB);

//                invertGroup = new ButtonGroup();
//                invertGroup.add(invertNoneRB);
//                invertGroup.add(invertXRB);
//                invertGroup.add(invertYRB);
//                invertGroup.add(invertXYRB);

                viewMenu.addSeparator();

                rotate0RB = new CheckboxMenuItem("0 degrees", rotation == AstroCanvas.ROT_0);
                rotate0RB.addItemListener(this);
                viewMenu.add(rotate0RB);

                rotate90RB = new CheckboxMenuItem("90 degrees", rotation == AstroCanvas.ROT_90);
                rotate90RB.setEnabled(false);
                rotate90RB.addItemListener(this);
                viewMenu.add(rotate90RB);

                rotate180RB = new CheckboxMenuItem("180 degrees", rotation == AstroCanvas.ROT_180);
                rotate180RB.addItemListener(this);
                rotate180RB.setEnabled(true);
                viewMenu.add(rotate180RB);

                rotate270RB = new CheckboxMenuItem("270 degrees", rotation == AstroCanvas.ROT_270);
                rotate270RB.addItemListener(this);
                rotate270RB.setEnabled(false);
                viewMenu.add(rotate270RB);
                
                viewMenu.addSeparator();

                showZoomCB = new CheckboxMenuItem("Show zoom indicator in overlay", showZoom);
                showZoomCB.addItemListener(this);
                viewMenu.add(showZoomCB);

                showDirCB = new CheckboxMenuItem("Show north & east in overlay", showDir);
                showDirCB.addItemListener(this);
                viewMenu.add(showDirCB);

                showXYCB = new CheckboxMenuItem("Show x-dir & y-dir in overlay", showXY);
                showXYCB.addItemListener(this);
                viewMenu.add(showXYCB);


//                rotationGroup = new ButtonGroup();
//                rotationGroup.add(rotate0RB);
//                rotationGroup.add(rotate90RB);
//                rotationGroup.add(rotate180RB);
//                rotationGroup.add(rotate270RB);

                mainMenuBar.add(viewMenu);

//------EDIT menu---------------------------------------------------------------------

                editMenu = new Menu("Edit");

                apertureSettingsMenuItem = new MenuItem("Aperture settings...");
                apertureSettingsMenuItem.addActionListener(this);
                editMenu.add(apertureSettingsMenuItem);

                measurementSettingsMenuItem = new MenuItem("Measurement settings...");
                measurementSettingsMenuItem.addActionListener(this);
                editMenu.add(measurementSettingsMenuItem);

                editFitsHeaderMenuItem = new MenuItem("FITS header (incomplete)...");
                editFitsHeaderMenuItem.addActionListener(this);

                editMenu.add(editFitsHeaderMenuItem);

                stackSorterMenuItem = new MenuItem("Stack...");
                stackSorterMenuItem.addActionListener(this);
                editMenu.add(stackSorterMenuItem);

                mainMenuBar.add(editMenu);


//------Process menu---------------------------------------------------------------------

                processMenu = new Menu("Process");
                processMenu.add("**Warning - these selections may modify your image data**");

                processMenu.addSeparator();

                dataReducerMenuItem = new MenuItem("Data reduction facility...");
                dataReducerMenuItem.addActionListener(this);
                processMenu.add(dataReducerMenuItem);

                processMenu.addSeparator();

                imageCalcMenuItem = new MenuItem("Image/stack calculator...");
                imageCalcMenuItem.addActionListener(this);
                processMenu.add(imageCalcMenuItem);

                removeOutliersMenuItem = new MenuItem("Remove outliers from image/stack...");
                removeOutliersMenuItem.addActionListener(this);
                processMenu.add(removeOutliersMenuItem);
                
                smoothMenuItem = new MenuItem("Smooth image/stack...");
                smoothMenuItem.addActionListener(this);
                processMenu.add(smoothMenuItem);    
                
                sharpenMenuItem = new MenuItem("Sharpen image/stack...");
                sharpenMenuItem.addActionListener(this);
                processMenu.add(sharpenMenuItem);

                normalizeStackMenuItem = new MenuItem("Normalize image/stack...");
                normalizeStackMenuItem.addActionListener(this);
                processMenu.add(normalizeStackMenuItem);

                processMenu.addSeparator();

                alignStackMenuItem = new MenuItem("Align stack using apertures...");
                alignStackMenuItem.addActionListener(this);
                processMenu.add(alignStackMenuItem);

                imageStabilizerMenuItem = new MenuItem("Align stack using image stabilizer...");
                imageStabilizerMenuItem.addActionListener(this);
                processMenu.add(imageStabilizerMenuItem);

                imageStabilizerApplyMenuItem = new MenuItem("Apply image stabilizer coefficients...");
                imageStabilizerApplyMenuItem.addActionListener(this);
                processMenu.add(imageStabilizerApplyMenuItem);

                shiftImageMenuItem = new MenuItem("Shift image manually...");
                shiftImageMenuItem.addActionListener(this);
                processMenu.add(shiftImageMenuItem);

                selectBestFramesMenuItem = new MenuItem("Select stack images with best edges...");
                selectBestFramesMenuItem.addActionListener(this);
                processMenu.add(selectBestFramesMenuItem);

                processMenu.addSeparator();


                flipDataXMenuItem = new MenuItem("Flip data in x-axis");
                flipDataXMenuItem.addActionListener(this);
                processMenu.add(flipDataXMenuItem);

                flipDataYMenuItem = new MenuItem("Flip data in y-axis");
                flipDataYMenuItem.addActionListener(this);
                processMenu.add(flipDataYMenuItem);

                rotateDataCWMenuItem = new MenuItem("Rotate data 90 degrees clockwise");
                rotateDataCWMenuItem.addActionListener(this);
                processMenu.add(rotateDataCWMenuItem);

                rotateDataCCWMenuItem = new MenuItem("Rotate data 90 degrees counter-clockwise");
                rotateDataCCWMenuItem.addActionListener(this);
                processMenu.add(rotateDataCCWMenuItem);

                mainMenuBar.add(processMenu);


//------ANALYZE menu---------------------------------------------------------------------

                analyzeMenu = new Menu("Analyze");

                multiApertureMenuItem = new MenuItem("Multi-aperture...");
                multiApertureMenuItem.addActionListener(this);
                analyzeMenu.add(multiApertureMenuItem);

                multiPlotMenuItem = new MenuItem("Multi-plot...");
                multiPlotMenuItem.addActionListener(this);
                analyzeMenu.add(multiPlotMenuItem);

                measurementMenuItem = new MenuItem("Measure *");
                measurementMenuItem.addActionListener(this);
                analyzeMenu.add(measurementMenuItem);

                analyzeMenu.addSeparator();
                
                seeingProfileMenuItem = new MenuItem("Plot seeing profile... *");
                seeingProfileMenuItem.addActionListener(this);
                analyzeMenu.add(seeingProfileMenuItem);
                
                staticProfilerMenuItem = new MenuItem("Plot static line/box profile... *");
                staticProfilerMenuItem.addActionListener(this);
                analyzeMenu.add(staticProfilerMenuItem);

                dynamicProfilerMenuItem = new MenuItem("Plot dynamic line/box profile... *");
                dynamicProfilerMenuItem.addActionListener(this);
                analyzeMenu.add(dynamicProfilerMenuItem);

                azimuthalAverageMenuItem = new MenuItem("Plot azimuthal average... *");
                azimuthalAverageMenuItem.addActionListener(this);
                azimuthalAverageMenuItem.setEnabled(false);
                analyzeMenu.add(azimuthalAverageMenuItem);

                threeDSurfacePlotMenuItem = new MenuItem("Interactive 3-D surface plot");
                threeDSurfacePlotMenuItem.addActionListener(this);
                analyzeMenu.add(threeDSurfacePlotMenuItem);

                analyzeMenu.addSeparator();

                analyzeMenu.add("*Requires line, box, or circle selection");
                analyzeMenu.add("  on image before execution.");

                mainMenuBar.add(analyzeMenu);

//------end menus---------------------------------------------------------------------

                mainPanel = new Panel(new SpringLayout());
                topPanelA = new JPanel();
                topPanelA.setLayout(new BoxLayout(topPanelA, BoxLayout.LINE_AXIS));
                zoomPanel = new JPanel();
                zoomPanel.setLayout(new BoxLayout(zoomPanel, BoxLayout.LINE_AXIS));
                topPanelB = new JPanel();
                topPanelB.setLayout(new BoxLayout(topPanelB, BoxLayout.LINE_AXIS));
                topPanelBC = new JPanel(new SpringLayout());
                bottomPanelB = new JPanel();
                bottomPanelB.setLayout(new BoxLayout(bottomPanelB, BoxLayout.LINE_AXIS));

                topPanelB.add(Box.createHorizontalGlue());

                Dimension valueDim = new Dimension(90, 20);
                Dimension valueDimMin = new Dimension(90, 20);
                Dimension intCntDim = new Dimension(120, 20);
                Dimension intCntDimMin = new Dimension(120, 20);
                Dimension labelDim = new Dimension(65, 20);
                Dimension labelDimMin = new Dimension (65, 20);

                JLabel ijXLabel = new JLabel("ImageJ X:");
                ijXLabel.setFont(p12);
                ijXLabel.setHorizontalAlignment(JLabel.RIGHT);
                ijXLabel.setPreferredSize(labelDim);
                ijXLabel.setMaximumSize(labelDim);
                ijXLabel.setMinimumSize(labelDimMin);
                ijXLabel.setLabelFor(ijXTextField);
                topPanelBC.add(ijXLabel);

                ijXTextField = new JTextField("");
                ijXTextField.setFont(p12);
                ijXTextField.setHorizontalAlignment(JLabel.RIGHT);
                ijXTextField.setPreferredSize(valueDim);
                ijXTextField.setMaximumSize(valueDim);
                ijXTextField.setMinimumSize(valueDimMin);
                ijXTextField.setEditable(false);
                topPanelBC.add(ijXTextField);

                JLabel ijYLabel = new JLabel("ImageJ Y:");
                ijYLabel.setFont(p12);
                ijYLabel.setHorizontalAlignment(JLabel.RIGHT);
                ijYLabel.setPreferredSize(labelDim);
                ijYLabel.setMaximumSize(labelDim);
                ijYLabel.setMinimumSize(labelDimMin);
                ijYLabel.setLabelFor(ijYTextField);
                topPanelBC.add(ijYLabel);

                ijYTextField = new JTextField("");
                ijYTextField.setFont(p12);
                ijYTextField.setHorizontalAlignment(JTextField.RIGHT);
                ijYTextField.setPreferredSize(valueDim);
                ijYTextField.setMaximumSize(valueDim);
                ijYTextField.setMinimumSize(valueDimMin);
                ijYTextField.setEditable(false);
                topPanelBC.add(ijYTextField);

                JLabel valueLabel = new JLabel("Value:");
                valueLabel.setFont(p12);
                valueLabel.setHorizontalAlignment(JLabel.RIGHT);
                valueLabel.setPreferredSize(labelDim);
                valueLabel.setMaximumSize(labelDim);
                valueLabel.setMinimumSize(labelDimMin);
                valueLabel.setLabelFor(valueTextField);
                topPanelBC.add(valueLabel);

                valueTextField = new JTextField("");
                valueTextField.setFont(b12);
                valueTextField.setHorizontalAlignment(JTextField.RIGHT);
                valueTextField.setPreferredSize(intCntDim);
                valueTextField.setMaximumSize(intCntDim);
                valueTextField.setMinimumSize(intCntDimMin);
                valueTextField.setEditable(false);
                topPanelBC.add(valueTextField);

                JLabel RALabel = new JLabel("RA:");
                RALabel.setFont(p12);
                RALabel.setHorizontalAlignment(JLabel.RIGHT);
                RALabel.setPreferredSize(labelDim);
                RALabel.setMaximumSize(labelDim);
                RALabel.setMinimumSize(labelDimMin);
                RALabel.setLabelFor(RATextField);
                topPanelBC.add(RALabel);

                RATextField = new JTextField("");
                RATextField.setFont(p12);
                RATextField.setHorizontalAlignment(JLabel.RIGHT);
                RATextField.setPreferredSize(valueDim);
                RATextField.setMaximumSize(valueDim);
                RATextField.setMinimumSize(valueDimMin);
                RATextField.setEditable(false);
                topPanelBC.add(RATextField);

                JLabel DecLabel = new JLabel("DEC:");
                DecLabel.setFont(p12);
                DecLabel.setHorizontalAlignment(JLabel.RIGHT);
                DecLabel.setPreferredSize(labelDim);
                DecLabel.setMaximumSize(labelDim);
                DecLabel.setMinimumSize(labelDimMin);
                DecLabel.setLabelFor(DecTextField);
                topPanelBC.add(DecLabel);

                DecTextField = new JTextField("");
                DecTextField.setFont(p12);
                DecTextField.setHorizontalAlignment(JLabel.RIGHT);
                DecTextField.setPreferredSize(valueDim);
                DecTextField.setMaximumSize(valueDim);
                DecTextField.setMinimumSize(valueDimMin);
                DecTextField.setEditable(false);
                topPanelBC.add(DecTextField);

//                JLabel arcLengthLabel = new JLabel("Arclen:");
                peakLabel = new JTextField("Peak:");
                peakLabel.setFont(p12);
                peakLabel.setBorder(BorderFactory.createEmptyBorder());
                peakLabel.setBackground(topPanelA.getBackground());
                peakLabel.setPreferredSize(labelDim);
                peakLabel.setMaximumSize(labelDim);
                peakLabel.setMinimumSize(labelDimMin);
                peakLabel.setHorizontalAlignment(JLabel.RIGHT);
                topPanelBC.add(peakLabel);

                peakTextField = new JTextField("");
                peakTextField.setFont(p12);
                peakTextField.setHorizontalAlignment(JLabel.RIGHT);
                peakTextField.setPreferredSize(intCntDim);
                peakTextField.setMaximumSize(intCntDim);
                peakTextField.setMinimumSize(intCntDimMin);
                peakTextField.setEditable(false);
                topPanelBC.add(peakTextField);

                JLabel fitsXLabel = new JLabel("FITS X:");
                fitsXLabel.setFont(p12);
                fitsXLabel.setHorizontalAlignment(JLabel.RIGHT);
                fitsXLabel.setPreferredSize(labelDim);
                fitsXLabel.setMaximumSize(labelDim);
                fitsXLabel.setMinimumSize(labelDimMin);
                fitsXLabel.setLabelFor(fitsXTextField);
                topPanelBC.add(fitsXLabel);

                fitsXTextField = new JTextField("");
                fitsXTextField.setFont(p12);
                fitsXTextField.setHorizontalAlignment(JLabel.RIGHT);
                fitsXTextField.setPreferredSize(valueDim);
                fitsXTextField.setMaximumSize(valueDim);
                fitsXTextField.setMinimumSize(valueDimMin);
                fitsXTextField.setEditable(false);
                topPanelBC.add(fitsXTextField);

                JLabel fitsYLabel = new JLabel("FITS Y:");
                fitsYLabel.setFont(p12);
                fitsYLabel.setHorizontalAlignment(JLabel.RIGHT);
                fitsYLabel.setPreferredSize(labelDim);
                fitsYLabel.setMaximumSize(labelDim);
                fitsYLabel.setMinimumSize(labelDimMin);
                fitsYLabel.setLabelFor(fitsYTextField);
                topPanelBC.add(fitsYLabel);

                fitsYTextField = new JTextField("");
                fitsYTextField.setFont(p12);
                fitsYTextField.setHorizontalAlignment(JLabel.RIGHT);
                fitsYTextField.setPreferredSize(valueDim);
                fitsYTextField.setMaximumSize(valueDim);
                fitsYTextField.setMinimumSize(valueDimMin);
                fitsYTextField.setEditable(false);
                topPanelBC.add(fitsYTextField);

//                JLabel lengthLabel = new JLabel("Length:");
                lengthLabel = new JTextField("Int Cnts:");
                lengthLabel.setFont(p12);
                lengthLabel.setBorder(BorderFactory.createEmptyBorder());
                lengthLabel.setBackground(topPanelA.getBackground());
                lengthLabel.setPreferredSize(labelDim);
                lengthLabel.setMaximumSize(labelDim);
                lengthLabel.setMinimumSize(labelDimMin);
                lengthLabel.setHorizontalAlignment(JLabel.RIGHT);
                topPanelBC.add(lengthLabel);

                lengthTextField = new JTextField("");
                lengthTextField.setFont(p12);
                lengthTextField.setHorizontalAlignment(JLabel.RIGHT);
                lengthTextField.setPreferredSize(intCntDim);
                lengthTextField.setMaximumSize(intCntDim);
                lengthTextField.setMinimumSize(intCntDimMin);
                lengthTextField.setEditable(false);
                topPanelBC.add(lengthTextField);

                SpringUtil.makeCompactGrid (topPanelBC, 3, topPanelBC.getComponentCount()/3, 3,3,3,3);
                topPanelB.add(topPanelBC);

                topPanelB.add(Box.createGlue());

                mainPanel.add(topPanelB);

                topPanelA.add(Box.createHorizontalGlue());
                topPanelA.add(Box.createHorizontalStrut(20));
//                Insets buttonMargin = new Insets(0,2,0,2); //top,left,bottom,right
//                if (IJ.isWindows() || IJ.isMacintosh())
//                       buttonMargin = new Insets(2,4,2,4); //top,left,bottom,right

                buttonSub32768 = new JButton("-32768");
//                buttonSub32768.setMargin(buttonMargin);
                buttonSub32768.addActionListener(this);
//                topPanelAC.add(buttonSub32768);
                buttonAdd32768 = new JButton("+32768");
//                buttonAdd32768.setMargin(buttonMargin);
                buttonAdd32768.addActionListener(this);
//                topPanelAC.add(buttonAdd32768);
                buttonNegative = new JButton("Neg");
//                buttonNegative.setMargin(buttonMargin);
                buttonNegative.addActionListener(this);
                topPanelA.add(buttonNegative);
                topPanelA.add(Box.createHorizontalStrut(10));
                buttonFlipX = new JButton("FlipX");
//                buttonFlipX.setMargin(buttonMargin);
                buttonFlipX.addActionListener(this);
//                topPanelA.add(buttonFlipX);
                buttonFlipY = new JButton("FlipY");
//                buttonFlipY.setMargin(buttonMargin);
                buttonFlipY.addActionListener(this);
//                topPanelA.add(buttonFlipY);
                buttonRotCCW = new JButton("RotCCW");
//                buttonRotCCW.setMargin(buttonMargin);
                buttonRotCCW.addActionListener(this);
//                topPanelA.add(buttonRotCCW);
                buttonRotCW = new JButton("RotCW");
//                buttonRotCW.setMargin(buttonMargin);
                buttonRotCW.addActionListener(this);
//                topPanelA.add(buttonRotCW);
                buttonHeader = new JButton("Header");
//                buttonHeader.setMargin(buttonMargin);
                buttonHeader.addActionListener(this);
                topPanelA.add(buttonHeader);
                topPanelA.add(Box.createHorizontalStrut(10));
                topPanelA.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

//                buttonLUT = new JButton("LUT");
//                buttonLUT.setMargin(buttonMargin);
//                buttonLUT.addActionListener(this);
//                topPanelAC.add(buttonLUT);


                JLabel zoomLabel = new JLabel("Zoom  ");
                zoomPanel.add(zoomLabel);
                int zbWidth = 200;
                int zbHeight = 50;
                Dimension zoomDimension = new Dimension(zbWidth, zbHeight);
                buttonZoomInFast = new JButton("In x8");
                buttonZoomInFast.addActionListener(this);
//                buttonZoomInFast.setPreferredSize(zoomDimension);
                zoomPanel.add(buttonZoomInFast);

                buttonZoomIn = new JButton("  In  ");
                buttonZoomIn.addActionListener(this);
//                buttonZoomIn.setPreferredSize(zoomDimension);
                zoomPanel.add(buttonZoomIn);

                buttonZoomOut = new JButton(" Out ");
                buttonZoomOut.addActionListener(this);
//                buttonZoomOut.setPreferredSize(zoomDimension);
                zoomPanel.add(buttonZoomOut);

                buttonFit = new JButton(" Fit ");
//                buttonFit.setPreferredSize(zoomDimension);
                buttonFit.addActionListener(this);
                zoomPanel.add(buttonFit);
                zoomPanel.setBorder(BorderFactory.createTitledBorder(""));
                topPanelA.add(zoomPanel);
//                buttonRefresh = new JButton("Refresh");
//                buttonRefresh.setMargin(buttonMargin);
//                buttonRefresh.addActionListener(this);

                topPanelA.add(Box.createHorizontalStrut(10));
                
                buttonAutoLevels = new JButton("Auto Scale");
                buttonAutoLevels.addActionListener(this);
                topPanelA.add(buttonAutoLevels);
                topPanelA.add(Box.createHorizontalGlue());
                mainPanel.add(topPanelA);


//                icPanel.add(ac);
//                SpringUtil.makeCompactGrid (icPanel, 1, 1, 0,0,0,0);
                mainPanel.add(ac);

                stackSliders = this.getComponents();
//                IJ.log("stackSliders.length="+stackSliders.length);
                if (stackSliders.length > 0)
                        for (int i = 0; i < stackSliders.length; i++)
                                {
                                stackSliders[i].setPreferredSize(new Dimension(100,18));
                                mainPanel.add(stackSliders[i]);
                                }


                minValueTextField = new JTextField(fourPlaces.format(minValue));
                minValueTextField.setFont(p12);
                minValueTextField.setPreferredSize(new Dimension(70,17));
                minValueTextField.setHorizontalAlignment(JTextField.LEFT);
                bottomPanelB.add(minValueTextField);
                JTextField minlabelTextField = new JTextField(":min");
                minlabelTextField.setFont(p12);
                minlabelTextField.setPreferredSize(new Dimension(30,17));
                minlabelTextField.setHorizontalAlignment(JTextField.LEFT);
                minlabelTextField.setBorder(BorderFactory.createEmptyBorder());
                minlabelTextField.setEditable(false);
                bottomPanelB.add(minlabelTextField);

                bottomPanelB.add(Box.createHorizontalStrut(10));

                minTextField = new JTextField(fourPlaces.format(min));
                minTextField.setFont(b12);
                minTextField.setPreferredSize(new Dimension(70,17));
                minTextField.setHorizontalAlignment(JTextField.RIGHT);
                minTextField.setBorder(BorderFactory.createLineBorder(Color.RED));
                minTextField.setEditable(true);
                minTextField.addActionListener(this);
//                minTextField.getDocument().addDocumentListener(new thisDocumentListener());
                bottomPanelB.add(minTextField);

                JTextField lowlabelTextField = new JTextField(":black");
                lowlabelTextField.setFont(p12);
                lowlabelTextField.setPreferredSize(new Dimension(30,17));
                lowlabelTextField.setHorizontalAlignment(JTextField.LEFT);
                lowlabelTextField.setBorder(BorderFactory.createEmptyBorder());
                lowlabelTextField.setEditable(false);
                bottomPanelB.add(lowlabelTextField);

                bottomPanelB.add(Box.createHorizontalGlue());

                JTextField meanlabelTextField = new JTextField("mean:");
                meanlabelTextField.setFont(p12);
                meanlabelTextField.setPreferredSize(new Dimension(70,17));
                meanlabelTextField.setHorizontalAlignment(JTextField.RIGHT);
                meanlabelTextField.setBorder(BorderFactory.createEmptyBorder());
                meanlabelTextField.setEditable(false);
                bottomPanelB.add(meanlabelTextField);

                meanTextField = new JTextField(fourPlaces.format(stats.mean));
                meanTextField.setFont(p12);
                meanTextField.setPreferredSize(new Dimension(70,17));
                meanTextField.setHorizontalAlignment(JTextField.LEFT);
                meanTextField.setBorder(BorderFactory.createEmptyBorder());
                meanTextField.setEditable(false);
                bottomPanelB.add(meanTextField);

                bottomPanelB.add(Box.createHorizontalGlue());

                JTextField highlabelTextField = new JTextField("white:");
                highlabelTextField.setFont(p12);
                highlabelTextField.setPreferredSize(new Dimension(30,17));
                highlabelTextField.setHorizontalAlignment(JTextField.RIGHT);
                highlabelTextField.setBorder(BorderFactory.createEmptyBorder());
                highlabelTextField.setEditable(false);
                bottomPanelB.add(highlabelTextField);

                maxTextField = new JTextField(fourPlaces.format(max));
                maxTextField.setFont(b12);
                maxTextField.setPreferredSize(new Dimension(70,17));
                maxTextField.setHorizontalAlignment(JTextField.RIGHT);
                maxTextField.setBorder(BorderFactory.createLineBorder(Color.RED));
                maxTextField.setEditable(true);
                maxTextField.addActionListener(this);
//                maxTextField.getDocument().addDocumentListener(new thisDocumentListener());
                bottomPanelB.add(maxTextField);

                bottomPanelB.add(Box.createHorizontalStrut(10));

                JTextField maxlabelTextField = new JTextField("max:");
                maxlabelTextField.setFont(p12);
                maxlabelTextField.setPreferredSize(new Dimension(30,17));
                maxlabelTextField.setHorizontalAlignment(JTextField.RIGHT);
                maxlabelTextField.setBorder(BorderFactory.createEmptyBorder());
                maxlabelTextField.setEditable(false);
                bottomPanelB.add(maxlabelTextField);

                maxValueTextField = new JTextField(fourPlaces.format(maxValue));
                maxValueTextField.setFont(p12);
                maxValueTextField.setPreferredSize(new Dimension(70,17));
                maxValueTextField.setHorizontalAlignment(JTextField.RIGHT);
                bottomPanelB.add(maxValueTextField);
                updateMinMaxValueTextFields();
                bottomPanelB.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

//                SpringUtil.makeCompactGrid (bottomPanelB, 1, bottomPanelB.getComponentCount(), 5,0,5,0);
                ImageProcessor ip = imp.getProcessor();
                if (imp.getType()==ImagePlus.COLOR_RGB)
                    {
                    ip.reset();
                    }
                getBiSliderStatistics();
                histogram = stats.histogram;
                logHistogram = new double[histogram.length];

                for (int i=0; i<histogram.length; i++)
                    {
                    if (histogram[i] <= 1)
                        logHistogram[i] = 0;
                    else
                        logHistogram[i] = Math.log(histogram[i]);
                    if (logHistogram[i] > histMax)
                        histMax = logHistogram[i];
                    }

                minMaxBiSlider = new BiSlider(BiSlider.RGB);
                minMaxBiSlider.setUniformSegment(false);
                minMaxBiSlider.setDecimalFormater(fourPlaces);
                minMaxBiSlider.setFont(p13);
//                minMaxBiSlider.setHorizontal(false);
//                minMaxBiSlider.setUI((BiSliderPresentation) metal);
                minMaxBiSlider.setVisible(true);
                minMaxBiSlider.setValues(minValue, maxValue);
                minMaxBiSlider.setMinimumColoredValue(min);
                minMaxBiSlider.setMaximumColoredValue(max);
                minMaxBiSlider.setMinimumValue(minValue);
                minMaxBiSlider.setMaximumValue(maxValue);
                minMaxBiSlider.setSegmentSize((maxValue - minValue)/(double)BISLISER_SEGMENTS);
                minMaxBiSlider.setMinimumColor(Color.BLACK);

                minMaxBiSlider.setMiddleColor(Color.BLACK);
                minMaxBiSlider.setMaximumColor(Color.BLACK);
                minMaxBiSlider.setColoredValues(min, max);
                minMaxBiSlider.setUnit("  ");
                minMaxBiSlider.setSliderBackground(Color.LIGHT_GRAY);
                minMaxBiSlider.setForeground(Color.BLACK);

                minMaxBiSlider.setDefaultColor(Color.WHITE);
                minMaxBiSlider.setPreferredSize(new Dimension(535,75));
                minMaxBiSlider.setPrecise(true);
                minMaxBiSlider.setOpaque(true);
                minMaxBiSlider.setArcSize(0);
                minMaxBiSlider.addContentPainterListener(new ContentPainterListener() {
                  public void paint(ContentPainterEvent ContentPainterEvent_Arg){
                    Graphics2D Graphics2 = (Graphics2D)ContentPainterEvent_Arg.getGraphics();
                    Rectangle Rect1 = ContentPainterEvent_Arg.getRectangle();
                    Graphics2.setColor((new Color(230, 230, 230)));
                    Graphics2.fillRect(Rect1.x, Rect1.y, Rect1.width, Rect1.height);
                    Rectangle Rect2 = ContentPainterEvent_Arg.getBoundingRectangle();
//                    double BarHeight = Math.abs(Math.cos(Math.PI*(Rect2.x+Rect2.width/2) / minMaxBiSlider.getWidth()));
//                    double BarHeight = (double)(Rect2.x+Rect2.width/2) / minMaxBiSlider.getWidth();
//                    double BarHeight = Math.random();

//                    float X = ((float)Rect2.x-minMaxBiSlider.getWidth()/2)/minMaxBiSlider.getWidth()*6;
                    double X = ((double)(Rect2.x - 10 - minMaxBiSlider.getX()))/((double)minMaxBiSlider.getWidth()-22.0);
//                    double BarHeight = 1-Math.exp((-1*X*X)/2);
                    double BarHeight = 1.0-(logHistogram[(int)(histogram.length*X)])/histMax;
//                    X = ((float)(Rect2.x-Rect2.width - 10 - minMaxBiSlider.getX()))/(double)minMaxBiSlider.getWidth();
//                    double BarHeight2 = 1-Math.exp((-1*X*X)/2);
                    double BarHeight2 = 1.0-(logHistogram[(int)(histogram.length*X)])/histMax;

                    if (ContentPainterEvent_Arg.getColor()!=null) {
                      Graphics2.setColor(Color.WHITE);
                      Graphics2.fillRect(Rect2.x, Rect2.y, Rect2.width, (int)((BarHeight*Rect2.height)));
                      Graphics2.setColor(new Color(120, 165, 255));//(ContentPainterEvent_Arg.getColor());
                      Graphics2.fillRect(Rect2.x, Rect2.y+(int)((BarHeight*Rect2.height)), Rect2.width+1, 1+(int)(((1-BarHeight)*Rect2.height)));
                      //Graphics2.drawRect(Rect2.x, Rect2.y+(int)((BarHeight*Rect2.height)), Rect2.width+1, 1+(int)(((1-BarHeight)*Rect2.height)));
                    } else {
                      Graphics2.setColor(Color.LIGHT_GRAY);//(new Color(255, 255, 218, 64));
                      Graphics2.fillRect(Rect2.x, Rect2.y+(int)((BarHeight*Rect2.height)), Rect2.width+1, 1+(int)(((1-BarHeight)*Rect2.height)));
                    }
//                    Graphics2.setColor(Color.LIGHT_GRAY);
//                    //Graphics2.drawRect(Rect2.x, Rect2.y+(int)((BarHeight*Rect2.height)), Rect2.width-1, (int)(((1-BarHeight)*Rect2.height)));
//                    Graphics2.drawLine(Rect2.x, Rect2.y+(int)((BarHeight*Rect2.height)), Rect2.x+Rect2.width-1, Rect2.y+(int)((BarHeight*Rect2.height)));
//                    Graphics2.drawLine(Rect2.x, Rect2.y+(int)((BarHeight*Rect2.height)), Rect2.x, Rect2.y+(int)((prevBarHeight*Rect2.height)));
////                    Graphics2.drawLine(Rect2.x, Rect2.y+(int)((Math.max(BarHeight, BarHeight2)*Rect2.height)), Rect2.x, Rect2.y+Rect2.height);
//                    Rect3 = Rect2;
//                    prevBarHeight = BarHeight;
                  }
                });

//                final JPopupMenu JPopupMenu6 = minMaxBiSlider.createPopupMenu();
//                minMaxBiSlider.addMouseListener(new MouseAdapter(){
//                  public void mousePressed(MouseEvent MouseEvent_Arg){
//                    if (MouseEvent_Arg.getButton()==MouseEvent.BUTTON3){
//                      JPopupMenu6.show(minMaxBiSlider, MouseEvent_Arg.getX(), MouseEvent_Arg.getY());
//                    }
//                  }
//                });

                final String initialText = "\n\n\n Use this BiSlider to see the events generated\n";
                final JTextArea JTextArea5 = new  JTextArea(initialText);
                minMaxBiSlider.addBiSliderListener(new BiSliderAdapter(){
                      /** something changed that modified the color gradient between min and max */
                      public void newColors(BiSliderEvent BiSliderEvent_Arg) {
//                      IJ.log("newColors()");
                      }
                      /**  min or max colored values changed  */
                      public void newValues(BiSliderEvent BiSliderEvent_Arg) {

                            min = minMaxBiSlider.getMinimumColoredValue();
                            max = minMaxBiSlider.getMaximumColoredValue();
//
//                            if (min < minValue)
//                                    {
//                                    min = minValue;
//                                    minMaxBiSlider.setMinimumColoredValue(min);
//                                    }
//                            if (min > maxValue)
//                                     {
//                                    min = maxValue;
//                                    minMaxBiSlider.setMinimumColoredValue(min);
//                                    }
//                            if (max > maxValue)
//                                    {
//                                    max = maxValue;
//                                    minMaxBiSlider.setMaximumColoredValue(max);
//                                    }
//                            if (max < min)
//                                    {
//                                    max = min;
//                                    minMaxBiSlider.setMaximumColoredValue(max);
//                                    }

                            imp.setDisplayRange(min, max);
                            minMaxChanged = true;
                            minTextField.setText(fourPlaces.format(min));
                            maxTextField.setText(fourPlaces.format(max));
                            savedMin = min;
                            savedMax = max;
//                            Prefs.set("Astronomy_Tool.savedMin", savedMin);
//                            Prefs.set("Astronomy_Tool.savedMax", savedMax);

                            imp.updateAndDraw();
                      }
                      /**  min selected value changed  */
                      public void newMinValue(BiSliderEvent BiSliderEvent_Arg) {
//                      IJ.log("newMinValue()");
//                          getBiSliderStatistics;
//                          updatePanelValues();
                      }
                      /**  max selected value changed  */
                      public void newMaxValue(BiSliderEvent BiSliderEvent_Arg) {
//                      IJ.log("newMaxValue()");
//                            getBiSliderStatistics;
//                            maxValue = minMaxBiSlider.getMaximumValue();
//                            minMaxBiSlider.setSegmentSize((maxValue - minValue)/256.0);
//                            histogram = stats.histogram;
//                            IJ.log("after histogram="+minMaxBiSlider.getMaximumValue());
//                            for (int i=0; i<histogram.length; i++)
//                                {
//                                if (histogram[i] <= 1)
//                                    logHistogram[i] = 0;
//                                else
//                                    logHistogram[i] = Math.log(histogram[i]);
//                                if (logHistogram[i] > histMax)
//                                    histMax = logHistogram[i];
//                                }
//                            updatePanelValues();
                      }
                      /**  selected segments changed  */
                      public void newSegments(BiSliderEvent BiSliderEvent_Arg) {
//                          IJ.log("newSegments()");
//                          getBiSliderStatistics);
//                          updatePanelValues();
                      }
                    });
//                imp.setDisplayRange(min, max);
//                minMaxChanged = true;
                mainPanel.add(minMaxBiSlider);
                mainPanel.add(bottomPanelB);

                SpringUtil.makeCompactGrid (mainPanel, mainPanel.getComponentCount(), 1, 0,0,0,0);
                
                setMenuBar(mainMenuBar);
                setTitle(impTitle);
                setName(impTitle);
                add(mainPanel);
                
                setResizable(true);

                if (rememberWindowLocation)
                        this.setLocation(frameLocationX, frameLocationY);
                pack();

                otherPanelsHeight = topPanelA.getHeight() + topPanelB.getHeight() +
                                    bottomPanelB.getHeight() + minMaxBiSlider.getHeight();
     }

//            class thisDocumentListener implements DocumentListener
//                {
//                public void insertUpdate (DocumentEvent ev)
//                    {
//                    IJ.log("insert");
//                    }
//                public void removeUpdate (DocumentEvent ev)
//                    {
//                    IJ.log("remove");
//                    }
//                public void changedUpdate (DocumentEvent ev)
//                    {
//                    IJ.log("changed");
//                    }
//                }
    void updateMinMaxValueTextFields() 
        {
        if (!useFixedMinMaxValues)
            {
            minValueTextField.setEditable(false);
            minValueTextField.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            minValueTextField.removeActionListener(this);
            maxValueTextField.setEditable(false);
            maxValueTextField.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            maxValueTextField.removeActionListener(this);
            }
        else
            {
            minValueTextField.setBorder(BorderFactory.createLineBorder(Color.RED));
            minValueTextField.setEditable(true);
            minValueTextField.addActionListener(this);
            maxValueTextField.setBorder(BorderFactory.createLineBorder(Color.RED));
            maxValueTextField.setEditable(true);
            maxValueTextField.addActionListener(this);
            }
        }

	public void itemStateChanged (ItemEvent e)
		{
		Object source = e.getItemSelectable();
		if (e.getStateChange() == ItemEvent.SELECTED)
			{
			if (source == autoConvertCB)
                {
				autoConvert = true;
                Prefs.set("Astronomy_Tool.autoConvert",autoConvert);
                }
            else if (source == startupAutoLevelRB)
                {
                startupAutoLevel = true;
                startupPrevLevels = false;
                usePreviousLevelsRB.setState(false);
                useFullRangeRB.setState(false);
                Prefs.set("Astronomy_Tool.startupAutoLevel",startupAutoLevel);
                Prefs.set("Astronomy_Tool.startupPrevLevels",startupPrevLevels);
                setAutoLevels(null);
                }
            else if (source == usePreviousLevelsRB)
                {
                startupAutoLevel = false;
                startupPrevLevels = true;
                startupAutoLevelRB.setState(false);
                useFullRangeRB.setState(false);
                Prefs.set("Astronomy_Tool.startupAutoLevel",startupAutoLevel);
                Prefs.set("Astronomy_Tool.startupPrevLevels",startupPrevLevels);
                }
             else if (source == useFullRangeRB)
                {
                startupAutoLevel = false;
                startupPrevLevels = false;
                startupAutoLevelRB.setState(false);
                usePreviousLevelsRB.setState(false);
                Prefs.set("Astronomy_Tool.startupAutoLevel",startupAutoLevel);
                Prefs.set("Astronomy_Tool.startupPrevLevels",startupPrevLevels);
                min = minValue;
                max = maxValue;
                updatePanelValues();
                }
            else if (source == useFixedMinMaxValuesCB)
                {
                if (imp.getType() == ImagePlus.COLOR_256 || imp.getType() == ImagePlus.COLOR_RGB || imp.getType() == ImagePlus.GRAY8)
                    {
                    useFixedMinMaxValues = false;
                    useFixedMinMaxValuesCB.setState(false);
                    }
                else
                    {
                    useFixedMinMaxValues = true;
                    Prefs.set("Astronomy_Tool.useFixedMinMaxValues", useFixedMinMaxValues);
      
                    if (imp.getType() == ImagePlus.GRAY16 && fixedMaxValue - fixedMinValue < 256)
                         fixedMaxValue = fixedMinValue + 255;
                    maxValue = fixedMaxValue;
                    minValue = fixedMinValue;             
                    updateMinMaxValues();
                    updateMinMaxValueTextFields();
                    }
                }
            else if(source == usePreviousSizeCB)
                {
				startupPrevSize = true;
                Prefs.set("Astronomy_Tool.startupPrevSize",startupPrevSize);
                }
            else if(source == usePreviousPanCB)
                {
				startupPrevPan = true;
                Prefs.set("Astronomy_Tool.startupPrevPan",startupPrevPan);
                }
            else if(source == usePreviousZoomCB)
                {
				startupPrevZoom = true;
                Prefs.set("Astronomy_Tool.startupPrevZoom",startupPrevZoom);
                }
            else if(source == rememberWindowLocationCB)
                {
				rememberWindowLocation = true;
                Prefs.set("Astronomy_Tool.rememberWindowLocation",rememberWindowLocation);
                }
            else if(source == useSexagesimalCB)
                {
				useSexagesimal = true;
                Prefs.set("Astronomy_Tool.useSexagesimal",useSexagesimal);
                }
            else if(source == writeMiddleClickValuesCB)
                {
				writeMiddleClickValues = true;
                Prefs.set("Astronomy_Tool.writeMiddleClickValues",writeMiddleClickValues);
                }
            else if(source == writeMiddleDragValuesCB)
                {
				writeMiddleDragValues = true;
                Prefs.set("Astronomy_Tool.writeMiddleDragValues",writeMiddleDragValues);
                }
            else if(source == showPhotometerCB)
                {
                showPhotometer = true;
                updatePhotometerOverlay();
                Prefs.set("Astronomy_Tool.showPhotometer",showPhotometer);
                }
            else if(source == removeBackStarsCB)
                {
				removeBackStars = true;
                Prefs.set("Astronomy_Tool.removeBackStars",removeBackStars);
                }
            else if(source == showZoomCB)
                {
				showZoom = true;
                ac.setShowZoom(showZoom);
                clearAndPaint();
                Prefs.set("Astronomy_Tool.showZoom",showZoom);
                }
            else if(source == showDirCB)
                {
				showDir = true;
                ac.setShowDir(showDir);
                clearAndPaint();
                Prefs.set("Astronomy_Tool.showDir",showDir);
                }
            else if(source == showXYCB)
                {
				showXY = true;
                ac.setShowXY(showXY);
                clearAndPaint();
                Prefs.set("Astronomy_Tool.showXY",showXY);
                }
            else if(source == invertNoneRB)
                {
				invertX = false;
                invertY = false;
                invertXRB.setState(false);
                invertYRB.setState(false);
                invertXYRB.setState(false);
                setOrientation();
                }
            else if(source == invertXRB)
                {
				invertX = true;
                invertY = false;
                invertNoneRB.setState(false);
                invertYRB.setState(false);
                invertXYRB.setState(false);
                setOrientation();
                }
            else if(source == invertYRB)
                {
				invertX = false;
                invertY = true;
                invertXRB.setState(false);
                invertNoneRB.setState(false);
                invertXYRB.setState(false);
                setOrientation();
                }
            else if(source == invertXYRB)
                {
				invertX = true;
                invertY = true;
                invertXRB.setState(false);
                invertYRB.setState(false);
                invertNoneRB.setState(false);
                setOrientation();
                }
            else if(source == rotate0RB)
                {
				rotation = AstroCanvas.ROT_0;
                rotate90RB.setState(false);
                rotate180RB.setState(false);
                rotate270RB.setState(false);
                setOrientation();
                }
            else if(source == rotate90RB)
                {
				rotation = AstroCanvas.ROT_90;
                rotate0RB.setState(false);
                rotate180RB.setState(false);
                rotate270RB.setState(false);
                setOrientation();
                }
            else if(source == rotate180RB)
                {
				rotation = AstroCanvas.ROT_180;
                rotate0RB.setState(false);
                rotate90RB.setState(false);
                rotate270RB.setState(false);
                setOrientation();
                }
            else if(source == rotate270RB)
                {
				rotation = AstroCanvas.ROT_270;
                rotate0RB.setState(false);
                rotate90RB.setState(false);
                rotate180RB.setState(false);
                setOrientation();
                }
            }
        else if(e.getStateChange() == ItemEvent.DESELECTED)
			{
			if (source == autoConvertCB)
                {
				autoConvert = false;
                Prefs.set("Astronomy_Tool.autoConvert",autoConvert);
                }
            else if (source == useFixedMinMaxValuesCB)
                {
                useFixedMinMaxValues = false;
                getStatistics();
                if (imp.getType() == ImagePlus.COLOR_RGB || imp.getType() == ImagePlus.GRAY8)
                    {
                    minValue = 0;
                    maxValue = 255;
                    }
                else if (imp.getType() == ImagePlus.GRAY16 && stats.max-stats.min < 256)
                    {
                    minValue = stats.min;
                    maxValue = stats.min + 255;
                    }
                else
                    {
                    minValue=stats.min;
                    maxValue=stats.max;
                    Prefs.set("Astronomy_Tool.useFixedMinMaxValues", useFixedMinMaxValues);
                    }
                updateMinMaxValueTextFields();
                updateMinMaxValues();
                }
            else if(source == usePreviousSizeCB)
                {
				startupPrevSize = false;
                Prefs.set("Astronomy_Tool.startupPrevSize",startupPrevSize);
                }
            else if(source == usePreviousPanCB)
                {
				startupPrevPan = false;
                Prefs.set("Astronomy_Tool.startupPrevPan",startupPrevPan);
                }
            else if(source == usePreviousZoomCB)
                {
				startupPrevZoom = false;
                Prefs.set("Astronomy_Tool.startupPrevZoom",startupPrevZoom);
                }
            else if(source == rememberWindowLocationCB)
                {
				rememberWindowLocation = false;
                Prefs.set("Astronomy_Tool.rememberWindowLocation",rememberWindowLocation);
                }
            else if(source == useSexagesimalCB)
                {
				useSexagesimal = false;
                Prefs.set("Astronomy_Tool.useSexagesimal",useSexagesimal);
                }
            else if(source == writeMiddleClickValuesCB)
                {
				writeMiddleClickValues = false;
                Prefs.set("Astronomy_Tool.writeMiddleClickValues",writeMiddleClickValues);
                }
            else if(source == writeMiddleDragValuesCB)
                {
				writeMiddleDragValues = false;
                Prefs.set("Astronomy_Tool.writeMiddleDragValues",writeMiddleDragValues);
                }
            else if(source == showPhotometerCB)
                {
                showPhotometer = false;
                apertureOverlay.clear();
                ac.repaint();
                Prefs.set("Astronomy_Tool.showPhotometer",showPhotometer);
                }
            else if(source == removeBackStarsCB)
                {
				removeBackStars = false;
                Prefs.set("Astronomy_Tool.removeBackStars",removeBackStars);
                }
            else if(source == showZoomCB)
                {
				showZoom = false;
                ac.setShowZoom(showZoom);
                clearAndPaint();
                Prefs.set("Astronomy_Tool.showZoom",showZoom);
                }
            else if(source == showDirCB)
                {
				showDir = false;
                ac.setShowDir(showDir);
                clearAndPaint();
                Prefs.set("Astronomy_Tool.showDir",showDir);
                }
            else if(source == showXYCB)
                {
				showXY = false;
                ac.setShowXY(showXY);
                clearAndPaint();
                Prefs.set("Astronomy_Tool.showXY",showXY);
                }
            else if(source == invertNoneRB)
                {
                invertNoneRB.setState(true);
                }
            else if(source == invertXRB)
                {
				invertXRB.setState(true);
                }
            else if(source == invertYRB)
                {
				invertYRB.setState(true);
                }
            else if(source == invertXYRB)
                {
				invertXYRB.setState(true);
                }
            else if(source == rotate0RB)
                {
				rotate0RB.setState(true);
                }
            else if(source == rotate90RB)
                {
				rotate90RB.setState(true);
                }
            else if(source == rotate180RB)
                {
				rotate180RB.setState(true);
                }
            else if(source == rotate270RB)
                {
				rotate270RB.setState(true);
                }
            else if (source == startupAutoLevelRB)
                {
                startupAutoLevelRB.setState(true);
                setAutoLevels(null);
                }
            else if (source == usePreviousLevelsRB)
                {
                usePreviousLevelsRB.setState(true);
                }
             else if (source == useFullRangeRB)
                {
                useFullRangeRB.setState(true);
                min = minValue;
                max = maxValue;
                updatePanelValues();
                }
            }
        }

         void setOrientation() {
            ac.setOrientation(invertX, invertY, rotation);
            netFlipX = ac.getNetFlipX();
            netFlipY = ac.getNetFlipY();
            netRotate = ac.getNetRotate();
            clearAndPaint();
            }


            public void actionPerformed(ActionEvent e) {
                Object b = e.getSource();
                currentSlice = imp.getCurrentSlice();

//------FILE menu--------------------------------------------------------------------------------------

                if (b==openMenuItem)
                    {
                    ImagePlus imp2 = IJ.openImage();
                    if (imp2 != null)
                        {
                        StackProcessor sp = new StackProcessor(imp.getStack(), imp2.getProcessor());
                        ImageStack s2 = imp2.getImageStack();
                        imp.setStack(s2);
                        imp.setFileInfo(imp2.getFileInfo());
                        copyImageProperties(imp2);
                        imp.setProcessor(imp2.getTitle(), imp2.getProcessor());
                        setAstroProcessor(false);
                        }
                    }
                else if (b==openInNewWindowMenuItem)
                    {
                    IJ.run("Open...");
                    }
                else if (b==openSeqMenuItem)
                    {
//				    String path = IJ.getDirectory ("current");
//                    DirectoryChooser.setDefaultDirectory(path);
//                    DirectoryChooser od = new DirectoryChooser ("Open Image Sequence...");
//        			if (od.getDirectory() != null)
//                        {
//                        path = od.getDirectory();
                        ImagePlus imp2 = FolderOpener.open(null);
                        StackProcessor sp = new StackProcessor(imp.getStack(), imp2.getProcessor());
                        ImageStack s2 = imp2.getImageStack();
                        imp.setStack(s2);
                        imp.setFileInfo(imp2.getFileInfo());
                        copyImageProperties(imp2);
                        imp.setProcessor(imp2.getTitle(), imp2.getProcessor());
                        setAstroProcessor(false);
//                        }
                    }
                else if (b==openSeqInNewWindowMenuItem)
                    {
                    IJ.run("Image Sequence...");
                    }
                else if (b==openMeasurementsTableMenuItem)
                    {
                    IJ.runPlugIn("Read_MeasurementTable","");
                    }
                else if (b==saveMenuItem)
                    {
                    IJ.saveAs(""+imp.getTitle().substring(imp.getTitle().lastIndexOf('.')),""+imp.getTitle());
                    }
                else if (b==saveFitsMenuItem)
                    {
                    IJ.run("FITS...");
                    }
                else if (b==saveTiffMenuItem)
                    {
                    IJ.run("Tiff...");
                    }
                else if (b==saveJpegMenuItem)
                    {
                    IJ.run("Jpeg...");
                    }
                else if (b==saveGifMenuItem)
                    {
                    IJ.run("Gif...");
                    }
                else if (b==savePngMenuItem)
                    {
                    IJ.run("PNG...");
                    }
                else if (b==saveBmpMenuItem)
                    {
                    IJ.run("BMP...");
                    }
                else if (b==saveAviMenuItem)
                    {
                    IJ.run("AVI... ");
                    }
                else if (b==saveStackSequenceMenuItem)
                    {
                    IJ.run("Image Sequence... ");
                    }
                else if(b == exitMenuItem)
                    {
                    saveAndClose(true);
                    }

//-----PREFERENCES menu -------------------------------------------------------------

                else if(b == setPixelScaleMenuItem)
                    {
                    setPixelScaleDialog();
                    }
                else if(b == setZoomIndicatorSizeMenuItem)
                    {
                    setZoomIndicatorSizeDialog();
                    }
                else if(b == dirAngleMenuItem)
                    {
                    setDirAngleDialog();
                    }
              // Other are checkboxMenuItems



//-----VIEW menu --------------------------------------------------------------------

                else if(b == clearOverlayMenuItem)
                    {
                    IJ.runPlugIn("Clear_Overlay", "");
                    }

               // Others are checkboxMenuItems

//-----EDIT menu --------------------------------------------------------------------

                else if(b == apertureSettingsMenuItem)
                    {
                    IJ.runPlugIn("Set_Aperture", "");
                    }
                else if(b == measurementSettingsMenuItem)
                    {
                    IJ.run("Set Measurements...", "");
                    }
                else if(b == editFitsHeaderMenuItem)
                    {
                    IJ.runPlugIn("FITS_Header_Editor", "");
                    }
                else if(b == stackSorterMenuItem)
                    {
                    IJ.runPlugIn("Stack_Sorter", "");
                    }

//-----PROCESS menu ------------------------------------------------------------------


                else if(b == dataReducerMenuItem)
                    {
                    IJ.runPlugIn("Data_Processor", "");
                    }
                else if(b == imageCalcMenuItem)
                    {
                    IJ.run("Image Calculator...", "");
                    }
                else if(b == removeOutliersMenuItem)
                    {
                    IJ.run("Remove Outliers...", "");
                    }
                else if(b == smoothMenuItem)
                    {
                    IJ.run("Smooth", "");
                    }
                else if(b == sharpenMenuItem)
                    {
                    IJ.run("Sharpen", "");
                    }
                else if(b == normalizeStackMenuItem)
                    {
                    IJ.runPlugIn("Normalize_Stack", "");
                    }
                else if(b == alignStackMenuItem)
                    {
                    IJ.runPlugIn("Stack_Aligner", "");
                    }
                else if(b == imageStabilizerMenuItem)
                    {
                    IJ.runPlugIn("Image_Stabilizer", "");
                    }
                else if(b == imageStabilizerApplyMenuItem)
                    {
                    IJ.runPlugIn("Image_Stabilizer_Log_Applier", "");
                    }
                else if(b == shiftImageMenuItem)
                    {
                    IJ.runPlugIn("Image_Shifter", "");
                    }
                else if(b == selectBestFramesMenuItem)
                    {
                    IJ.runPlugIn("Select_Frames_With_Best_Edges", "");
                    }
                else if (b==flipDataXMenuItem)
                    {
                    flipDataX = true;
                    startDataFlipRotate();
                    }
                else if (b==flipDataYMenuItem)
                    {
                    flipDataY = true;
                    startDataFlipRotate();
                    }
                else if (b==rotateDataCWMenuItem)
                    {
                    rotateDataCW = true;
                    startDataFlipRotate();
                    }
                else if (b==rotateDataCCWMenuItem)
                    {
                    rotateDataCCW = true;
                    startDataFlipRotate();
                    }



//-----ANALYZE menu ------------------------------------------------------------------


                else if(b == multiApertureMenuItem)
                    {
                    IJ.runPlugIn("MultiAperture_", "");
                    }
                else if(b == multiPlotMenuItem)
                    {
                    IJ.runPlugIn("Plot_IJ", "");
                    }
                else if(b == measurementMenuItem)
                    {
                    IJ.run("Measure", "");
                    }
                else if(b == seeingProfileMenuItem)
                    {
                    IJ.runPlugIn("Seeing_Profile", "");
                    }
                else if(b == staticProfilerMenuItem)
                    {
                    IJ.run("Plot Profile", "");
                    }
                else if(b == dynamicProfilerMenuItem)
                    {
                    IJ.runPlugIn("Dynamic_Profiler", "");
                    }
                else if(b == azimuthalAverageMenuItem)
                    {
                    IJ.runPlugIn("Azimuthal_Average", "imp");
                    }
                else if(b == threeDSurfacePlotMenuItem)
                    {
                    IJ.runPlugIn("Interactive_3D_Surface_Plot", "");
                    }






//-----TEST fields ------------------------------------------------------------------

                else if(b == minTextField)
                    {
                    min = Double.parseDouble(minTextField.getText().replaceAll(",", ""));
                    Prefs.set("Astronomy_Tool.savedMin", savedMin);
                    updatePanelValues();
                    }
                else if(b == maxTextField)
                    {
                    max = Double.parseDouble(maxTextField.getText().replaceAll(",", ""));
                    Prefs.set("Astronomy_Tool.savedMin", savedMin);
                    updatePanelValues();
                    }
                else if(b == minValueTextField)
                    {
                    minValue = Double.parseDouble(minValueTextField.getText().replaceAll(",", ""));
                    if (imp.getType()==ImagePlus.GRAY16 && maxValue - minValue < 256)
                        minValue = maxValue - 255;
                    if (minValue > maxValue) minValue = maxValue;
                    if (min < minValue) min = minValue;
                    fixedMinValue = minValue;
                    Prefs.set("Astronomy_Tool.fixedMinValue", fixedMinValue);
                    updateMinMaxValues();
                    }
                else if(b == maxValueTextField)
                    {
                    maxValue = Double.parseDouble(maxValueTextField.getText().replaceAll(",", ""));
                    if (imp.getType()==ImagePlus.GRAY16 && maxValue - minValue < 256)
                        maxValue = minValue + 255;
                    if (maxValue < minValue) maxValue = minValue;
                    if (max > maxValue) max = maxValue;
                    fixedMaxValue = maxValue;
                    Prefs.set("Astronomy_Tool.fixedMaxValue", fixedMaxValue);
                    updateMinMaxValues();
                    }


//-----BUTTONS---------------------------------------------------------------------

                else if(b == buttonHeader)
                    {
                    IJ.run("Show Info...");
                    }
                else if(b == buttonNegative)
                    {
//                    for(int i = 1; i <= stackSize; i++)
//                            {
                            useInvertingLut = !useInvertingLut;
                            ImageProcessor ip = imp.getProcessor();
//                            imp.setSlice(i);
                            if (useInvertingLut != ip.isInvertedLut() && !ip.isColorLut())
                                ip.invertLut();
//                            imp.getProcessor().invert();
                            imp.updateAndDraw();
//                            }
//                    if (stackSize > 1)
//                            {
//                            imp.setSlice(currentSlice);
//                            imp.updateAndDraw();
//                            }
                    }
                else if (b==buttonFlipX)
                    {
                    for(int i = 1; i <= stackSize; i++)
                            {
                            imp.setSlice(i);
                            imp.getProcessor().flipHorizontal();
                            imp.updateAndDraw();
                            }
                    if (stackSize > 1)
                            {
                            imp.setSlice(currentSlice);
                            imp.updateAndDraw();
                            }
                    }
                else if (b==buttonFlipY)
                    {
                    for(int i = 1; i <= stackSize; i++)
                            {
                            imp.setSlice(i);
                            imp.getProcessor().flipVertical();
                            imp.updateAndDraw();
                            }
                    if (stackSize > 1)
                            {
                            imp.setSlice(currentSlice);
                            imp.updateAndDraw();
                            }
                    }
                else if (b==buttonRotCCW)
                    {
                    Calibration cal = imp.getCalibration();
                    currentSlice = imp.getCurrentSlice();
                    ImageProcessor ip = imp.getProcessor();
                    icWidth = ac.getWidth();
                    icHeight = ac.getHeight();
                    magnification = ac.getMagnification();
                    if (ipWidth != ipHeight)
                            {
                            StackProcessor sp = new StackProcessor(imp.getStack(), ip);
                            ImageStack s2 = null;
                            s2 = sp.rotateLeft();
                            imp.setStack(null, s2);
                            if (IJVersion.compareTo("1.42q") > 0 && IJVersion.compareTo("1.44f") < 0 ) 
                                imp = WindowManager.getImage(impTitle); 
                            double pixelWidth = cal.pixelWidth;
                            cal.pixelWidth = cal.pixelHeight;
                            cal.pixelHeight = pixelWidth;
                            imp.setCalibration(cal);
                            if (imp.getStackSize() > 1)
                                stackRotated = true;
                            layoutContainer(this);
                            ac.repaint();
//                            refreshAstroWindow();
                            }
                    else
                            {
                            for(int i = 1; i <= stackSize; i++)
                                    {
                                    imp.setSlice(i);
                                    ip = imp.getProcessor().rotateLeft();
                                    imp.setProcessor(null, ip);
                                    }
                            double pixelWidth = cal.pixelWidth;
                            cal.pixelWidth = cal.pixelHeight;
                            cal.pixelHeight = pixelWidth;  
                            imp.setCalibration(cal);
                            imp.setSlice(currentSlice);
                            }
                    }
                else if (b==buttonRotCW)
                    {
                    Calibration cal = imp.getCalibration();
                    ImageProcessor ip = imp.getProcessor();
                    currentSlice = imp.getCurrentSlice();
                    if (ipWidth != ipHeight)
                            {
                            StackProcessor sp = new StackProcessor(imp.getStack(), ip);
                            ImageStack s2 = null;
                            s2 = sp.rotateRight();
                            imp.setStack(null, s2);
                            if (IJVersion.compareTo("1.42q") > 0 && IJVersion.compareTo("1.44f") < 0 ) 
                                imp = WindowManager.getImage(impTitle);
                            double pixelWidth = cal.pixelWidth;
                            cal.pixelWidth = cal.pixelHeight;
                            cal.pixelHeight = pixelWidth;
                            imp.setCalibration(cal);
                            if (imp.getStackSize() > 1)
                                stackRotated = true;
                            ac.repaint();
//                            refreshAstroWindow();
                            }
                    else
                            {

//                            ImagePlus impr = NewImage.createFloatImage("temp", ipHeight, ipWidth, stackSize, 0);
//                            ImageProcessor ipr = impr.getProcessor();
//                            for(int i = 1; i <= stackSize; i++)
//                                    {
//                                    imp.setSlice(i);
//                                    impr.setSlice(i);
//                                    ipr = imp.getProcessor().rotateRight();
//                                    impr.setProcessor(null, ipr);
//                                    }
//                            ipr = impr.getProcessor();
//                            imp.flush();
//                            imp.setProcessor(null, ipr);
//                            ip = imp.getProcessor();
//                            imp.setWindow(iw);
                            for(int i = 1; i <= stackSize; i++)
                                    {
                                    imp.setSlice(i);
                                    ip = imp.getProcessor().rotateLeft();
                                    imp.setProcessor(null, ip);
                                    }
                            double pixelWidth = cal.pixelWidth;
                            cal.pixelWidth = cal.pixelHeight;
                            cal.pixelHeight = pixelWidth;
                            imp.setCalibration(cal);
                            imp.setSlice(currentSlice);
//                            Graphics g = ac.getGraphics();
//                            g.clearRect(0, 0, ac.getWidth(), ac.getHeight());
//                            ac.update(g);
                            ac.repaint();
                            }
                    }
//                else if (b==buttonLUT)
//                    {
//                    IJ.run("Image>Lookup Tables");
//                    }
                else if (b==buttonZoomOut)
                    {
                    zoomOut(startDragScreenX, startDragScreenY, true, true);
                    }
                else if (b==buttonZoomIn)
                    {
                    zoomIn(startDragScreenX, startDragScreenY, true, true, 0.0);
                    }
                else if (b==buttonZoomInFast)
                    {
                    zoomIn(startDragScreenX, startDragScreenY, true, true, 8.0);
                    }
                else if (b==buttonFit)
                    {
                    if ((e.getModifiers()& e.ALT_MASK) != 0 )
                        fillNotFit =true;
                    fitImageToCanvas();
                    }
                else if (b==buttonAutoLevels)
                    {
                    setAutoLevels(null);
                    }
            }

    void copyImageProperties(ImagePlus impp)
            {
            //CLEAR PROPERTIES FROM OPENIMAGE
            Enumeration enProps;
            String key;
            Properties props = imp.getProperties();
            if (props != null)
                {
                enProps = props.propertyNames();
                key = "";
                while (enProps.hasMoreElements())
                        {
                        key = (String) enProps.nextElement();
                        imp.setProperty(key, null);
                        }
                }
            // COPY NEW PROPERTIES TO OPEN WINDOW IMAGEPLUS
            props = impp.getProperties();
            if (props != null)
                {
                enProps = props.propertyNames();
                key = "";
                while (enProps.hasMoreElements())
                        {
                        key = (String) enProps.nextElement();
                        imp.setProperty(key, props.getProperty(key));
                        }
                }
            }

    void setPixelScaleDialog()
        {
        GenericDialog gd = new GenericDialog ("Set Pixel Scale");

        gd.addMessage ("Enter 0 to report length in pixels.");
		gd.addNumericField ("Pixel scale: ",pixelScale,4,8, "(seconds of arc per pixel)");
        gd.addMessage ("");

		gd.showDialog();
		if (gd.wasCanceled()) return;
		pixelScale = gd.getNextNumber();
        Prefs.set("Astronomy_Tool.pixelScale", pixelScale);
        }

    void setZoomIndicatorSizeDialog()
        {
        GenericDialog gd = new GenericDialog ("Set Zoom Indicator Size");

		gd.addNumericField ("Zoom indicator height: ",ac.zoomIndicatorSize,0,6,"(pixels)");
        gd.addMessage ("(width is scaled according to image aspect ratio)");

		gd.showDialog();
		if (gd.wasCanceled()) return;
		ac.zoomIndicatorSize = (int)gd.getNextNumber();
        ac.updateZoomBoxParameters();
        clearAndPaint();
        Prefs.set("Astronomy_Tool.zoomIndicatorSize", ac.zoomIndicatorSize);
        }

    void setDirAngleDialog()
        {
        GenericDialog gd = new GenericDialog ("Set Direction Indicator Angle");
        gd.addMessage ("Direction angle is used when WCS is not available for an image.");
		gd.addNumericField ("Direction indicator angle: ",ac.dirAngle,0,6,"(degrees)");
        gd.addMessage ("(north up = 0, north left = 90, etc.)");

		gd.showDialog();
		if (gd.wasCanceled()) return;
        ac.dirAngle=gd.getNextNumber();
        ac.updateZoomBoxParameters();
        clearAndPaint();
        Prefs.set("Astronomy_Tool.dirAngle", ac.dirAngle);
        }

    void updateMinMaxValues()
        {
        if (startupAutoLevel)
            {
            setAutoLevels(null);
            }
        else if (startupPrevLevels)
            {
            updatePanelValues();
            }
        else
            {
            min = minValue;
            max = maxValue;
            updatePanelValues();
            }
        }

	void startDataFlipRotate()
		{
		try	{
			rotateTask = new TimerTask ()
				{
				public void run ()
					{
                    if (flipDataX) invertData("x", true);
                    else if (flipDataY) invertData("y", true);
                    else if (rotateDataCCW) rotateData("CCW", true);
                    else if (rotateDataCW) rotateData("CW", true);
                    flipDataX = false;
                    flipDataY = false;
                    rotateDataCCW = false;
                    rotateDataCW = false;

//                    adjustImageRotation(IMAGE_UPDATE);
                    rotateTask = null;
                    rotateTaskTimer = null;
                    }
                };
            rotateTaskTimer = new java.util.Timer();
            rotateTaskTimer.schedule (rotateTask,0);
            }

        catch (Exception e)
            {
            IJ.showMessage ("Error starting rotation task : "+e.getMessage());
            }
        }


//       void adjustImageRotation(boolean updateImage)
//            {
//            if (fX != prevInvertX)
//                {
//                invert("x", updateImage);
//                prevInvertX = invertX;
//
//                }
//            if (invertY != prevInvertY)
//                {
//                invert("y", updateImage);
//                prevInvertY = invertY;
//
//                }
//            if (rotation != prevRotation)
//                {
//                int del = rotation - prevRotation;
//                if (del == 1) rotate("left", updateImage);
//                else if (del ==-1) rotate("right", updateImage);
//                else if (del == 2 || del == - 2) {invert("x", updateImage); invert("y", updateImage);}
//                else if (del == 3) rotate("right", updateImage);
//                else if (del ==-3) rotate("left", updateImage);
//                prevRotation = rotation;
//
//                }
//            }
       
       void invertData(String dir, boolean updateImage)
            {
            stackSize = imp.getStackSize();
            currentSlice = imp.getCurrentSlice();
            ImageProcessor ip = imp.getProcessor();
            if (!dir.equals("x") && !dir.equals("y"))
                return;           
            for(int i = 1; i <= stackSize; i++)
                    {
                    minMaxChanged = true;
                    imp.setSlice(i);
                    IJ.showStatus("Invert-"+dir+": "+i+"/"+stackSize);
                    IJ.showProgress((double)i/(double)stackSize);
                    if (dir.equals("x"))
                        {minMaxChanged = true; imp.getProcessor().flipHorizontal();}
                    else
                        {minMaxChanged = true; imp.getProcessor().flipVertical();}
                    if (updateImage) {minMaxChanged = true; imp.updateAndDraw();}
                    }
            if (stackSize > 1)
                    {
                    minMaxChanged = true;
                    imp.setSlice(currentSlice);
                    if (updateImage) {minMaxChanged = true; imp.updateAndDraw();}
                    }
            }

       void rotateData(String dir, boolean updateImage)
            {
            if (!dir.equals("CCW") && !dir.equals("CW"))
                return;
            Calibration cal = imp.getCalibration();
            ImageProcessor ip = imp.getProcessor();
            icWidth = ac.getWidth();
            icHeight = ac.getHeight();
            ipWidth = imp.getWidth();
            ipHeight = imp.getHeight();
            stackSize = imp.getStackSize();
            currentSlice = imp.getCurrentSlice();
            if (ipWidth != ipHeight)
                {
                StackProcessor sp = new StackProcessor(imp.getStack(), ip);
                ImageStack s2 = null;
                if (dir.equals("CW")) {s2 = sp.rotateLeft();}
                else {s2 = sp.rotateRight();}
                imp.setStack(null, s2);
                if (IJVersion.compareTo("1.42q") > 0 && IJVersion.compareTo("1.44f") < 0 )
                    imp = WindowManager.getImage(impTitle);
                double pixelWidth = cal.pixelWidth;
                cal.pixelWidth = cal.pixelHeight;
                cal.pixelHeight = pixelWidth;
                minMaxChanged = true;
                imp.setCalibration(cal);
                if (imp.getStackSize() > 1)
                    stackRotated = true;
                if (updateImage)
                    {
                    dataRotated = true;
//                    layoutContainer(this);
                    clearAndPaint();
                    }
                }
            else
                {
                for (int i = 1; i <= stackSize; i++)
                    {
                    imp.setSlice(i);
                    IJ.showStatus("Rotate: "+i+"/"+stackSize);
                    IJ.showProgress((double)i/(double)stackSize);
                    if (dir.equals("CW")) {ip = imp.getProcessor().rotateLeft();}
                    else {ip = imp.getProcessor().rotateRight();}
                    imp.setProcessor(null, ip);
                    if (updateImage) {imp.updateAndDraw();}
                    }
                double pixelWidth = cal.pixelWidth;
                cal.pixelWidth = cal.pixelHeight;
                cal.pixelHeight = pixelWidth;
                imp.setCalibration(cal);
                imp.setSlice(currentSlice);
                }
            if (stackSize > 1)
                    {
                    imp.setSlice(currentSlice);
                    if (updateImage) {imp.updateAndDraw();}
                    }
            }

       void fitImageToCanvas(){

            int canvasWidth = newCanvasWidth();
            int canvasHeight = newCanvasHeight();


            double xmag = (double)canvasWidth/(double)imp.getWidth();
            double ymag = (double)canvasHeight/(double)imp.getHeight();
//            if (ac.getNetRotate())
//                {
//                xmag = (double)newCanvasWidth()/(double)imp.getHeight();
//                ymag = (double)newCanvasHeight()/(double)imp.getWidth();
//                }

            ac.setDrawingSize(canvasWidth, canvasHeight);
            if (fillNotFit)
                {
                fillNotFit = false;
                ac.setMagnification(Math.max(xmag,ymag));
                }
            else
                {
                ac.setMagnification(Math.min(xmag,ymag));
                }
            
            Rectangle r = new Rectangle((int)((imp.getWidth()/2.0) - canvasWidth/ac.getMagnification()/2.0),
                    (int)((imp.getHeight()/2.0 - canvasHeight/ac.getMagnification()/2.0 )),
                    (int)((double)newCanvasWidth()/ac.getMagnification()),
                    (int)((double)newCanvasHeight()/ac.getMagnification()));
            ac.setSourceRect(r);

            magnification = ac.getMagnification();
            if (!fillNotFit)
                {
                Graphics g = ac.getGraphics();
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, ac.getWidth(), ac.getHeight());
                ac.paint(g);
                }
            clearAndPaint();

            srcRect = ac.getSrcRect();
            savedPanX = srcRect.x;
            savedPanY = srcRect.y;
            savedPanHeight = srcRect.height;
            savedPanWidth = srcRect.width;
            savedMag = ac.getMagnification();
            savedICWidth = ac.getWidth();
            savedICHeight = ac.getHeight();
            Prefs.set("Astronomy_Tool.savedMag", savedMag);
            Prefs.set("Astronomy_Tool.savedICWidth", savedICWidth);
            Prefs.set("Astronomy_Tool.savedICHeight", savedICHeight);
            Prefs.set("Astronomy_Tool.savedPanX", savedPanX);
            Prefs.set("Astronomy_Tool.savedPanY", savedPanY);
            Prefs.set("Astronomy_Tool.savedPanHeight", savedPanHeight);
            Prefs.set("Astronomy_Tool.savedPanWidth", imp.getWidth());
            setImageEdges();
            }

       int newCanvasWidth()
            {
            return getWidth() - getInsets().left - getInsets().right - 12;
            }

       int newCanvasHeight()
            {
            return this.getHeight() - this.getInsets().top
                    - this.getInsets().bottom - otherPanelsHeight
                    - stackSliders.length*18 - 12;
            }

       void clearAndPaint(){
            ac.resetDoubleBuffer();
            
//            Graphics g = ac.getGraphics();
//            g.setColor(Color.WHITE);
//            g.fillRect(0, 0, ac.getWidth(), ac.getHeight());
//            ac.paint(g);
//            ac.repaint();
//            IJ.log("clearandPaint");
            if (!IJ.isMacOSX())
                {
                Graphics g = ac.getGraphics();
                int x1 = ac.screenX(0) > 0 ? ac.screenX(0) : 0;    //top left screen x-location
                int y1 = ac.screenY(0) > 0 ? ac.screenY(0) : 0;    //top left screen y-location
                int x2 = ac.screenX(imp.getWidth()) < ac.getWidth() ? ac.screenX(imp.getWidth()) : ac.getWidth();    //bottom right screen x-location
                int y2 = ac.screenY(imp.getHeight()) < ac.getHeight() ? ac.screenY(imp.getHeight()) : ac.getHeight();    //bottom right screen y-location

                g.setColor(Color.WHITE);
                if (x1 >= 0)
                    g.fillRect(0, 0, x1, ac.getHeight());
                if (y1 >= 0)
                    g.fillRect(x1, 0 , ac.getWidth()-x1, y1);
                if (x2 <= ac.getWidth())
                    g.fillRect(x2, y1 , ac.getWidth()-x2, ac.getHeight()-y1);
                if (y2 <= ac.getHeight())
                    g.fillRect(x1, y2 , x2-x1, ac.getHeight()-y2);
                ac.paint(g);
               }

            ac.repaint();
            }


//       public void refreshAstroWindow()
//                {
//                double newMag, oldMag;
//                imageWindow = imp.getWindow();
//                ac = OverlayCanvas.getOverlayCanvas(imp);
//                oldMag = ac.getMagnification();
//                ImageProcessor ip = imp.getProcessor();
//
//                ipWidth = ip.getWidth();
//                ipHeight = ip.getHeight();
//                icWidth = ac.getWidth();
//                icHeight = ac.getHeight();
//                imageHeight = imageWindow.getHeight();
//                imageWidth = imageWindow.getWidth();
//
//                int height = imageHeight-58-otherPanelsHeight-stackSliders.length*18;
//                int width = imageWidth - 26;
//                if (width < MIN_FRAME_WIDTH) width = MIN_FRAME_WIDTH;
//
//                if (stackRotated && (IJVersion.compareTo("1.44f") >= 0)) {
//                    newMag = Math.max((double)width/(double)ipHeight, (double)height/(double)ipWidth);
//                    stackRotated = false;
//                } else {
//                    newMag = Math.max((double)width/(double)ipWidth, (double)height/(double)ipHeight);
//                }
//                ac.setDrawingSize((int)(ipWidth*newMag), (int)(ipHeight*newMag));
//                ac.setMagnification(newMag);
////                ac.repaint();
////                toolbar.removeMouseListener(toolbarMouseListener);
//                astroWindow = new AstroStackWindow(imp, (OverlayCanvas)ac, REFRESH, NORESIZE);
//                }

void setupListeners() {

//                imageWindow.removeComponentListener(this);
//                imageWindow.addComponentListener(this);
//                WindowListener[] wl = originalWindow.getWindowListeners();
//                if (wl.length>0)
//                    for (int i=0; i<wl.length; i++)
//                            imageWindow.addWindowListener(wl[i]);

        mwl = this.getMouseWheelListeners();
        if (mwl.length>0)
                for (int i=0; i<mwl.length; i++)
                        this.removeMouseWheelListener(mwl[i]);

        icmwl = ac.getMouseWheelListeners();
        if (icmwl.length>0)
                for (int i=0; i<icmwl.length; i++)
                        ac.removeMouseWheelListener(icmwl[i]);


//                mml = ac.getMouseMotionListeners();
//                if (mml.length>0)
//                        for (int i=0; i<mml.length; i++)
//                                ac.removeMouseMotionListener(mml[i]);

        ml = ac.getMouseListeners();
        if (ml.length>0)
                for (int i=0; i<ml.length; i++)
                        ac.removeMouseListener(ml[i]);

        ac.removeMouseMotionListener(this);
        ac.addMouseMotionListener(this);
        ac.removeMouseListener(this);
        ac.addMouseListener(this);
        ac.removeMouseWheelListener(this);
        ac.addMouseWheelListener(this);
        ac.addKeyListener(this);


//        FocusListener[] fl = super.getFocusListeners();
//        IJ.log("# of focus listeners = "+fl.length);
//        if (fl.length>0)
//                for (int i=0; i<fl.length; i++)
//                    super.removeFocusListener(fl[i]);
//
//
//        addFocusListener(new FocusListener(){
//            public void focusGained(FocusEvent e) {
//                IJ.log("focusGained: "+imp.getTitle());
//                WindowManager.setWindow(asw);
//            }
//            public void focusLost(FocusEvent e) {}});
//

//        this.addFocusListener(this);
        
        toolbar = Toolbar.getInstance();
        astronomyToolId = toolbar.getToolId("Astronomy_Tool");
        if (astronomyToolId != -1)
            toolbar.setTool(astronomyToolId);
        else
            astronomyToolId = -9999;
        apertureToolId = toolbar.getToolId("Aperture");
        if (apertureToolId == -1)
                apertureToolId = -9999;
        zoomToolId = Toolbar.MAGNIFIER;
        panToolId = Toolbar.HAND;
        toolbar.removeMouseListener(toolbarMouseListener);
        toolbar.addMouseListener(toolbarMouseListener);
    }


    void exitAstronomyTool() {

//        ac.removeMouseMotionListener(this);
//                ac.removeMouseWheelListener(this);

        ac.removeMouseListener(this);


//                if (mwl.length>0)
//                        for (int i=0; i<mwl.length; i++)
//                                imageWindow.addMouseWheelListener(mwl[i]);

//        if (mml.length>0)
//                for (int i=0; i<mml.length; i++)
//                        ac.removeMouseMotionListener(mml[i]);
//        if (mml.length>0)
//                for (int i=0; i<mml.length; i++)
//                        ac.addMouseMotionListener(mml[i]);

        if (ml.length>0)
                for (int i=0; i<ml.length; i++)
                        ac.removeMouseListener(ml[i]);
        if (ml.length>0)
                for (int i=0; i<ml.length; i++)
                        ac.addMouseListener(ml[i]);
        frameLocationX = this.getLocation().x;
        frameLocationY = this.getLocation().y;
        apertureOverlay.clear();
        ac.repaint(); 
        savePrefs();
        astronomyMode = false;
        imp.unlock();
        };

    void reenterAstronomyTool()  {

//        ac.addMouseMotionListener(this);
        ac.removeMouseListener(this);
        ac.addMouseListener(this);

//        if (mml.length>0)
//                for (int i=0; i<mml.length; i++)
//                        ac.removeMouseMotionListener(mml[i]);
        if (ml.length>0)
                for (int i=0; i<ml.length; i++)
                        ac.removeMouseListener(ml[i]);
        astronomyMode = true;
        ac.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        radius = (int)Prefs.get("aperture.radius", radius);
		rBack1 = (int)Prefs.get("aperture.rback1",rBack1);
		rBack2 = (int)Prefs.get("aperture.rback2",rBack2);
        removeBackStars = Prefs.get("Astronomy_Tool.removeBackStars", removeBackStars);
        photom.setSourceApertureRadius (radius);
        photom.setBackgroundApertureRadii (rBack1,rBack2);
        photom.setRemoveBackStars(removeBackStars);
        apertureOverlay.clear();
        ac.repaint();
        }

//	public void focusGained(FocusEvent e) {
//		WindowManager.setWindow(this);
//
//        if(IJ.isMacOSX())
//            {
//            IJ.wait(1);
//            setMenuBar(mainMenuBar);
//            }
//        IJ.log("focus gained");
//
//	}
//
//	public void focusLost(FocusEvent e) {}


    MouseListener toolbarMouseListener = new MouseListener() {
                public void mousePressed(MouseEvent e) {
                    IJ.wait(250);
                    currentToolId = toolbar.getToolId();
                    if (currentToolId == astronomyToolId ||
                        currentToolId == zoomToolId ||
                        currentToolId == apertureToolId ||
                        currentToolId == panToolId)
                        {
                        reenterAstronomyTool();
                        }
                    else
                        {
                        exitAstronomyTool();
                        }
                    }

                public void mouseReleased(MouseEvent e) { }

                public void mouseEntered(MouseEvent e) { }

                public void mouseExited(MouseEvent e) { }

                public void mouseClicked(MouseEvent e) { } };


        public void setAutoLevels(String windowName) {

//            if (windowName == null)
//                imp = WindowManager.getImage(impTitle);
//            else
//                imp = WindowManager.getImage(windowName);

            getStatistics();
            
            if (imp.getType()==ImagePlus.COLOR_RGB)
                {
                min = Math.max(stats.mean - 2.0*stats.stdDev, minValue);
                max = Math.min(stats.mean + 6.0*stats.stdDev, maxValue);
                }
            else
                {
                double ratio = 1.0;
//                if (useFixedMinMaxValues)
//                    ratio = (stats.max - stats.min)/(maxValue - minValue + 0.1);
                min = Math.max(stats.mean - 0.5*stats.stdDev*ratio, minValue);
                max = Math.min(stats.mean + 2.0*stats.stdDev*ratio, maxValue);
                }

            updatePanelValues();
            setImageEdges();
            savedMin = min;
            savedMax = max;
            Prefs.set("Astronomy_Tool.savedMin", savedMin);
            Prefs.set("Astronomy_Tool.savedMax", savedMax);

            radius = (int)Prefs.get("aperture.radius", radius);
            rBack1 = (int)Prefs.get("aperture.rback1", rBack1);
            rBack2 = (int)Prefs.get("aperture.rback2", rBack2);
            photom.setSourceApertureRadius (radius);
            photom.setBackgroundApertureRadii (rBack1,rBack2);
            photom.setRemoveBackStars(removeBackStars);
            }

    protected void getStatistics()
            {
            Roi roi = imp.getRoi();
            imp.killRoi();
            stats = imp.getStatistics(ImageStatistics.MEAN+ImageStatistics.MIN_MAX+ImageStatistics.STD_DEV);
            imp.setRoi(roi);
            }

     protected void getBiSliderStatistics()
            {
            Roi roi = imp.getRoi();
            imp.killRoi();
            stats = imp.getStatistics((ImageStatistics.MEAN+ImageStatistics.MIN_MAX+
                                           ImageStatistics.STD_DEV), BISLISER_SEGMENTS,
                                           minValue, maxValue);
            imp.setRoi(roi);
            }

    public void setAstroProcessor(boolean adjustImage ) {

            ImageProcessor ip = imp.getProcessor();
//            prevInvertX = false;
//            prevInvertY = false;
//            prevRotation = 0;
//            IJ.log("setAStroProcessor");
//            if (adjustImage)
//                adjustImageRotation(NO_IMAGE_UPDATE);



            impTitle = imp.getTitle();
            this.setTitle(impTitle);
            stackSize = imp.getStackSize();
            getStatistics();
            if (imp.getType() == ImagePlus.COLOR_256 || imp.getType() == ImagePlus.COLOR_RGB || imp.getType() == ImagePlus.GRAY8)
                {
                useFixedMinMaxValues = false;
                useFixedMinMaxValuesCB.setState(false);
                minValue = 0;
                maxValue = 255;
                }
            else
                {
                maxValue = useFixedMinMaxValues ? fixedMaxValue : stats.max;
                minValue = useFixedMinMaxValues ? fixedMinValue : stats.min;
                if (imp.getType() == ImagePlus.GRAY16 && maxValue - minValue < 256)
                     maxValue = minValue + 255;
                }

            if (startupAutoLevel)
                {
                if (imp.getType()==ImagePlus.COLOR_RGB)
                    {
                    min = Math.max(stats.mean - 2.0*stats.stdDev, minValue);
                    max = Math.min(stats.mean + 6.0*stats.stdDev, maxValue);
                    }
                else
                    {
                    min = Math.max(stats.mean - 0.5*stats.stdDev, minValue);
                    max = Math.min(stats.mean + 2.0*stats.stdDev, maxValue);
                    }
                }
            else if (!startupPrevLevels)
                {
                min = minValue;
                max = maxValue;
                }
//            infoTextField.setText(""+super.createSubtitle());
            layoutContainer(this);
            ac.updateZoomBoxParameters();
            updatePanelValues();
            setImageEdges();
            radius = (int)Prefs.get("aperture.radius", radius);
            rBack1 = (int)Prefs.get("aperture.rback1", rBack1);
            rBack2 = (int)Prefs.get("aperture.rback2", rBack2);
            photom = new Photometer (imp.getCalibration());
            photom.setSourceApertureRadius (radius);
            photom.setBackgroundApertureRadii (rBack1,rBack2);
            photom.setRemoveBackStars(removeBackStars);
            double value = ip.getPixelValue(lastImageX, lastImageY);
            valueTextField.setText(fourPlaces.format(value));
            photom.measure (ip,(double)lastImageX,(double)lastImageY);
            peakLabel.setText("Peak:");
            peakTextField.setText(fourPlaces.format(photom.peakBrightness()));
            lengthLabel.setText("Int Cnts:");
            lengthTextField.setText(fourPlaces.format(photom.sourceBrightness()));
            
            }

    void updatePanelValues()
            {
            ImageProcessor ip = imp.getProcessor();

//            sliderMultiplier = 100.0*(double)(ac.getWidth())/(double)(maxValue - minValue);
//            if (sliderMultiplier > 1000.0) sliderMultiplier = 1000.0;
//
//            maxSlider.setMinimum((int)((minValue+sliderShift)*sliderMultiplier));
//            maxSlider.setMaximum((int)((maxValue+sliderShift)*sliderMultiplier));
//            maxSlider.setValue((int)((max+sliderShift)*sliderMultiplier));
//
//            minSlider.setMinimum((int)((minValue+sliderShift)*sliderMultiplier));
//            minSlider.setMaximum((int)((maxValue+sliderShift)*sliderMultiplier));
//            minSlider.setValue((int)((min+sliderShift)*sliderMultiplier));
            if (imp.getType()==ImagePlus.COLOR_RGB)
                {
                ip.reset();
                }

            getBiSliderStatistics();

            if (min < minValue) min = minValue;
            if (min > maxValue) min = maxValue;
            if (max > maxValue) max = maxValue;
            if (max < min) max = min;
            
            histogram = stats.histogram;

            for (int i=0; i<histogram.length; i++)
                {
                if (histogram[i] <= 1)
                    logHistogram[i] = 0;
                else
                    logHistogram[i] = Math.log(histogram[i]);
                if (logHistogram[i] > histMax)
                    histMax = logHistogram[i];
                }

            minMaxBiSlider.setParameters(BiSlider.RGB, false,
                                         ((maxValue - minValue)/(double)BISLISER_SEGMENTS),
                                         Color.BLACK, Color.BLACK, minValue, maxValue, min, max);

            minValueTextField.setText(""+fourPlaces.format(minValue));
            maxValueTextField.setText(""+fourPlaces.format(maxValue));

            minTextField.setText(""+fourPlaces.format(min));
            maxTextField.setText(""+fourPlaces.format(max));

            meanTextField.setText(""+fourPlaces.format(stats.mean));
            valueTextField.setText(""+fourPlaces.format(ip.getPixelValue(lastImageX, lastImageY)));

            imp.setDisplayRange(min, max);
            minMaxChanged = true;
            imp.updateAndDraw();

            }

    /** LAYOUT MANAGER METHODS **/

    /** Not used by this class. */
    public void addLayoutComponent(String name, Component comp) {
    }

    /** Not used by this class. */
    public void removeLayoutComponent(Component comp) {
    }

    /** Returns the preferred dimensions for this layout. */
    public Dimension preferredLayoutSize(Container target) {
		Dimension dim = new Dimension((int)newCanvasWidth(),(int)(this.getHeight()-56));

//		int nmembers = target.getComponentCount();
//		for (int i=0; i<nmembers; i++) {
//		    Component m = target.getComponent(i);
//			Dimension d = m.getPreferredSize();
//			dim.width = Math.max(dim.width, d.width);
//			if (i>0) dim.height += vgap;
//			dim.height += d.height;
//		}
//		Insets insets = target.getInsets();
//		dim.width += insets.left + insets.right + hgap*2;
//		dim.height += insets.top + insets.bottom + vgap*2;
        if (!redrawing )
            {
            ac.setDrawingSize((int)newCanvasWidth(), (int)newCanvasHeight());
            }

		return dim;
    }

    /** Returns the minimum dimensions for this layout. */
    public Dimension minimumLayoutSize(Container target) {
		return preferredLayoutSize(target);
//        return new Dimension(500, 500);
    }

    /** Centers the elements in the specified column, if there is any slack.*/
    private void moveComponents(Container target, int x, int y, int width, int height, int nmembers) {
//       IJ.log("moveComponents executed");
//    	int x2 = 0;
//	    y += height / 2;
//		for (int i=0; i<nmembers; i++) {
//		    Component m = target.getComponent(i);
//		    Dimension d = m.getSize();
//		    if (i==0 || d.height>60)
//		    	x2 = x + (width - d.width)/2;
//			m.setLocation(x2, y);
//			y += vgap + d.height;
//		}
    }

    /** Lays out the container and calls ImageCanvas.resizeCanvas()
		to adjust the image canvas size as needed. */
    public void layoutContainer(Container target) {
        if (!redrawing)
            {
            redrawing = true;
//		Insets insets = target.getInsets();
//		int nmembers = target.getComponentCount();
//		Dimension d;
//		int extraHeight = 0;
//		for (int i=1; i<nmembers; i++) {
//			Component m = target.getComponent(i);
//			d = m.getPreferredSize();
//			extraHeight += d.height;
//		}
//
//		d = target.getSize();
//		int preferredImageWidth = d.width - (insets.left + insets.right + hgap*2);
//		int preferredImageHeight = d.height - (insets.top + insets.bottom + vgap*2 + extraHeight);
//
//		resizeCanvas ((int)(imageWindow.getWidth() - 26), (int)(imageWindow.getHeight()-56-otherPanelsHeight-stackSliders.length*18));//(preferredImageWidth, preferredImageHeight);

//		int maxwidth = d.width - (insets.left + insets.right + hgap*2);
//		int maxheight = d.height - (insets.top + insets.bottom + vgap*2);
//		Dimension psize = preferredLayoutSize(target);
//		int x =  hgap + (d.width - psize.width)/2; //insets.left + hgap + (d.width - psize.width)/2;
//		int y = 0;
//		int colw = 0;

//		for (int i=0; i<nmembers; i++) {
//			Component m = target.getComponent(0);
//			Dimension d = m.getPreferredSize();
//			if ((m instanceof ScrollbarWithLabel) || (m instanceof Scrollbar)) {
//				int scrollbarWidth = target.getComponent(0).getPreferredSize().width;
//				Dimension minSize = m.getMinimumSize();
//				if (scrollbarWidth<minSize.width) scrollbarWidth = minSize.width;
//				m.setSize(scrollbarWidth, d.height);
//			} else
//				m.setSize(d.width, d.height);
//			if (y > 0) y += vgap;
//			y += d.height;
//			colw = Math.max(colw, d.width);
//		}
//		moveComponents(target, x, insets.top + vgap, colw, maxheight - y, nmembers);

//        else
//            {
            Dimension psize = preferredLayoutSize(target);
            Component m = target.getComponent(0);
            Dimension d = m.getPreferredSize();
            m.setSize(d.width, d.height);

//            }

            ac.setDrawingSize(newCanvasWidth(), newCanvasHeight());
            Rectangle r = new Rectangle(ac.getSrcRect());
            double xmag = (double)newCanvasWidth()/(double)(ac.getSrcRect().width);
            double ymag = (double)newCanvasHeight()/(double)(ac.getSrcRect().height);
            if (IJ.altKeyDown() || (r.x <= 12 && r.y <= 12 && r.x+r.width > imp.getWidth()- 12 && r.x+r.height>imp.getHeight() - 12))
                {
                xmag = (double)newCanvasWidth()/(double)(imp.getWidth());
                ymag = (double)newCanvasHeight()/(double)(imp.getHeight());
                ac.setMagnification(Math.min(xmag,ymag));
                r.x = 0;
                r.y = 0;
                }

            magnification = ac.getMagnification();
            
            r.width = (int)((double)newCanvasWidth()/magnification);
            r.height = (int)((double)newCanvasHeight()/magnification);
            ac.setSourceRect(r);
            
//            imageWindow = imp.getWindow();
//            ac = OverlayCanvas.getOverlayCanvas(imp);
            dstWidth = ac.getWidth();
            dstHeight = ac.getHeight();
            magnification = ac.getMagnification();
            srcRect = ac.getSrcRect();
//            maxBounds = imageWindow.getMaximumBounds();
//            imageWindow.setMaximizedBounds(new Rectangle (1000, 500));
            imageWidth = imp.getWidth();
            imageHeight = imp.getHeight();
            oldICWidth = this.getWidth();
            oldICHeight = this.getHeight();

            winWidth = this.getWidth();
            winHeight = this.getHeight();

            srcRect = ac.getSrcRect();
            savedPanX = srcRect.x;
            savedPanY = srcRect.y;
            savedPanHeight = srcRect.height;
            savedPanWidth = srcRect.width;
            savedMag = magnification;
            savedICWidth = newCanvasWidth();
            savedICHeight = newCanvasHeight();
            savedIpWidth = imp.getWidth();
            savedIpHeight = imp.getHeight();
            Prefs.set("Astronomy_Tool.savedPanX", savedPanX);
            Prefs.set("Astronomy_Tool.savedPanY", savedPanY);
            Prefs.set("Astronomy_Tool.savedPanHeight", savedPanHeight);
            Prefs.set("Astronomy_Tool.savedPanWidth", savedPanWidth);
            Prefs.set("Astronomy_Tool.savedMag", savedMag);
            Prefs.set("Astronomy_Tool.savedICWidth", savedICWidth);
            Prefs.set("Astronomy_Tool.savedICHeight", savedICHeight);
            Prefs.set("Astronomy_Tool.savedIpWidth", savedIpWidth);
            Prefs.set("Astronomy_Tool.savedIpHeight", savedIpHeight);
            setImageEdges();
            redrawing = false;
        }
    }

//	void setMaxBounds() {
//        IJ.log("setMaxBounds");
//        imageWindow.setMaximizedBounds(new Rectangle (500, 1000));
////		if (maxBoundsReset) {
////			maxBoundsReset = false;
////			imageWindow = imp.getWindow();
////			if (imageWindow!=null && !IJ.isLinux() && maxBounds!=null) {
////				imageWindow.setMaximizedBounds(maxBounds);
////				setMaxBoundsTime = System.currentTimeMillis();
////			}
////		}
//	}
//
//	void resetMaxBounds() {
//        IJ.log("resetMaxBounds");
//		imageWindow = imp.getWindow();
//		if (imageWindow!=null && (System.currentTimeMillis()-setMaxBoundsTime)>500L) {
//			imageWindow.setMaximizedBounds(maxBounds);
//			maxBoundsReset = true;
//		}
//	}
//
//
//	/** Enlarge the canvas if the user enlarges the window. */
//	void resizeCanvas(int width, int height) {
//        IJ.log("resize canvas");
//        ac.setDrawingSize(newCanvasWidth(), newCanvasHeight());
//        double xmag = (double)newCanvasWidth()/(double)(ac.getSrcRect().width);
//        double ymag = (double)newCanvasHeight()/(double)(ac.getSrcRect().height);
//        ac.setMagnification(Math.max(xmag,ymag));
//
//
//		imageWindow = imp.getWindow();
//        ac = OverlayCanvas.getOverlayCanvas(imp);
//        dstWidth = ac.getWidth();
//        dstHeight = ac.getHeight();
//        magnification = ac.getMagnification();
//        srcRect = ac.getSrcRect();
//        maxBounds = imageWindow.getMaximumBounds();
//
//        imageWidth = imp.getWidth();
//        imageHeight = imp.getHeight();
//
////		IJ.log("resizeCanvas: "+srcRect+" "+imageWidth+"  "+imageHeight+" "+width+"  "+height+" "+dstWidth+"  "+dstHeight+" "+win.maxBounds);
//		if (!maxBoundsReset&& (width>dstWidth||height>dstHeight)&&imageWindow!=null&&maxBounds!=null&&width!=maxBounds.width-10) {
//			if (resetMaxBoundsCount!=0)
//				resetMaxBounds(); // Works around problem that prevented window from being larger than maximized size
//			resetMaxBoundsCount++;
//        }
//
////		if (IJ.altKeyDown())
////			{fitToWindow(); return;}
////		if (srcRect.width<imageWidth || srcRect.height<imageHeight) {
////			if (width>imageWidth*magnification)
////				width = (int)(imageWidth*magnification);
////			if (height>imageHeight*magnification)
////				height = (int)(imageHeight*magnification);
////			ac.setDrawingSize(width, height);
////			srcRect.width = (int)(dstWidth/magnification);
////			srcRect.height = (int)(dstHeight/magnification);
////			if ((srcRect.x+srcRect.width)>imageWidth)
////				srcRect.x = imageWidth-srcRect.width;
////			if ((srcRect.y+srcRect.height)>imageHeight)
////				srcRect.y = imageHeight-srcRect.height;
////			ac.repaint();
//		}
        
//        winWidth = imageWindow.getWidth();
//        winHeight = imageWindow.getHeight();
//
//        IJ.log(""+oldWidth +" "+ winWidth +" "+ oldHeight +" "+ winHeight);
//
//        ac.setSize((int)(winWidth - 26), (int)(winHeight-otherPanelsHeight-stackSliders.length*18));
//        ac.setDrawingSize((int)(winWidth - 26), (int)(winHeight-otherPanelsHeight-stackSliders.length*18));
//        ac.setBounds(10, 50, (int)(winWidth - 26), (int)(winHeight-otherPanelsHeight-stackSliders.length*18));
//        ac.setMagnification(ac.getMagnification()*(double)winHeight/(double)oldHeight);
//        SpringUtil.makeCompactGrid (mainPanel, 5 + stackSliders.length, 1, 0,0,0,0);
//        ac.validate();
//        topPanelAL.validate();
//        topPanelAC.validate();
//        topPanelAR.validate();
//        topPanelBL.validate();
//        topPanelBC.validate();
//        topPanelBR.validate();
//        topPanelA.validate();
//        topPanelB.validate();
//        bottomPanelA.validate();
//        bottomPanelB.validate();
//        mainPanel.validate();

 //       mainPanel.repaint();

//        ac = OverlayCanvas.getOverlayCanvas(imp);
//        icHeight = ac.getHeight();
//        icWidth = ac.getWidth();
//		//IJ.log("resizeCanvas2: "+srcRect+" "+dstWidth+"  "+dstHeight+" "+width+"  "+height);
//	}



    /** Handle the key typed event from the text field. */
    public void keyTyped(KeyEvent e) {

    }

    /** Handle the key-pressed event from the text field. */
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        if(keyCode == KeyEvent.VK_TAB)
            {
            shiftAndControlWasDown = true;
            IJ.setTool(0);
            exitAstronomyTool();
            }
        if (e.isShiftDown() && keyCode == KeyEvent.VK_UP)
            zoomIn(startDragScreenX, startDragScreenY, true, true, 8.0);
        else if (e.isShiftDown() && keyCode == KeyEvent.VK_DOWN)
            {
            if ((e.getModifiers()& e.ALT_MASK) != 0 )
                fillNotFit =true;
            fitImageToCanvas();
            }
        else if(keyCode == '+' || keyCode == '=' || keyCode == KeyEvent.VK_UP)
            zoomIn(startDragScreenX, startDragScreenY, true, true, 0.0);

        else if(keyCode == '-' || keyCode == '_' || keyCode == KeyEvent.VK_DOWN)
            zoomOut(startDragScreenX, startDragScreenY, true, true);

        }

    /** Handle the key-released event from the text field. */
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if(keyCode == KeyEvent.VK_TAB)
            {
            shiftAndControlWasDown = false;
            IJ.setTool(astronomyToolId);
            reenterAstronomyTool();
            }
    }




        public void mouseClicked(MouseEvent e) {

                // mouse clicked code is in mouseReleased() to allow drag/click thresholding
                IJ.setInputEvent(e);
                }

        public void mousePressed(MouseEvent e) {
                startDragScreenX = e.getX();
                startDragScreenY = e.getY();
                ac.setMouseScreenPosition(startDragScreenX, startDragScreenY);
                startDragX = ac.offScreenX(startDragScreenX);
                startDragY = ac.offScreenY(startDragScreenY);
                lastScreenX = startDragScreenX;  //update in mouseDragged during drag
                lastScreenY = startDragScreenY;  //update in mouseDragged during drag
                startDragSubImageX =ac.getSrcRect().x;
                startDragSubImageY =ac.getSrcRect().y;
                button2Drag = false;
                IJ.setInputEvent(e);
                newClick = true;
                if((e.getModifiers() & MouseEvent.BUTTON2_MASK) != 0)
                    {
                    if (e.getClickCount() == 1)
                        {
                        imp.killRoi();
                        }
                    else
                        {
                        IJ.runPlugIn("Clear_Overlay", "");
                        }
                    }

                if (goodWCS)
                    {
                    xy[0]= startDragX;
                    xy[1]= startDragY;
                    startRadec = wcs.pixels2wcs (xy);
                    }
                }

        
        public void mouseReleased(MouseEvent e) {
                int screenX = e.getX();
                int screenY = e.getY();
                ac.setMouseScreenPosition(screenX, screenY);
                double imageX = ac.offScreenXD(screenX);
                double imageY = ac.offScreenYD(screenY);
                ImageProcessor ip = imp.getProcessor();
                IJ.setInputEvent(e);
                if ((((e.getModifiers() & MouseEvent.BUTTON2_MASK) != 0) && button2Drag) ||
                        (((e.getModifiers() & MouseEvent.BUTTON3_MASK) != 0) && button2Drag && e.isControlDown()))
                        {                                // measure distance and report in Results Table
                        button2Drag = false;
                        double value = ip.getPixelValue((int)imageX, (int)imageY);
                        String lab;
                        if (goodWCS)
                                {
                                xy[0]= imageX;
                                xy[1]= imageY;
                                radec = wcs.pixels2wcs (xy);
                                updateWCS(DRAGGING);
                                }
                        updateXYValue(imageX, imageY, value, DRAGGING);
                        if (writeMiddleDragValues)
                                updateResultsTable(imageX, imageY, value, DRAGGING);
                        }

                if (Math.abs(screenX-startDragScreenX) + Math.abs(screenY-startDragScreenY) < 4.0)    //check mouse click/drag threshold
                        {
                        if (e.getClickCount() > 1 && e.getButton() == MouseEvent.BUTTON3 && !e.isControlDown() && currentToolId != zoomToolId)
                            {
                            if (IJ.altKeyDown() || e.getClickCount() == 2)
                                fillNotFit = true;
                            fitImageToCanvas();
                            }
                        else if (e.getClickCount() > 1 && e.getButton() == MouseEvent.BUTTON1 && !e.isControlDown() && currentToolId != zoomToolId)
                            {
                            zoomIn(startDragScreenX, startDragScreenY, true, false, 8.0);
                            }
                        else
                            {
                            if (e.getButton() == MouseEvent.BUTTON1)                          //left mouse click
                                    {
                                    if (e.isControlDown() || currentToolId == zoomToolId)
                                        zoomControl(e.getX(), e.getY(), -1, MOUSECLICK);
                                    else if(e.isShiftDown() || currentToolId == apertureToolId)
                                        {
                                        imp.setRoi(new OvalRoi((int)(imageX-rBack1), (int)(imageY-rBack1), rBack1*2, rBack1*2));
                                        imp.setRoi(new OvalRoi((int)(imageX-radius), (int)(imageY-radius), radius*2, radius*2));
                                        IJ.runPlugIn(imp, "Aperture_", "");
                                        }
                                    }

                            else if (e.getButton() == MouseEvent.BUTTON2)                     //middle mouse click
                                    {
                                    double value = ip.getPixelValue((int)imageX, (int)imageY);
                                    String lab;
                                    if (goodWCS)
                                            {
                                            xy[0]= imageX;
                                            xy[1]= imageY;
                                            radec = wcs.pixels2wcs (xy);
                                            updateWCS(NOT_DRAGGING);
                                            }
                                    updateXYValue(imageX, imageY, value, NOT_DRAGGING);
                                    if (writeMiddleClickValues)
                                            updateResultsTable(imageX, imageY, value, NOT_DRAGGING);
                                    }

                            else if (e.getButton() == MouseEvent.BUTTON3)    //right mouse click
                                    {
                                    if (e.isControlDown()  || currentToolId == zoomToolId)
                                            zoomControl(screenX, screenY, 1, MOUSECLICK);
                                    }
                            }
                        }
                ac.repaint();
                startDragScreenX = screenX;
                startDragScreenY = screenY;
                }

        public void mouseEntered(MouseEvent e)
            {
            apertureOverlay.clear();
            ac.repaint();
            }

        public void mouseExited(MouseEvent e) 
            {
            apertureOverlay.clear();
            ac.repaint();
            }

        public void mouseMoved(MouseEvent e) {

                ac.setMouseScreenPosition(e.getX(),e.getY());
                double imageX = ac.offScreenXD(e.getX());
                double imageY = ac.offScreenYD(e.getY());
                ImageProcessor ip = imp.getProcessor();
                lastImageX = (int)imageX;
                lastImageY = (int)imageY;
                double value = ip.getPixelValue((int)imageX, (int)imageY);
                String lab;
                xy[0]= imageX;
                xy[1]= imageY;
                photom.measure (ip,imageX,imageY);
                IJ.setInputEvent(e);
                if (showPhotometer != e.isShiftDown())
                    {
                    if (lastImageX > imageEdgeX1 && lastImageX < imageEdgeX2 &&     //needed when image is zoomed out
                        lastImageY > imageEdgeY1 && lastImageY < imageEdgeY2)       //to be smaller than canvas
                        {
                        updatePhotometerOverlay();
                        }
                    else
                        {
                        apertureOverlay.clear();
                        ac.repaint();
                        }
                    }
                else if (prevShiftDownState != e.isShiftDown())
                    {
                    apertureOverlay.clear();
                    ac.repaint();
                    }

                if (goodWCS)
                        {
                        radec = wcs.pixels2wcs (xy);
                        lab = "   "+hms(radec[0]/15.0, 3)+", ";
                        if (radec[1] > 0.0)
                                lab += "+"+hms(radec[1], 2);
                        else
                                lab += hms(radec[1], 2);
                        updateWCS(NOT_DRAGGING);

                        }
                else
                        {
                        lab = "";
//                        peakLabel.setText("Peak:");
//                        peakTextField.setText(fourPlaces.format(photom.peakBrightness()));
                        }
                updateXYValue(imageX, imageY, value, NOT_DRAGGING);
                prevShiftDownState = e.isShiftDown();
                }

        public void mouseDragged(MouseEvent e) {
                int screenX = e.getX();
                int screenY = e.getY();
                ac.setMouseScreenPosition(screenX, screenY);
                int imageX = ac.offScreenX(screenX);
                int imageY = ac.offScreenY(screenY);
                ImageProcessor ip = imp.getProcessor();
                double value = ip.getPixelValue((int)imageX, (int)imageY);
                magnification = ac.getMagnification();
                photom.measure (ip,imageX,imageY);
                IJ.setInputEvent(e);
                if (Math.abs(screenX-startDragScreenX) + Math.abs(screenY-startDragScreenY) >= 4.0)  //check mouse click/drag threshold
                        {
                        apertureOverlay.clear();
                        ac.repaint();
                        if ((e.getModifiers() & MouseEvent.BUTTON3_MASK) != 0)    // control-dragging with right mouse button
                                {                                                 // measure distance, update data display
                                if (e.isControlDown())
                                        {
//                                        button2Drag = true;
//                                        imp.setRoi(new Line(startDragX, startDragY, imageX, imageY));
//                                        if (goodWCS)
//                                                {
//                                                xy[0]= imageX;
//                                                xy[1]= imageY;
//                                                radec = wcs.pixels2wcs (xy);
//                                                updateWCS(DRAGGING);
//                                                }
//                                        else
//                                                {
//                                                arcLengthLabel.setText("Peak:");
//                                                arcLengthTextField.setText(fourPlaces.format(photom.peakBrightness()));
//                                                }
//                                        updateXYValue(imageX, imageY, value, DRAGGING);
//                                        String lab = ", RA: "+fourPlaces.format(radec[0])+", DEC: "+fourPlaces.format(radec[1]);
                                        }
                                else                                                //no modifier - change screen min and max display values
                                        {
                                        adjustMinAndMax(lastScreenX - screenX, lastScreenY - screenY);

//                                        THE COMMENTED CODE BELOW DIRECTLY CONTROLS MAX AND MIN DISPLAYED VALUES
//                                        scaleMin = 100.0;
//                                        scaleMax = 100.0;
//                                        if ((min < 1.5) && (imp.getType() == ImagePlus.GRAY32)) scaleMin = 0.01;
//                                        else if ((min < 10.0)  && (imp.getType() == ImagePlus.GRAY32)) scaleMin = 0.1;
//                                        else if (min < 100.0) scaleMin = 1.0;
//                                        else if (min < 1000.0) scaleMin = 10.0;
//                                        else if (min < 10000.0) scaleMin = 50.0;
//                                        if ((max < 1.5) && (imp.getType() == ImagePlus.GRAY32)) scaleMax = 0.01;
//                                        else if ((max < 10.0)  && (imp.getType() == ImagePlus.GRAY32)) scaleMax = 0.1;
//                                        else if (max < 100.0) scaleMax = 1.0;
//                                        else if (max < 1000.0) scaleMax = 10.0;
//                                        else if (max < 10000.0) scaleMax = 50.0;
//                                        min -= scaleMin*(double)(lastScreenX - screenX);
//                                        max += scaleMax*(double)(lastScreenY - screenY);
//                                        if ((min + scaleMin) > max)
//                                                max = min + scaleMax;
//                                        if (min < minValue )
//                                                min = minValue;
//                                        if (max < (minValue + scaleMax))
//                                                max = minValue + scaleMax;
//                                        if (min > (maxValue - scaleMin))
//                                                min = maxValue - scaleMin;
//                                        if (max > maxValue)
//                                                max = maxValue;
//                                        ip.setMinAndMax(min, max);


                                        lastScreenX = screenX;
                                        lastScreenY = screenY;
                                        if (goodWCS)
                                                {
                                                xy[0]= imageX;
                                                xy[1]= imageY;
                                                radec = wcs.pixels2wcs (xy);
                                                updateWCS(NOT_DRAGGING);
                                                }
                                        else
                                                {
//                                                peakLabel.setText("Peak:");
//                                                peakTextField.setText(fourPlaces.format(photom.peakBrightness()));
                                                }  
                                        updateXYValue(imageX, imageY, value, NOT_DRAGGING);
                                        }
                                }
                        else if ((e.getModifiers() & MouseEvent.BUTTON2_MASK) != 0)        // dragging with middle mouse button
                                {                                                          // measure distance, show on plot
                                button2Drag = true;                                        // and save to results window when mouse released
                                String lab;
                                imp.setRoi(new Line(startDragX, startDragY, imageX, imageY));
                                if (goodWCS)
                                        {
                                        xy[0]= imageX;
                                        xy[1]= imageY;
                                        radec = wcs.pixels2wcs (xy);
                                        updateWCS(DRAGGING);
                                        }
                                else
                                        {
//                                        peakLabel.setText("Peak:");
//                                        peakTextField.setText(fourPlaces.format(photom.peakBrightness()));
                                        }
                                updateXYValue(imageX, imageY, value, DRAGGING);
                                }
                        else if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0 && e.isShiftDown() && !e.isControlDown())
                            {
                            imp.setRoi(new OvalRoi(imageX>startDragX?startDragX:imageX, imageY>startDragY?startDragY:imageY,
                                    imageX>startDragX?imageX-startDragX:startDragX-imageX,
                                    imageY>startDragY?imageY-startDragY:startDragY-imageY));
                            }
                        else if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0 && e.isControlDown() && !e.isShiftDown())
                            {
                            imp.setRoi(new Rectangle(imageX>startDragX?startDragX:imageX, imageY>startDragY?startDragY:imageY,
                                    imageX>startDragX?imageX-startDragX:startDragX-imageX,
                                    imageY>startDragY?imageY-startDragY:startDragY-imageY));
                            }
                        else if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0 && !(e.isControlDown() || e.isShiftDown() || e.isAltDown()))  
                                {                                                 // dragging with left mouse button (pan image)
                                Rectangle imageRect = ac.getSrcRect();
                                String lab;
                                if (astronomyMode)
                                        {
//                                        int w = (int)Math.round(icWidth/magnification);
//                                        if (w*magnification<icWidth) w++;
//                                        int h = (int)Math.round(icHeight/magnification);
//                                        if (h*magnification<icHeight) h++;
                                        int height = imp.getHeight();
                                        int width = imp.getWidth();
                                        int ox = netFlipX ? startDragSubImageX + imageRect.width - (int)(e.getX()/magnification) :
                                                 startDragSubImageX + (int)(e.getX()/magnification);
                                        int oy = netFlipY ? startDragSubImageY + imageRect.height - (int)(e.getY()/magnification) :
                                                 startDragSubImageY + (int)(e.getY()/magnification);
                                        imageRect.x =  startDragSubImageX + (startDragX-ox);
                                        imageRect.y = startDragSubImageY + (startDragY-oy);
                                        savedPanX = imageRect.x;
                                        savedPanY = imageRect.y;
                                        savedPanHeight = imageRect.height;
                                        savedPanWidth = imageRect.width;

//                                        if (imageRect.x<0) imageRect.x = 0;
//                                        if (imageRect.y<0) imageRect.y = 0;
//                                        if (imageRect.x+w>icWidth) imageRect.x = ipWidth-w;
//                                        if (imageRect.y+h>icHeight) imageRect.y = ipHeight-h;

                                        ac.setSourceRect(imageRect);
                                        savedIpWidth = ip.getWidth();
                                        savedIpHeight = ip.getHeight();
                                        Prefs.set("Astronomy_Tool.savedIpWidth", savedIpWidth);
                                        Prefs.set("Astronomy_Tool.savedIpHeight", savedIpHeight);
                                        Prefs.set("Astronomy_Tool.savedPanX", savedPanX);
                                        Prefs.set("Astronomy_Tool.savedPanY", savedPanY);
                                        Prefs.set("Astronomy_Tool.savedPanHeight", savedPanHeight);
                                        Prefs.set("Astronomy_Tool.savedPanWidth", savedPanWidth);
                                        setImageEdges();
                                        if (imageRect.x<0 || imageRect.y<0 || imageRect.x+imageRect.width>width || imageRect.y+imageRect.height>height)
                                            {
                                            clearAndPaint();
                                            }
                                        else
                                            ac.repaint();
                                        }
                                xy[0]= imageX;
                                xy[1]= imageY;
                                if (goodWCS)
                                        {
                                        radec = wcs.pixels2wcs (xy);
                                        lab = ", RA: "+fourPlaces.format(radec[0])+", DEC: "+fourPlaces.format(radec[1]);
                                        updateWCS(NOT_DRAGGING);
                                        }
                                else
                                        {
                                        lab = "";
//                                        peakLabel.setText("Peak:");
//                                        peakTextField.setText(fourPlaces.format(photom.peakBrightness()));
                                        }
                                updateXYValue(imageX, imageY, value, NOT_DRAGGING);
                                }
                        }

                }


    void updateXYValue(double imageX, double imageY, double value, boolean dragging)
            {
            valueTextField.setText(fourPlaces.format(value));
            ijXTextField.setText(fourPlaces.format(imageX));
            ijYTextField.setText(fourPlaces.format(imageY));
            fitsXTextField.setText(fourPlaces.format(imageX - Centroid.PIXELCENTER + 1.0));
            fitsYTextField.setText(fourPlaces.format((double)imp.getHeight() - imageY + Centroid.PIXELCENTER));
            peakLabel.setText("Peak:");
            peakTextField.setText(fourPlaces.format(photom.peakBrightness()));
            if (dragging && !goodWCS)
                {
                if (pixelScale <= 0.0)
                    {
                    lengthLabel.setText("Length:");
                    lengthTextField.setText(fourPlaces.format(Math.sqrt((imageX-startDragX)*(imageX-startDragX)
                                                                      + (imageY-startDragY)*(imageY-startDragY))));
                    }
                else
                    {
                    lengthLabel.setText("Arclen:");
                    if (useSexagesimal)
                        lengthTextField.setText(hms(pixelScale*Math.sqrt((imageX-startDragX)*(imageX-startDragX)
                                                                      + (imageY-startDragY)*(imageY-startDragY))/3600,2));
                    else
                        lengthTextField.setText(fourPlaces.format(pixelScale*Math.sqrt((imageX-startDragX)*(imageX-startDragX)
                                                                      + (imageY-startDragY)*(imageY-startDragY))/3600));
                    }
                }
            else if (!goodWCS)
                {
                lengthLabel.setText("Int Cnts:");
                lengthTextField.setText(fourPlaces.format(photom.sourceBrightness()));
                }
            }

    void updateWCS(boolean dragging)
            {
//            peakLabel.setText("Peak:");
//            peakTextField.setText(fourPlaces.format(photom.peakBrightness()));
            if (useSexagesimal)
                {
                RATextField.setText(hms(radec[0]/15.0, 3));
                if (radec[1] > 0.0)
                        DecTextField.setText("+"+hms(radec[1], 2));
                else
                        DecTextField.setText(hms(radec[1], 2));
                if (dragging)
                        {
                        lengthLabel.setText("Arclen:");
                        lengthTextField.setText(hms(arcLength(),2));
                        }
                else
                        {
                        lengthLabel.setText("Int Cnts:");
                        lengthTextField.setText(fourPlaces.format(photom.sourceBrightness()));
                        }
                }
            else
                {
                RATextField.setText(sixPlaces.format(radec[0]));
                DecTextField.setText(sixPlaces.format(radec[1]));
                if (dragging)
                        {
                        lengthLabel.setText("Arclen:");
                        lengthTextField.setText(fourPlaces.format(arcLength()));
                        }
                else
                        {
                        lengthLabel.setText("Int Cnts:");
                        lengthTextField.setText(fourPlaces.format(photom.sourceBrightness()));
                        }
                }

            }

    double arcLength()
            {
            double RADDEG = Math.PI/180.;
            return Math.acos(Math.cos((90.-startRadec[1])*RADDEG)*Math.cos((90. - radec[1])*RADDEG) +
                    Math.sin((90.-startRadec[1])*RADDEG)*Math.sin((90.-radec[1])*RADDEG)*
                    Math.cos((startRadec[0]-radec[0])*RADDEG))/RADDEG;
            }

    void updateResultsTable(double imageX, double imageY, double value, boolean dragging)
            {
            String lab = "";
            lab += "Xij: "+threePlaces.format(imageX);
            lab += ",  Yij: "+threePlaces.format(imageY);
            lab += ",  Xfits: "+threePlaces.format(imageX - Centroid.PIXELCENTER + 1.0);
            lab += ",  Yfits: "+threePlaces.format((double)imp.getHeight() - imageY + Centroid.PIXELCENTER);
            lab += ",  Value: "+uptoSixPlaces.format(value);
            if (goodWCS)
                {
                if (useSexagesimal)
                    {
                    lab += ",     RA: "+hms(radec[0]/15.0, 3);
                    if (radec[1] >= 0.0)
                            lab += ",  DEC: +"+hms(radec[1], 2);
                    else
                            lab += ",  DEC: "+hms(radec[1], 2);
                    if (dragging)
                            lab += ",  Arclength: "+(hms(arcLength(),2));
                    }
                else
                    {
                    lab += ",     RA: "+fourPlaces.format(radec[0]);
                    lab += ",  DEC: "+fourPlaces.format(radec[1]);
                    if (dragging)
                            lab += ",  Arclength: "+(fourPlaces.format(arcLength()));
                    }
                }
            else if (dragging)
                {
                if (pixelScale <= 0.0)
                    {
                    lab += ",  Length: "+(fourPlaces.format((Math.sqrt((imageX-startDragX)*(imageX-startDragX)
                                                                      + (imageY-startDragY)*(imageY-startDragY))))+" pixels");
                    }
                else if (useSexagesimal)
                    {
                    lab += ",  Arclength: "+(hms(pixelScale*Math.sqrt((imageX-startDragX)*(imageX-startDragX)
                                                                      + (imageY-startDragY)*(imageY-startDragY))/3600,2));
                    }
                else
                    {
                     lab += ",  Arclength: "+(fourPlaces.format(pixelScale*Math.sqrt((imageX-startDragX)*(imageX-startDragX)
                                                                      + (imageY-startDragY)*(imageY-startDragY))/3600));
                    }
                }
            IJ.log(lab);
            }

        public void mouseWheelMoved( MouseWheelEvent e ) {
                        {
                        int screenX = e.getX();
                        int screenY = e.getY();
                        ac.setMouseScreenPosition(screenX, screenY);
                        double imageX = ac.offScreenXD(screenX);
                        double imageY = ac.offScreenYD(screenY);
                        ImageProcessor ip = imp.getProcessor();
                        double value = ip.getPixelValue((int)imageX, (int)imageY);
                        int magChangeSteps = e.getWheelRotation();
                        IJ.setInputEvent(e);
                        if (e.isControlDown() && ((e.getModifiers() & MouseEvent.BUTTON3_MASK) == 0))
                            {
                            adjustMinAndMax(magChangeSteps, 0);
                            }
                        else if (e.isShiftDown() && ((e.getModifiers() & MouseEvent.BUTTON3_MASK) == 0))
                            {
                            adjustMinAndMax(0, -magChangeSteps);
                            }
                        else
                            {
                            zoomControl(screenX, screenY, magChangeSteps, WHEEL);
                            }
                        }
                }

        void adjustMinAndMax(int xSteps, int ySteps)
                {
                double low, high;
//                min = ip.getMin();
                min = imp.getDisplayRangeMin();
//                max = ip.getMax();
                max = imp.getDisplayRangeMax();
                brightness = (max + min)/2.0;
                contrast = (max - min)/2.0;

                if (imp.getType() == ImagePlus.COLOR_RGB || imp.getType() == ImagePlus.COLOR_256 ||
                        imp.getType() == ImagePlus.GRAY8) brightstepsize = 1.0;
                else if((contrast < 2.0) && (imp.getType() == ImagePlus.GRAY32) &&
                        (maxValue - minValue >= 10.0)) brightstepsize = 0.01;
                else if((contrast < 2.0) && (imp.getType() == ImagePlus.GRAY32) &&
                        (maxValue - minValue < 10.0)) brightstepsize = 0.001;
                else if ((contrast < 10.0)  && (imp.getType() == ImagePlus.GRAY32)) brightstepsize = 0.2;
                else if (contrast < 500.0) brightstepsize = 2.0;
                else if (contrast < 10000.0) brightstepsize = 20.0;
                else brightstepsize = 200.0;


                contrast -= brightstepsize*(double)(xSteps);
                if (brightstepsize > 0.2 && contrast < 10.0 && imp.getType() == ImagePlus.GRAY32)
                    {
                    contrast = 9.8;
                    brightstepsize = 0.2;
                    ySteps = -1;
                    }
                else if(brightstepsize > 0.01 && contrast < 2.0 && imp.getType() == ImagePlus.GRAY32)
                    {
                    contrast = 1.99;
                    brightstepsize = 0.001;
                    ySteps = -1;
                    }
                brightness += brightstepsize*(double)(ySteps);

                if (contrast < brightstepsize)
                        contrast = brightstepsize;
                else if ((contrast > (maxValue - minValue)/ 2.0))
                        contrast = (maxValue - minValue)/2.0;

                low = (brightness - contrast < minValue) ? minValue : brightness - contrast;
                high = (brightness + contrast < minValue + brightstepsize) ? minValue + brightstepsize : brightness + contrast;
                if (high > maxValue) high = maxValue;
//                ip.setMinAndMax(low, high);
                imp.setDisplayRange(low, high);
                minMaxChanged = true;
//                minSlider.setValue((int)((low + sliderShift)*(double)sliderMultiplier));
//                maxSlider.setValue((int)((high + sliderShift)*(double)sliderMultiplier));
                minMaxBiSlider.setColoredValues(low, high);
//                imp.updateAndDraw();
                }


        public void zoomControl(int screenX, int screenY, int magChangeSteps, boolean mouseClick)
                {
                ac.setMouseScreenPosition(screenX, screenY);
                double imageX = ac.offScreenXD(screenX);
                double imageY = ac.offScreenYD(screenY);
                double zoom = ac.getMagnification();
                double nextzoom = ac.getLowerZoomLevel(zoom);

                ipWidth = imp.getWidth();
                ipHeight = imp.getHeight();
                icWidth = ac.getWidth();
                icHeight = ac.getHeight();
                if (magChangeSteps > 0) //&& (zoom > initialHighMag))     //zoom out
                        {
                        zoomOut((int)(screenX), (int)(screenY), mouseClick, false);
                        }

                else if (magChangeSteps<0)    //zoom in
                        {
                        zoomIn((int)(screenX), (int)(screenY), mouseClick, false, 0.0);
                        }
//                try
//                        {
//                        Robot robot = new Robot();     //move mouse pointer to new position on screen
//                        robot.mouseMove(ac.getLocationOnScreen().x +ac.screenXD(Math.round(imageX+0.499)),
//                                        ac.getLocationOnScreen().y + ac.screenYD(Math.round(imageY+0.499)));
//                        }
//                catch (AWTException ee)
//                        {
//                        ee.printStackTrace();
//                        }
                }

	public void zoomIn(int screenX, int screenY, boolean mouseClick, boolean center, double factor) {
        int height = newCanvasHeight();
        int width = newCanvasWidth();
        int newHeight = height;
        int newWidth = width;
        magnification = ac.getMagnification();
        if (magnification >= 32) return;
        double newMag = magnification;
        if (mouseClick)
            {
            if (factor > 0.0)
                newMag = magnification*factor;
            else
                newMag = ac.getHigherZoomLevel(magnification);
            }
        else
            newMag = magnification*1.1;
        if (newMag >= 32) newMag = 32;
        adjustSourceRect(newMag, screenX, screenY, center);
	}

	public void zoomOut(int screenX, int screenY, boolean mouseClick, boolean center) {
        magnification = ac.getMagnification();
        if (magnification <= 0.03125) return;
        double newMag = magnification;
        if (mouseClick)
            newMag = ac.getLowerZoomLevel(magnification);
        else
            newMag = magnification/1.1;
        if (newMag <= 0.03125) newMag = 0.03125;
        adjustSourceRect(newMag, screenX, screenY, center);
        }


	void adjustSourceRect(double newMag, int screenX, int screenY, boolean center) {
        ac.setMouseScreenPosition(screenX, screenY);
        icWidth = ac.getWidth();
        icHeight = ac.getHeight();
		double w = (double)icWidth/newMag;
		if (w*newMag<icWidth) w++;
		double h = (double)icHeight/newMag;
		if (h*newMag<icHeight) h++;
        
        double xSign = netFlipX ? 1.0 : -1.0;
        double ySign = netFlipY ? 1.0 : -1.0;


		double offx = ac.offScreenXD(screenX);
		double offy = ac.offScreenYD(screenY);

        Rectangle r = ac.getSrcRect();

        if (center && newClick)
            {
            r.x = (int)(offx - w/2.0);
            r.y = (int)(offy - h/2.0);
            r.width = (int)w;
            r.height = (int)h;
            newClick = false;
            }
        else if (center && !newClick)
            {
            r.x = (int)(r.x + r.width/2.0 - w/2.0);
            r.y = (int)(r.y + r.height/2.0 - h/2.0);
            r.width = (int)w;
            r.height = (int)h;            
            }
        else
            {
            if (offx < 0) {offx = 0; screenX = ac.screenXD(offx);}
            if (offx > imp.getWidth()) {offx = imp.getWidth()-1; screenX = ac.screenXD(offx+2);}
            if (offy < 0) {offy = 0; screenY = ac.screenYD(offy);}
            if (offy > imp.getHeight()) {offy = imp.getHeight()-1; screenY = ac.screenYD(offy+2);}
            double offsetX = netFlipX ? 1.93 : 0.499;
            double offsetY = netFlipY ? 1.93 : 0.499;
            r = new Rectangle((int)(offsetX + offx-w/2.0*(1.0 + xSign*((double)icWidth/2.0 - (double)screenX)/((double)icWidth/2.0))),
                                        (int)(offsetY + offy-h/2.0*(1.0 + ySign*((double)icHeight/2.0 - (double)screenY)/((double)icHeight/2.0))),
                                        (int) w, (int) h);
            }

//        if (ac.getNetRotate())
//                  r = new Rectangle((int)(offsetY + offy-h/2.0*(1.0 + xSign*((double)icHeight/2.0 - (double)screenY)/((double)icHeight/2.0))),
//                                    (int)(offsetX + offx-w/2.0*(1.0 + ySign*((double)icWidth/2.0 - (double)screenX)/((double)icWidth/2.0))),
//                                    (int) h, (int) w);


        ac.setMagnification(newMag);
        ac.setSourceRect(r);


        savedPanX = r.x;
        savedPanY = r.y;
        savedPanHeight = r.height;
        savedPanWidth = r.width;
        savedMag = ac.getMagnification();
        savedIpWidth = imp.getWidth();
        savedIpHeight = imp.getHeight();
        Prefs.set("Astronomy_Tool.savedPanX", savedPanX);
        Prefs.set("Astronomy_Tool.savedPanY", savedPanY);
        Prefs.set("Astronomy_Tool.savedPanHeight", savedPanHeight);
        Prefs.set("Astronomy_Tool.savedPanWidth", savedPanWidth);
        Prefs.set("Astronomy_Tool.savedIpWidth", savedIpWidth);
        Prefs.set("Astronomy_Tool.savedIpHeight", savedIpHeight);
        setImageEdges();
        if (r.x<=0 || r.y<=0 || r.x+r.width>=ipWidth || r.y+h>=ipHeight)
            {
            clearAndPaint();
            }
        else
            ac.repaint();

        prevMag = ac.getMagnification();
    	}


    void setImageEdges()
        {
        imageEdgeX1 = ac.screenX(0) > 0 ? (int)photom.rBack2 : ac.getSrcRect().x;
        imageEdgeY1 = ac.screenY(0) > 0 ? (int)photom.rBack2 : ac.getSrcRect().y;
        imageEdgeX2 = ac.screenX(imp.getWidth()) < ac.getWidth() ? imp.getWidth() - (int)photom.rBack2 : ac.getSrcRect().x + ac.getSrcRect().width;
        imageEdgeY2 = ac.screenY(imp.getHeight()) < ac.getHeight() ? imp.getHeight() - (int)photom.rBack2 : ac.getSrcRect().y + ac.getSrcRect().height;
        }

//    public void fitToAstroWindow() {
//        ipWidth = imp.getWidth();
//        ipHeight = imp.getHeight();
//        icWidth = ac.getWidth();
//        icHeight = ac.getHeight();
//        Rectangle srcRect = new Rectangle(ac.getSrcRect());
//		ImageWindow win = imp.getWindow();
//		if (win==null) return;
//		Rectangle bounds = win.getBounds();
//		Insets insets = win.getInsets();
//		int sliderHeight = (win instanceof StackWindow)?20:0;
//		double xmag = (double)(bounds.width-10)/srcRect.width;
//		double ymag = (double)(bounds.height-(10+insets.top+insets.bottom+otherPanelsHeight+sliderHeight))/srcRect.height;
//		ac.setMagnification(Math.min(xmag, ymag));
//		int width=(int)(ipWidth*magnification);
//		int height=(int)(ipHeight*magnification);
//		if (width==bounds.width-10&&height==bounds.height-(10+insets.top+insets.bottom+otherPanelsHeight+sliderHeight)) return;
//		srcRect=new Rectangle(0,0,ipWidth, ipHeight);
//        ac.setSourceRect(srcRect);
//		ac.setDrawingSize(width, height);
//        savedPanX = 0;
//        savedPanY = 0;
//        savedPanHeight = ipHeight;
//        savedPanWidth = ipWidth;
//        savedMag = ac.getMagnification();
//        savedICWidth = width;
//        savedICHeight = height;
//        Prefs.set("Astronomy_Tool.savedPanX", savedPanX);
//        Prefs.set("Astronomy_Tool.savedPanY", savedPanY);
//        Prefs.set("Astronomy_Tool.savedPanHeight", savedPanHeight);
//        Prefs.set("Astronomy_Tool.savedPanWidth", savedPanWidth);
//        Prefs.set("Astronomy_Tool.savedMag", savedMag);
//        Prefs.set("Astronomy_Tool.savedICWidth", savedICWidth);
//        Prefs.set("Astronomy_Tool.savedICHeight", savedICHeight);
//        buildAstroWindow();
////        layoutContainer(imageWindow);
////        ac.repaint();
////		openFrame.doLayout();
////        openFrame.pack();
//	}

	public String hms (double d, int fractionPlaces)
		{
        NumberFormat nf = null;
        NumberFormat nf23 = null;
        NumberFormat nf22 = null;
        nf = NumberFormat.getInstance();
		nf.setMinimumIntegerDigits(2);
        nf23 = NumberFormat.getInstance();
        nf23.setMinimumIntegerDigits(2);
        nf23.setMinimumFractionDigits(3);
        nf23.setMaximumFractionDigits(3);
        nf22 = NumberFormat.getInstance();
        nf22.setMinimumIntegerDigits(2);
        nf22.setMinimumFractionDigits(2);
        nf22.setMaximumFractionDigits(2);
		double dd = Math.abs(d);
		int h = (int)dd;
		int m = (int)(60.0*(dd-(double)h));
		double s = 3600.0*(dd-(double)h-(double)m/60.0);

		String str = "";
		if (d < 0.0) str = "-";
		str += ""+nf.format(h)+":"+nf.format(m)+":";
        if (fractionPlaces == 2)
            str += nf22.format(s);
        else
            str += nf23.format(s);
		return str;
		}

	void getPrefs()
		{
        savedIpHeight=(int)Prefs.get("Astronomy_Tool.savedIpHeight", savedIpHeight);
        savedIpWidth=(int)Prefs.get("Astronomy_Tool.savedIpWidth", savedIpWidth);
        useSexagesimal=Prefs.get("Astronomy_Tool.useSexagesimal", useSexagesimal);
        startupAutoLevel=Prefs.get("Astronomy_Tool.startupAutoLevel", startupAutoLevel);
        startupPrevSize=Prefs.get("Astronomy_Tool.startupPrevSize", startupPrevSize);
        startupPrevPan=Prefs.get("Astronomy_Tool.startupPrevPan", startupPrevPan);
        startupPrevZoom=Prefs.get("Astronomy_Tool.startupPrevZoom", startupPrevZoom);
        startupPrevLevels=Prefs.get("Astronomy_Tool.startupPrevLevels", startupPrevLevels);
        writeMiddleClickValues=Prefs.get("Astronomy_Tool.writeMiddleClickValues", writeMiddleClickValues);
        writeMiddleDragValues=Prefs.get("Astronomy_Tool.writeMiddleDragValues", writeMiddleDragValues);
        autoConvert=Prefs.get("Astronomy_Tool.autoConvert", autoConvert);
        savedMag=Prefs.get("Astronomy_Tool.savedMag", savedMag);
        savedICWidth=(int)Prefs.get("Astronomy_Tool.savedICWidth", savedICWidth);
        savedICHeight=(int)Prefs.get("Astronomy_Tool.savedICHeight", savedICHeight);
        savedPanX =(int)Prefs.get("Astronomy_Tool.savedPanX", savedPanX);
        savedPanY=(int)Prefs.get("Astronomy_Tool.savedPanY", savedPanY);
        savedPanHeight=(int)Prefs.get("Astronomy_Tool.savedPanHeight", savedPanHeight);
        savedPanWidth=(int)Prefs.get("Astronomy_Tool.savedPanWidth", savedPanWidth);
        savedMin=Prefs.get("Astronomy_Tool.savedMin", savedMin);
        savedMax=Prefs.get("Astronomy_Tool.savedMax", savedMax);
        frameLocationX=(int)Prefs.get("Astronomy_Tool.frameLocationX",frameLocationX);
        frameLocationY=(int)Prefs.get("Astronomy_Tool.frameLocationY",frameLocationY);
        rememberWindowLocation=Prefs.get("Astronomy_Tool.rememberWindowLocation", rememberWindowLocation);
        radius = (int)Prefs.get("aperture.radius", radius);
		rBack1 = (int)Prefs.get("aperture.rback1",rBack1);
		rBack2 = (int)Prefs.get("aperture.rback2",rBack2);
        ac.zoomIndicatorSize = (int)Prefs.get("Astronomy_Tool.zoomIndicatorSize",ac.zoomIndicatorSize);
        pixelScale = Prefs.get("Astronomy_Tool.pixelScale", pixelScale);
        ac.dirAngle = Prefs.get("Astronomy_Tool.dirAngle", ac.dirAngle);
        showPhotometer = Prefs.get("Astronomy_Tool.showPhotometer", showPhotometer);
        removeBackStars = Prefs.get("Astronomy_Tool.removeBackStars", removeBackStars);
        useInvertingLut = Prefs.get("Astronomy_Tool.useInvertingLut", useInvertingLut);
        invertX = Prefs.get("Astronomy_Tool.invertX", invertX);
        invertY = Prefs.get("Astronomy_Tool.invertY", invertY);
        rotation = (int)Prefs.get("Astronomy_Tool.rotation", rotation);
        showZoom = Prefs.get("Astronomy_Tool.showZoom", showZoom);
        showDir = Prefs.get("Astronomy_Tool.showDir", showDir);
        showXY = Prefs.get("Astronomy_Tool.showXY", showXY);
        useFixedMinMaxValues = Prefs.get("Astronomy_Tool.useFixedMinMaxValues", useFixedMinMaxValues);
        fixedMinValue = Prefs.get("Astronomy_Tool.fixedMinValue", minValue);
        fixedMaxValue = Prefs.get("Astronomy_Tool.fixedMaxValue", maxValue);
		}

    void savePrefs()
        {
        savedIpWidth = imp.getWidth();
        savedIpHeight = imp.getHeight();

        Prefs.set("Astronomy_Tool.savedIpHeight", savedIpHeight);
        Prefs.set("Astronomy_Tool.savedIpWidth", savedIpWidth);
        Prefs.set("Astronomy_Tool.useSexagesimal", useSexagesimal);
        Prefs.set("Astronomy_Tool.startupAutoLevel", startupAutoLevel);
        Prefs.set("Astronomy_Tool.startupPrevPan", startupPrevPan);
        Prefs.set("Astronomy_Tool.startupPrevZoom", startupPrevZoom);
        Prefs.set("Astronomy_Tool.startupPrevLevels", startupPrevLevels);
        Prefs.set("Astronomy_Tool.writeMiddleClickValues", writeMiddleClickValues);
        Prefs.set("Astronomy_Tool.writeMiddleDragValues", writeMiddleDragValues);
        Prefs.set("Astronomy_Tool.autoConvert", autoConvert);
        Prefs.set("Astronomy_Tool.startupPrevSize", startupPrevSize);
        Prefs.set("Astronomy_Tool.savedMag", savedMag);
        Prefs.set("Astronomy_Tool.pixelScale", pixelScale);
        Prefs.set("Astronomy_Tool.savedICWidth", savedICWidth);
        Prefs.set("Astronomy_Tool.savedICHeight", savedICHeight);
        Prefs.set("Astronomy_Tool.savedPanX", savedPanX);
        Prefs.set("Astronomy_Tool.savedPanY", savedPanY);
        Prefs.set("Astronomy_Tool.savedPanHeight", savedPanHeight);
        Prefs.set("Astronomy_Tool.savedPanWidth", savedPanWidth);
        Prefs.set("Astronomy_Tool.savedMin", savedMin);
        Prefs.set("Astronomy_Tool.savedMax", savedMax);
        Prefs.set("Astronomy_Tool.zoomIndicatorSize",ac.zoomIndicatorSize);
        Prefs.set("Astronomy_Tool.dirAngle", ac.dirAngle);
        frameLocationX = this.getLocation().x;
        frameLocationY = this.getLocation().y;
        Prefs.set("Astronomy_Tool.frameLocationX",frameLocationX);
        Prefs.set("Astronomy_Tool.frameLocationY",frameLocationY);
        Prefs.set("Astronomy_Tool.rememberWindowLocation", rememberWindowLocation);
        Prefs.set("Astronomy_Tool.removeBackStars", removeBackStars);
        Prefs.set("Astronomy_Tool.useInvertingLut", useInvertingLut);
        Prefs.set("Astronomy_Tool.invertX", invertX);
        Prefs.set("Astronomy_Tool.invertY", invertY);
        Prefs.set("Astronomy_Tool.rotation", rotation);
        Prefs.set("Astronomy_Tool.showZoom", showZoom);
        Prefs.set("Astronomy_Tool.showDir", showDir);
        Prefs.set("Astronomy_Tool.showXY", showXY);
        if (imp.getType() != ImagePlus.COLOR_RGB)
            Prefs.set("Astronomy_Tool.useFixedMinMaxValues", useFixedMinMaxValues);
        Prefs.set("Astronomy_Tool.fixedMinValue", fixedMinValue);
        Prefs.set("Astronomy_Tool.fixedMaxValue", fixedMaxValue);
        }

}   // AstroStackWindow class


