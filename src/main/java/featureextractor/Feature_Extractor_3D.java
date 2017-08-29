package featureextractor;

import java.awt.AWTEvent;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Paint;
import java.awt.event.ItemEvent;
import java.awt.font.TextAttribute;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.JFileChooser;

import createmarkerimages.BaseMarkerImageCreator;
import data.Cell3D;
import data.Cell3D_Group;
import data.Coordinates;
import data.Nucleus3D;
import data.Spheroid;
import featureextractor.measurements.CellMeasurer;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.measure.ResultsTable;
import ij.plugin.ChannelSplitter;
import ij.plugin.PlugIn;
import ij.process.ImageConverter;
import migrationmodeanalysis.MigrationModeAnalyser;
import utils.MyMath;
import utils.NucleiDataVisualiser;

/**
 * This plugins use the following plugins:] - MorpholibJ - Measure 3D - Particle analyzer 3D - Region adjacency graph - 3D ImageJ Suite - 3D Intensity measure - 3D Geometric measure - 3D Shape Measure
 *
 * @author Esther
 *
 */
public class Feature_Extractor_3D implements PlugIn
{
	private class Labeled_Coordinate
	{
		Coordinates coordinates;
		int label;
		int grayValue;
		String migrationMode;


		/**
		 *
		 * @param aLabel
		 * @param aXValue
		 * @param aYValue
		 * @param aZValue
		 */
		public Labeled_Coordinate(final int aLabel, final Coordinates aCoordinates, final int aValue, final String aMigrationMode)
		{
			this.coordinates = aCoordinates;
			this.label = aLabel;
			this.grayValue = aValue;
			this.migrationMode = aMigrationMode;
		}


		public Coordinates getCoordinates()
		{
			return this.coordinates;
		}


		public int getGrayValue()
		{
			return this.grayValue;
		}


		/**
		 * @return the label of the Labled_Coordinate
		 */
		public int getLabel()
		{
			return this.label;
		}


		public String getMigrationMode()
		{
			return this.migrationMode;
		}


		public double getXCoordinate()
		{
			return this.coordinates.getXcoordinate();
		}


		public double getYCoordinate()
		{
			return this.coordinates.getYcoordinate();
		}


		public double getZCoordinate()
		{
			return this.coordinates.getZcoordinate();
		}


		/**
		 * @return: label (xValue, yValue, zValue) value
		 */
		@Override
		public String toString()
		{
			final String toString = this.label + " (" + getXCoordinate() + "," + getYCoordinate() + "," + getZCoordinate() + ") " + this.grayValue;
			return toString;
		}
	}

	private class TypedChannel
	{
		public int channelNr;
		public ImagePlus channelImage;
		public String channelType;


		public TypedChannel(final int aNr, final String aType)
		{
			this.channelNr = aNr;
			this.channelType = aType;
		}
	}

	private static final String INPUTDIR_PREF = "Feature_Extractor_3D.InputDir";
	private static final String OUTPUTDIR_PREF = "Feature_Extractor_3D.OutputDir";

	private static final String NONE = "None";
	private static final String NO_CHANNEL = "0";

	private static final int BIN_TOTAL = 20;
	public static final String NUCLEAR = "Nuclear";
	public static final String CELL = "Cell";
	public static final String CELL_NONUCLEUS = "Cell without the nucleus";
	public static final String NUCLEAR_CENTER = "Nuclear centre";
	private static final int NR_OF_ADDITIONAL_CHANNELS = 4;
	private final boolean[] calculateDams = new boolean[2];

	private List<TypedChannel> alternateChannels;
	private ImagePlus dapiImage;
	private ImagePlus actinImage;
	private ImagePlus resultImage;
	private ImagePlus dapiSegments;
	private ImagePlus actinSegments;
	private double[][] amountOfNuclei;
	private double[] amountOfNucleiMigration;
	private boolean saveImages;
	private boolean excludeBorderNuclei;
	private boolean excludeTooSmallNuclei;
	private boolean manualMarkers;


	/**
	 * Count the markers which are located: 0= in a nucleus, 1= multiple in a nucleus, 2= outside all the nuclei, 3 = in an excluded (border or too small) nucleus, 4 = in a disqualified nucleus. The counts are returned as an int array.
	 *
	 * @param aListOfMarkers
	 *            The List of Labeled_Coordinates to be counted (labelled with the migration mode)
	 * @param aCells
	 *            The list of cells to which the markers may belong
	 * @return An int[5] array containing the counts (see above for indexes).
	 */
	private int[] countMarkersAndAddToNucleus(final List<Labeled_Coordinate> aListOfMarkers, final Cell3D[] aCells)
	{
		int markersInExcludedNucleus = 0, markersInDisqualifiedNucleus = 0;
		int markersDoubleInNucleus = 0, markersInNucleus = 0, markersWithoutNucleus = 0;

		for (final Labeled_Coordinate marker : aListOfMarkers)
		{
			boolean markerInNucleus = false;
			final Coordinates markerCoords = marker.getCoordinates();
			for (final Cell3D cell : aCells)
			{
				final Nucleus3D nucleus = cell.getNucleus();
				// If the marker is located inside check if the seed is inside the nucleus
				if (nucleus.contains(markerCoords))
				{
					cell.addMarker(markerCoords, marker.getMigrationMode());
					markerInNucleus = true;
					if (nucleus.isBorderNucleus() || nucleus.isTooSmall())
					{
						markersInExcludedNucleus++;
					}
					if (nucleus.isDisqualified())
					{
						markersInDisqualifiedNucleus++;
					}
					else
					{
						if (nucleus.getMarkersCount() > 1)
						{
							markersDoubleInNucleus++;
						}
						else
						{
							markersInNucleus++;
						}

					}

					break;
				}
			}
			if (!markerInNucleus)
			{
				markersWithoutNucleus++;
			}
		}
		final int[] result = { markersInNucleus, markersDoubleInNucleus, markersWithoutNucleus, markersInExcludedNucleus, markersInDisqualifiedNucleus };
		return result;
	}


	/**
	 * Match the list of seeds to the list of cells and set the seed data in the corresponding cell.
	 *
	 * @param aSeeds
	 *            A list of seed markers as Labeled_Coordinates
	 * @param aCells
	 *            An array of Cell3Ds
	 */
	private void countSeedsAndAddToNucleus(final List<Labeled_Coordinate> aSeeds, final Cell3D[] aCells)
	{
		for (final Labeled_Coordinate seed : aSeeds)
		{
			for (final Cell3D cell : aCells)
			{
				final Nucleus3D nucleus = cell.getNucleus();
				// If the nucleus and the seed contains the same value the seeds is added to the nucleus
				if (seed.getLabel() == nucleus.getLabel())
				{
					nucleus.setSeed(seed.getCoordinates(), seed.getGrayValue());
					break;
				}
			}
		}
	}


	/**
	 * This method prepares two images for displaying the segmentation results. The input image is converted to a RGB image and a second blank RGB image (black background) is created with the same dimensions as the input image.
	 *
	 * @param aImage
	 *            The input image. This method will convert this image to an RGB image.
	 *
	 * @return A blank RGB image of the same dimensions as the input image.
	 */
	private ImagePlus createBackgroundResultImages(final ImagePlus aImage)
	{

		// Create results image with the original image as background
		final ImageConverter con = new ImageConverter(aImage);
		con.convertToRGB();
		aImage.setTitle("Nucleus outlines");

		// Create result image with black background
		final int height = aImage.getHeight();
		final int width = aImage.getWidth();
		final ImagePlus resultImageBlack = IJ.createImage("Nucleus segments", "16-bit Black", width, height, aImage.getNSlices());
		final ImageConverter con2 = new ImageConverter(resultImageBlack);
		con2.convertToRGB();

		aImage.show();
		resultImageBlack.show();

		return resultImageBlack;
	}


	/**
	 * Collect the spheroid information. Currently the user has to pick a few points to distill a sphere from. The points should be on the edge of the spheroid and one set of three points should be in the X/Y plane while the other set should be in
	 * the Y/Z plane.
	 *
	 * @return The Spheroid that can be constructed based on the two intersecting circles that are deduced from the points given.
	 */
	private Spheroid getSpheroidInformation()
	{
		// Calculate the z to x/y ratio to get the proper fit
		final double zFactor = this.dapiImage.getCalibration().pixelDepth / this.dapiImage.getCalibration().pixelWidth;

		final GenericDialog parametersDialog = new GenericDialog("Coordinates input");
		parametersDialog.addMessage("Please input three coordinates with a similar Z coordinate:");
		parametersDialog.addStringField("Coordinate 1", "0.0\t0.0\t0.0", 20);
		parametersDialog.addStringField("Coordinate 2", "0.0\t0.0\t0.0", 20);
		parametersDialog.addStringField("Coordinate 3", "0.0\t0.0\t0.0", 20);
		parametersDialog.addMessage("Please input three coordinates with a similar X coordinate:");
		parametersDialog.addStringField("Coordinate 4", "0.0\t0.0\t0.0", 20);
		parametersDialog.addStringField("Coordinate 5", "0.0\t0.0\t0.0", 20);
		parametersDialog.addStringField("Coordinate 6", "0.0\t0.0\t0.0", 20);
		parametersDialog.showDialog();

		if (parametersDialog.wasOKed())
		{
			final Coordinates coord1 = stringToCoordinate(parametersDialog.getNextString());
			final Coordinates coord2 = stringToCoordinate(parametersDialog.getNextString());
			final Coordinates coord3 = stringToCoordinate(parametersDialog.getNextString());
			final Coordinates coord4 = stringToCoordinate(parametersDialog.getNextString());
			final Coordinates coord5 = stringToCoordinate(parametersDialog.getNextString());
			final Coordinates coord6 = stringToCoordinate(parametersDialog.getNextString());

			// Swap the x and z coordinates in the last set to get a proper circle estimate (which is based on the X/Y plane)
			swapXZ(coord4);
			swapXZ(coord5);
			swapXZ(coord6);
			Coordinates center = MyMath.circleCentre(coord4, coord5, coord6);

			// Set the Y and Z coordinate based on the Y/Z circle centre
			final Coordinates centerTot = new Coordinates(0, 0, 0);
			centerTot.setYcoordinate(center.getYcoordinate());
			centerTot.setZcoordinate(center.getXcoordinate() / zFactor);

			// Get the circle estimate of the X?Y plane
			center = MyMath.circleCentre(coord1, coord2, coord3);
			centerTot.setXcoordinate(center.getXcoordinate());

			// Middle the Y coordinate between the two circles for the best estimate
			centerTot.setYcoordinate((center.getYcoordinate() + centerTot.getYcoordinate()) / 2);
			final double radius = centerTot.distanceFromPoint(coord1);
			return new Spheroid(centerTot, radius);
		}

		// Dialog was cancelled, return null.
		return null;
	}


	/**
	 * Get the title of an image and remove its file extension part (if any).
	 *
	 * @param aImage
	 *            The image for the shorter title
	 * @return The shortened image title
	 */
	private String getTitleWithoutExtension(final ImagePlus aImage)
	{
		String subTitle = aImage.getTitle();
		final int pointIndex = subTitle.lastIndexOf(".");
		if (pointIndex > 0)
		{
			subTitle = subTitle.substring(0, pointIndex);
		}
		return subTitle;
	}


	/**
	 * Measure the images (segmented and parameter) based on a set of MorphoLibJ and MCIB3D measurements and return the list of measured cells.
	 *
	 * @param aImage
	 *            The image to measure
	 * @param aIndex
	 *            The index of the segmented image that corresponds to the image
	 * @return An array of Cell3D with the measurements
	 */
	private Cell3D[] measureFeatures()
	{
		final boolean[] particleMeasure = { true, true, true, true, true, true };
		final boolean[] suite3DMeasure = { true, true, true, true, true, true, true, true, true, true, true, true, true, true };

		return CellMeasurer.getMeasuredCells(this.dapiImage, this.actinImage, this.dapiSegments, this.actinSegments, this.calculateDams, particleMeasure, suite3DMeasure);
	}


	/**
	 * From a list of marker-file names, select the file which matches the given image title and read the markers from the file. This produces two lists: one containing the marker Coordinates and one containing the Coordinates and all other info
	 * contained in the marker file.
	 *
	 * @param aMarkerFile
	 *            The list of possible marker-file names
	 * @param aReadCoordinates
	 *            The list of coordinates that will be extended with the marker coordinates
	 * @return A list of Labeled_Coordinate containing all the marker coordinates of the marker file and their additional values such as the label and detection value.
	 */
	private List<Labeled_Coordinate> readMarkerFile(final File aMarkerFile, final List<Coordinates> aReadCoordinates)
	{
		final List<Labeled_Coordinate> listofSeeds = new ArrayList<>();
		try
		{
			final FileReader fileReader = new FileReader(aMarkerFile);
			final BufferedReader br = new BufferedReader(fileReader);
			String line;
			final String splitter = "\t";
			final boolean getZ = this.dapiImage.getNSlices() != 1;
			try
			{
				// Read lines until they run out
				while ((line = br.readLine()) != null)
				{
					// Split line into columns
					final String[] columns = line.split(splitter);
					// Skip any line that starts with a space
					if (!columns[0].contains(" "))
					{
						final int label = Float.valueOf(columns[1]).intValue();
						final int xValue = Float.valueOf(columns[2]).intValue();
						final int yValue = Float.valueOf(columns[3]).intValue();

						int zValue = 0;
						if (getZ)
						{
							zValue = Float.valueOf(columns[4]).intValue();
						}

						final int value = Float.valueOf(columns[5]).intValue();
						String migrationMode = "";
						if (columns.length == 7)
						{
							migrationMode = columns[6];
						}
						final Coordinates seed = new Coordinates(xValue, yValue, zValue);
						aReadCoordinates.add(seed);
						listofSeeds.add(new Labeled_Coordinate(label, seed, value, migrationMode));
					}
				}
			}
			catch (final IOException ioe)
			{
				IJ.handleException(ioe);
			}
		}
		catch (final FileNotFoundException fnfe)
		{
			IJ.handleException(fnfe);
		}

		return listofSeeds;
	}


	@Override
	public void run(final String arg)
	{
		// Select all the input files and store the marker file names for later processing
		final File[] markerFileNames = selectInputFiles();
		if (markerFileNames == null)
		{
			// No marker files, so cancel
			return;
		}

		// Get the output directory if needed
		File directoryOutputFile = null;
		if (this.saveImages)
		{
			directoryOutputFile = selectDirectory("Select the directory where the results images need to be saved", OUTPUTDIR_PREF);
			if (directoryOutputFile == null)
			{
				// The user wants output, but selects no place to save it. Does not compute.
				return;
			}
		}

		// Set the feature extraction parameters
		final Boolean runMigrationMode = selectFeatureExtractionParameters();

		if (runMigrationMode == null)
		{
			// Dialog was cancelled
			return;
		}

		// Experimental: get the data on the spheroid from the user.
		final Spheroid spheroid = getSpheroidInformation();

		// Measure all the features of the detected cells/nuclei
		final String segmentationTitle = getTitleWithoutExtension(this.dapiSegments);
		IJ.log("Analyze 3D: " + segmentationTitle);
		final Cell3D[] cells = measureFeatures();

		// Before measuring, detect any cells that fail to meet the desired standards.
		PostProcessor.postProcessCellList(cells, this.dapiImage, this.excludeBorderNuclei, this.excludeTooSmallNuclei);

		// Create names which will be used to name and save the files
		final String shortTitleFile = segmentationTitle.substring(segmentationTitle.indexOf("MCWatershed") + 12, segmentationTitle.length());

		// Extract all the seeds that are created by the Point detection method and add them to the nucleus
		final List<Coordinates> seedCoords = new ArrayList<>();
		final String seedFileName = BaseMarkerImageCreator.createOutputFileName(this.dapiImage, BaseMarkerImageCreator.MEXICANHAT);
		File seedFile = null;
		for (final File markers : markerFileNames)
		{
			if (markers.getName().startsWith(seedFileName))
			{
				seedFile = markers;
				break;
			}
		}
		final List<Labeled_Coordinate> listOfSeeds = readMarkerFile(seedFile, seedCoords);
		countSeedsAndAddToNucleus(listOfSeeds, cells);

		int[] markerResults = null;
		List<Labeled_Coordinate> listOfMarkers = new ArrayList<>();
		if (this.manualMarkers)
		{
			final String markerFileName = BaseMarkerImageCreator.createOutputFileName(this.dapiImage, BaseMarkerImageCreator.MANUALPOINTS);
			File markerFile = null;
			for (final File markers : markerFileNames)
			{
				if (markers.getName().startsWith(markerFileName))
				{
					markerFile = markers;
					break;
				}
			}
			listOfMarkers = readMarkerFile(markerFile, seedCoords);
			markerResults = countMarkersAndAddToNucleus(listOfMarkers, cells);
		}

		if (spheroid != null)
		{
			setDistanceToSpheroid(cells, spheroid);
		}

		if (this.alternateChannels != null)
		{
			for (final TypedChannel measureChannel : this.alternateChannels)
			{
				CellMeasurer.measureCoordinatesIntensity(measureChannel.channelImage, measureChannel.channelType, cells);
			}
		}

		// Draw the nucleus and the coordinates of the markers and the seeds
		final ImagePlus blackImage = createBackgroundResultImages(this.resultImage);
		Visualiser.drawNucleusResults(cells, this.resultImage, blackImage, this.dapiSegments);
		Visualiser.drawMarkers(this.resultImage, seedCoords, Color.YELLOW, 3);

		final Cell3D_Group nucleusGroup = new Cell3D_Group(cells[0]);
		final Cell3D_Group nucleusGroupSelection = new Cell3D_Group(cells[0]);
		for (int k = 1; k < cells.length; k++)
		{
			nucleusGroup.setMember(cells[k]);
			if (!cells[k].getNucleus().isBorderNucleus() && !cells[k].getNucleus().isTooSmall())
			{
				nucleusGroupSelection.setMember(cells[k]);
			}
		}

		ResultsTable resultsperGroup = null;
		if (runMigrationMode)
		{
			// TODO check if actin profiling needs to be used as an alternative
			final MigrationModeAnalyser analysis = new MigrationModeAnalyser(this.dapiImage, nucleusGroupSelection, segmentationTitle);
			final List<Cell3D_Group> cellGroups = analysis.getAnalyzedCellGroups();
			// double maxVolume = 0;
			// int maxLabel = 0;
			// for (final Cell3D_Group group : cellGroups)
			// {
			// if (maxVolume < group.getTotalVolumeCell())
			// {
			// maxVolume = group.getTotalVolumeCell();
			// maxLabel = group.getLabel();
			// }
			// }
			this.amountOfNuclei = analysis.getAmountOfNuclei();
			this.amountOfNucleiMigration = analysis.getAmountOfNucleiMigrationMode();
			resultsperGroup = analysis.getResultsTableGroups();
			if (this.saveImages)
			{
				analysis.saveResultImages(directoryOutputFile);
			}
		}

		final ResultsTable mergedTable = ResultsTableGenerator.summaryCellNucleusTable(cells);
		if (mergedTable == null)
		{
			IJ.log("ERROR: A nulceus segment can not be correlated to an automated nucleus marker");
			IJ.log("  Check if the Marker file correspond with the segmented image");
			return;
		}
		mergedTable.show("Results Per Nucleus");

		ResultsTable cellTable = null;
		if (this.actinImage != null)
		{
			cellTable = ResultsTableGenerator.summaryCellTable(cells);
			cellTable.show("Results Per Cell");
		}

		final ResultsTable resultsSum = new ResultsTable();
		ResultsTableGenerator.summaryOfTheImages(resultsSum, this.amountOfNuclei, this.amountOfNucleiMigration, nucleusGroup, listOfSeeds.size(), listOfMarkers.size(), markerResults,
				segmentationTitle, runMigrationMode);
		resultsSum.show("Results summary");

		if (this.saveImages)
		{
			if (resultsperGroup != null)
			{
				ResultsTableGenerator.saveResultsTable(segmentationTitle + "_ResultsPerGroup", directoryOutputFile, resultsperGroup);
			}
			ResultsTableGenerator.saveResultsTable(segmentationTitle + "_NucleusFeatures", directoryOutputFile, mergedTable);
			if (cellTable != null)
			{
				ResultsTableGenerator.saveResultsTable(segmentationTitle + "_CellFeatures", directoryOutputFile, cellTable);
			}
			ResultsTableGenerator.saveResultsTable(getTitleWithoutExtension(this.dapiImage) + "_Summary", directoryOutputFile, resultsSum);
			saveOutputImage(this.resultImage, blackImage, segmentationTitle, directoryOutputFile);
		}

		// TODO: Handle this good/bad/not counted image
		blackImage.changes = false;
		blackImage.close();

		if (spheroid != null)
		{
			final double radius = spheroid.getRadius();
			final float[] migrationDists = new float[cells.length];
			final Paint[] colours = new Paint[cells.length];
			final float[][] cellData = new float[2][cells.length];
			int cellNr = 0;
			for (final Cell3D cell : cells)
			{
				cellData[0][cellNr] = (float) cell.getNucleus().getDistanceToCore();
				cellData[1][cellNr] = (float) cell.getSignalMeasurements().get(0).getMeanIntensity();
				cellData[1][cellNr] = cellData[1][cellNr] / (float) cell.getNucleus().getMeasurements().getMeanIntensity();
				migrationDists[cellNr] = (float) cell.getNucleus().getDistanceToCore();
				if (cell.getMigrationMode().equals(Cell3D_Group.SINGLE))
				{
					colours[cellNr] = Color.RED;
				}
				else
				{
					colours[cellNr] = Color.GREEN;
				}
				cellNr++;
			}

			NucleiDataVisualiser.plotData(cellData, colours, "Migration distance vs normalized actin density", "Migration distance", "Normalized actin density");
			final double maxDist = MyMath.getMaximum(migrationDists);
			final double stepSize = (maxDist + radius) / BIN_TOTAL;
			final double[] bins = new double[BIN_TOTAL];
			final double[] binNumbers = new double[BIN_TOTAL];
			for (int i = 0; i < BIN_TOTAL; i++)
			{
				bins[i] = 0;
				binNumbers[i] = -radius + (i * stepSize);
			}

			for (final double distance : migrationDists)
			{
				int bin = (int) ((distance + radius) / stepSize);
				bin = bin == BIN_TOTAL ? bin - 1 : bin; // Make sure that the max value is in the last bin
				bins[bin]++;
			}

			NucleiDataVisualiser.plotDistanceHistogram(binNumbers, bins, "Migration distance", "Migration distance (bin size = " + stepSize + ")", "Number of cells", null);
		}
	}


	/**
	 * Save the two output images in the given directory.
	 *
	 * @param aOutlinesImage
	 *            The image of nucleus outlines and seeds
	 * @param aNucleusImage
	 *            The image of nucleus segmentation coloured to depict its correctness
	 * @param aTitle
	 *            The main title for both images
	 * @param aDirectory
	 *            The directory in which to save the images
	 */
	private void saveOutputImage(final ImagePlus aOutlinesImage, final ImagePlus aNucleusImage, final String aTitle, final File aDirectory)
	{

		final File fileImages = new File(aDirectory.getPath());
		if (!fileImages.exists())
		{
			if (fileImages.mkdir())
			{
				IJ.log("Directory is created!");
			}
			else
			{
				IJ.log("ERROR: Failed to create directory!");
			}
		}
		final String imageNameForResultImageOriginal = "\\" + aTitle + "_Outlines" + ".tiff";
		final String imageNameForResultImageBlack = "\\" + aTitle + "_Nucleus" + ".tiff";
		final String name1 = fileImages.getPath() + imageNameForResultImageOriginal;
		final String name2 = fileImages.getPath() + imageNameForResultImageBlack;

		IJ.saveAs(aOutlinesImage, "Tif", name1);
		IJ.saveAs(aNucleusImage, "Tif", name2);
		IJ.log("OutputImage is save as: " + imageNameForResultImageOriginal + " in " + name1);
	}


	/**
	 * Create a dialog to have the user define which image channels are relevant to the feature extraction. Two of the standard channels are the nucleus (DAPI) channel and the actin (mostly phalloidin) channel. Any additional channels can be defined
	 * as well including the type of measurement that should be performed on them. The measurement types consist of averages of intensity in the nucleus, a specified volume around the nucleus seed, the full cell segment or the cell inus the nucleus.
	 *
	 * @param aImage
	 *            The ImagePlus on which the feature extraction will take place. This determines the number of channels to be chosen.
	 *
	 * @return The List of TypedChannels (channel plus measurement type String) that have been selected; this includes the standard channels.
	 */
	private List<TypedChannel> selectChannels(final ImagePlus aImage)
	{
		final List<TypedChannel> resultChannels = new ArrayList<>();
		final int channels = aImage.getNChannels();
		final GenericDialog gd = new GenericDialog("Set channels");
		final String[] channelchooser = new String[channels + 1];
		for (int i = 0; i <= channels; i++)
		{
			channelchooser[i] = i + "";
		}
		final String[] channelType = { NUCLEAR_CENTER, NUCLEAR, CELL, CELL_NONUCLEUS };

		gd.addMessage("Please select which signal is on what image channel.\nSelect 0 to ignore a channel.");
		gd.addChoice("Nucleus channel", channelchooser, channelchooser[1]);
		gd.addChoice("Actin channel", channelchooser, channelchooser[0]);
		final Component message = gd.getMessage();
		final Font underlineFont = message.getFont();
		final Map<TextAttribute, Object> attributes = new HashMap<>(underlineFont.getAttributes());
		attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
		attributes.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
		gd.addMessage("Additional channels:", underlineFont.deriveFont(attributes));
		for (int i = 0; i < NR_OF_ADDITIONAL_CHANNELS; i++)
		{
			gd.addChoice("Additional channel " + (i + 1), channelchooser, channelchooser[i]);
			gd.addChoice("Additional channel type " + (i + 1), channelType, channelType[i]);
		}

		for (int i = 0; i < NR_OF_ADDITIONAL_CHANNELS - 1; i++)
		{
			// Set all but the first additional channel choices to disabled
			Choice nextChoice = (Choice) gd.getChoices().get((2 * i) + 4); // 4 = nucleus + actin + first additional channel + measure type
			nextChoice.setEnabled(false);
			// And the measure type choice as well
			nextChoice = (Choice) gd.getChoices().get((2 * i) + 5);
			nextChoice.setEnabled(false);
		}

		gd.addDialogListener(new DialogListener()
		{
			// This listener will enable and disable subsequent choices if a channel choice is (de)selected.
			@Override
			public boolean dialogItemChanged(final GenericDialog gd, final AWTEvent e)
			{
				if (e instanceof ItemEvent)
				{
					final Choice changedChoice = (Choice) ((ItemEvent) e).getItemSelectable();
					int changedChoiceIndex = -1;
					final Vector<Choice> choices = gd.getChoices();

					// First find the last enabled channel choice.
					// Note that the first two choices are always nucleus and actin and next are a number of pairs of a channel choice and a type of measure
					int lastEnabledIndex = -1;
					for (int i = choices.size() - 2; i > 1; i = i - 2)
					{
						final Choice currentChoice = choices.get(i);
						if (changedChoice.equals(currentChoice))
						{
							changedChoiceIndex = i;
						}
						if (currentChoice.isEnabled() && lastEnabledIndex < 1)
						{
							// Working backward, so the first one found is the last enabled one
							lastEnabledIndex = i;
						}
					}

					if (lastEnabledIndex < choices.size() - 2 && changedChoiceIndex == lastEnabledIndex && !changedChoice.getSelectedItem().equals(NO_CHANNEL))
					{
						// The changed choice is the last enabled one, there are more choices after it (to enable) and it has been changed to a valid choice
						// Enable the next two choices (one channel and one measure type)
						choices.get(lastEnabledIndex + 2).setEnabled(true);
						choices.get(lastEnabledIndex + 3).setEnabled(true);
					}
					else if (changedChoiceIndex != lastEnabledIndex && changedChoice.getSelectedItem().equals(NO_CHANNEL))
					{
						// A choice (not the last one) is set to no channel: clean up the following choices and disable them
						for (int i = changedChoiceIndex + 2; i <= lastEnabledIndex; i = i + 2) // Start at the next channel choice and end at the last enabled one
						{
							final Choice channelChoice = choices.get(i);
							channelChoice.setEnabled(false);
							channelChoice.select(NO_CHANNEL);
							choices.get(i + 1).setEnabled(false);
						}
					}
					gd.revalidate();
					gd.repaint();
				}

				return true;
			}
		});

		gd.showDialog();

		resultChannels.add(new TypedChannel(Integer.parseInt(gd.getNextChoice()), NUCLEAR));
		resultChannels.add(new TypedChannel(Integer.parseInt(gd.getNextChoice()), CELL));
		for (int i = 2; i < gd.getChoices().size(); i = i + 2)
		{
			resultChannels.add(new TypedChannel(Integer.parseInt(gd.getNextChoice()), gd.getNextChoice()));
		}
		return resultChannels;
	}


	/**
	 * Select a directory (default based on a stored preference).
	 *
	 * @param aTitle
	 *            The title of the selection Dialog
	 * @param aPref
	 *            The preference used to store the default dir's parent directory
	 * @return The selected directory or null if none was selected
	 */
	private File selectDirectory(final String aTitle, final String aPref)
	{
		final String prefInputFile = Prefs.get(aPref, null);
		final JFileChooser file = aPref == null ? new JFileChooser() : new JFileChooser(prefInputFile);
		file.setDialogTitle(aTitle);
		file.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		if (file.showOpenDialog(IJ.getInstance()) == JFileChooser.CANCEL_OPTION)
		{
			return null;
		}

		final File directory = file.getSelectedFile();
		Prefs.set(aPref, directory.getParent());

		return directory;
	}


	/**
	 * Set the parameters used in this feature extraction plugin. This, for now, includes a setting for dam usage and a setting for migration mode analysis if an actin channel is available.
	 *
	 * @return True if action migration mode analysis needs to take place, false otherwise.
	 */
	private Boolean selectFeatureExtractionParameters()
	{
		final GenericDialog gd = new GenericDialog("Select available features");

		gd.addMessage("Settings for the segments");
		gd.addCheckbox("Segmented images are calculated with dams", true);

		if (this.actinImage != null)
		{
			gd.addMessage("Settings for the Actin segments");
			gd.addCheckbox("Migration Mode Analysis", false);
			gd.addCheckbox("Segmented Images (Actin) are calculated with dams", false);
		}

		gd.addMessage("Settings for post processing");
		gd.addCheckbox("Exclude cells with a nucleus touching/crossing any border of the image", true);
		gd.addCheckbox("Exclude cells with a very small nucleus volume", true);

		gd.showDialog();
		Boolean migrationMode = null;

		if (gd.wasOKed())
		{
			this.calculateDams[0] = gd.getNextBoolean();
			migrationMode = gd.getNextBoolean();

			if (this.actinImage != null)
			{
				this.calculateDams[1] = gd.getNextBoolean();
			}

			this.excludeBorderNuclei = gd.getNextBoolean();
			this.excludeTooSmallNuclei = gd.getNextBoolean();
		}

		return migrationMode;
	}


	/**
	 * Handle the selection of the image and marker files via a series of dialogs.
	 *
	 * @return The list of possible marker files or null if the selection process was aborted for some reason.
	 */
	private File[] selectInputFiles()
	{
		// Check the active amount of windows and make a list of names to select the original image and the segmented images
		final int amountWindows = WindowManager.getImageCount();
		if (amountWindows < 2)
		{
			IJ.log("Error: There are not enhoug images open");
			return null;
		}

		final String[] imageNames = new String[amountWindows + 1];
		imageNames[0] = NONE;
		for (int i = 0; i < amountWindows; i++)
		{
			imageNames[i + 1] = WindowManager.getImage(i + 1).getTitle();
		}

		// Ask the which is the original image
		if (!selectOriginalImage(imageNames))
		{
			return null;
		}

		// Select all the segmented images (nucleus and optionally actin)
		if (!selectSegmentedImages(imageNames))
		{
			return null;
		}

		// Select the directory where the marker files (not images) are located
		final File directoryInputFile = selectDirectory("Select the directory where the marker files are located", INPUTDIR_PREF);
		if (directoryInputFile == null)
		{
			return null;
		}

		// Make an array with the file names in the selected folder to select the manual point file
		final File[] filenamesFile = directoryInputFile.listFiles();

		return filenamesFile;
	}


	/**
	 * A Dialog asks the user to select the original image, which can be a 2D, 3D or a hyperstack image. The user can also assign an adjusted version of the original image as input. This adjusted image will then be used as the base for the output
	 * images.
	 *
	 * @param aNames
	 *            The list of names from which the user can choose. The first name is taken as default.
	 * @return False if the Dialog has been cancelled has been cancelled, true otherwise.
	 */
	private boolean selectOriginalImage(final String[] aNames)
	{
		ImagePlus originalImage = null;

		while (originalImage == null)
		{
			final GenericDialog gd = new GenericDialog("Original image");
			gd.addMessage("Select the original image and, optionally, an adjusted version of the original image, \n" + "which will be used for the output images");
			gd.addChoice("Original image", aNames, aNames[0]);
			gd.addChoice("Adjusted image for output", aNames, aNames[0]);
			gd.addMessage(""); // Add an empty line for readability
			gd.addCheckbox("Save the results", false);
			gd.addCheckbox("Include manual markers", false);
			gd.showDialog();

			if (gd.wasCanceled())
			{
				return false;
			}

			final String originalImageName = gd.getNextChoice();
			final String originalImageName2 = gd.getNextChoice();
			this.saveImages = gd.getNextBoolean();
			this.manualMarkers = gd.getNextBoolean();
			originalImage = WindowManager.getImage(originalImageName);
			this.resultImage = WindowManager.getImage(originalImageName2);
		}

		// Check if the original image is a hyperstack or Z-Stack
		// If it is a hyperstack, split the different channels
		if (originalImage.getNChannels() > 1)
		{
			final List<TypedChannel> channels = selectChannels(originalImage);

			if (channels.get(0).channelNr == 0)
			{
				return false; // No DAPI channel chosen
			}

			final ImagePlus[] channel = ChannelSplitter.split(originalImage);
			this.dapiImage = channel[channels.get(0).channelNr - 1];
			this.dapiImage.setTitle(originalImage.getShortTitle());
			if (channels.get(1).channelNr != 0)
			{
				this.actinImage = channel[channels.get(1).channelNr - 1];
			}

			this.alternateChannels = new ArrayList<>();
			for (int channelNr = 2; channelNr < channels.size(); channelNr++)
			{
				final TypedChannel nextChannel = channels.get(channelNr);
				if (nextChannel.channelNr != 0)
				{
					nextChannel.channelImage = channel[nextChannel.channelNr - 1];
					this.alternateChannels.add(nextChannel);
				}
				else
				{
					break;
				}
			}

			// Save memory by closing the unneeded images
			for (int i = 0; i < channel.length; i++)
			{
				if (i != channels.get(0).channelNr - 1 && i != channels.get(1).channelNr - 1 && i != channels.get(2).channelNr - 1)
				{
					channel[i].close();
				}
			}
		}
		else
		{
			this.dapiImage = originalImage;
		}

		if (this.resultImage == null)
		{
			this.resultImage = this.dapiImage.duplicate();
			this.resultImage.show();
		}

		return true;
	}


	/**
	 * This Dialog asks the user for the input of segmented images. The Dialog can be cancelled.
	 *
	 * @param aimages,
	 *            the list where the images will be located
	 * @param anames,
	 *            a array with names of the active images in FIJI @return, if the dialogue was oked
	 */
	private boolean selectSegmentedImages(final String[] aNames) // Dialog for the Labeled nucleus images
	{
		while (this.dapiSegments == null)
		{
			final GenericDialog gd = new GenericDialog("Options");
			gd.addMessage("Please select the segmented images\n ");
			gd.addChoice("Nucleus segmented image", aNames, aNames[0]);
			gd.addChoice("Actin segmented image (optional)", aNames, aNames[0]);
			gd.showDialog();

			if (gd.wasCanceled())
			{
				return false;
			}

			final String imageIndex1 = gd.getNextChoice();
			final String imageIndex2 = gd.getNextChoice();

			if (imageIndex1 != NONE)
			{
				this.dapiSegments = WindowManager.getImage(imageIndex1);
			}

			if (imageIndex2 != NONE)
			{
				this.actinSegments = WindowManager.getImage(imageIndex2);
			}
		}

		// Check image sizes
		final int width = this.dapiImage.getWidth();
		final int height = this.dapiImage.getHeight();
		if (width != this.dapiSegments.getWidth() || height != this.dapiSegments.getHeight()
				|| (this.actinSegments != null && (width != this.actinSegments.getWidth() || height != this.actinSegments.getHeight())))
		{
			IJ.error("Feature_Extractor_3D input error", "Error: segmented images must have the same size as the original image");
			return false;
		}

		return true;
	}


	/**
	 * Calculate and set the distance of all cells compared to the spheroid centre and edge.
	 *
	 * @param aCells
	 *            The list of Cell3Ds to process
	 * @param aCoreCentre
	 *            The Coordinates of the spheroid centre
	 * @param aCoreRadius
	 *            The radius of the spheroid
	 */
	private void setDistanceToSpheroid(final Cell3D[] aCells, final Spheroid aSpheroid)
	{
		final double zFactor = this.dapiImage.getCalibration().pixelDepth / this.dapiImage.getCalibration().pixelWidth;
		for (final Cell3D cell : aCells)
		{
			final Nucleus3D nucleus = cell.getNucleus();
			nucleus.setDistanceToCentre(nucleus.getSeed().correctedDistanceFromPoint(aSpheroid.getCentre(), zFactor));
			nucleus.setDistanceToCore(nucleus.getDistanceToCentre() - aSpheroid.getRadius());
		}

	}


	/**
	 * Split a String into three separate coordinates (x, y and z). The String can be split along the tab or comma characters.
	 *
	 * @param aString
	 *            The String to split
	 * @return The Coordinates object that can be constructed from the String
	 */
	private Coordinates stringToCoordinate(final String aString)
	{
		String[] coords;
		if (aString.contains("\t"))
		{
			coords = aString.split("\t");
		}
		else
		{
			coords = aString.split(",");
		}
		final Coordinates result = new Coordinates(Double.parseDouble(coords[0]), Double.parseDouble(coords[1]), Double.parseDouble(coords[2]));
		return result;
	}


	/**
	 * Swap the X and Z coordinates of a set of Coordinates. Used to get a good circle estimate (method is based on X/Y plane).
	 *
	 * @param aCoordinates
	 *            The target Coordinates. Note, these will actually be changed. No copy is made.
	 */
	private void swapXZ(final Coordinates aCoordinates)
	{
		final double zFactor = this.dapiImage.getCalibration().pixelDepth / this.dapiImage.getCalibration().pixelWidth;

		final double temp = aCoordinates.getXcoordinate();
		aCoordinates.setXcoordinate(aCoordinates.getZcoordinate() * zFactor);
		aCoordinates.setZcoordinate(temp);
	}
}
