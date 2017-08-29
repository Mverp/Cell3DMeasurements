package featureextractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import data.Cell3D;
import data.Cell3D_Group;
import data.Coordinates;
import data.Nucleus3D;
import data.SegmentMeasurements;
import ij.IJ;
import ij.measure.ResultsTable;

/**
 * Create a summary ResultsTable based on the data that has been provided.
 *
 * @author Merijn van Erp
 * @author Esther Markus
 *
 */
public class ResultsTableGenerator
{

	/**
	 * Save a results table as a Excel readable file.
	 *
	 * @param aTitle
	 *            The title to save the table under (without extension!)
	 * @param aDirectory
	 *            The directory to save this results table to
	 * @param aResults
	 *            The ResultsTable to save
	 */
	public static void saveResultsTable(final String aTitle, final File aDirectory, final ResultsTable aResultsTable)
	{
		if (!aDirectory.exists())
		{
			if (aDirectory.mkdir())
			{
				IJ.log("Directory " + aDirectory + " has been created!");
			}
			else
			{
				IJ.log("ERROR: Failed to create directory " + aDirectory + "!");
			}
		}
		final String filename = aDirectory.getPath() + File.separator + aTitle + ".xls";
		aResultsTable.save(filename);
		IJ.log("Results tabel has been saved as: " + filename);
	}


	public static ResultsTable summaryCellNucleusTable(final Cell3D[] aCells)
	{

		final ResultsTable resultsTable = new ResultsTable();
		for (final Cell3D cell : aCells)
		{
			final Nucleus3D nucleus = cell.getNucleus();
			resultsTable.incrementCounter();
			resultsTable.addValue("Label", nucleus.getLabel());
			final Coordinates seed = nucleus.getSeed();
			if (seed == null)
			{
				return null;
			}
			resultsTable.addValue("XCordinate", seed.getXcoordinate());
			resultsTable.addValue("YCordinate", seed.getYcoordinate());
			resultsTable.addValue("ZCordinate", seed.getZcoordinate());
			resultsTable.addValue("Amount markers", nucleus.getMarkersCount());
			resultsTable.addValue("Under volume threshold", nucleus.isTooSmall() + "");
			// resultsTable.addValue("Disqualified", nucleus.isDisqualified() + "");
			resultsTable.addValue("BorderNucleus", nucleus.isBorderNucleus() + "");
			resultsTable.addValue("Distance to centre", nucleus.getDistanceToCentre() + "");
			resultsTable.addValue("Distance to spheroid border", nucleus.getDistanceToCore() + "");

			resultsTable.addValue("Manual migration mode", cell.getMarkerMigrationMode());

			final SegmentMeasurements measurements = nucleus.getMeasurements();
			resultsTable.addValue("MeanGray", measurements.getMeanIntensity());
			resultsTable.addValue("StdDev", measurements.getStandardDeviation());
			resultsTable.addValue("Max", measurements.getMaxIntensity());
			resultsTable.addValue("Min", measurements.getMinIntensity());
			resultsTable.addValue("Median", measurements.getMedianIntensity());
			resultsTable.addValue("Mode", measurements.getModeIntensity());
			resultsTable.addValue("Skewness", measurements.getSkewness());
			resultsTable.addValue("Kurtiosis", measurements.getKurtosis());
			resultsTable.addValue("LoGValue", measurements.getMexicanHatValue());
			resultsTable.addValue("NumberOfVoxels", nucleus.getNumberOfVoxels());
			resultsTable.addValue("Volume", nucleus.getVolume());
			resultsTable.addValue("SurfaceArea", measurements.getSurfaceArea());
			resultsTable.addValue("Sphericities", measurements.getSphericities());
			resultsTable.addValue("EulerNumber", measurements.getEulerNumber());

			resultsTable.addValue("Ellipsiod centerX", measurements.getEllipsoid()[0]);
			resultsTable.addValue("Ellipsiod centerY", measurements.getEllipsoid()[1]);
			resultsTable.addValue("Ellipsiod centerZ", measurements.getEllipsoid()[2]);
			resultsTable.addValue("Ellipsiod radius 1", measurements.getEllipsoid()[3]);
			resultsTable.addValue("Ellipsiod radius 2", measurements.getEllipsoid()[4]);
			resultsTable.addValue("Ellipsiod radius 3", measurements.getEllipsoid()[5]);
			resultsTable.addValue("Ellipsiod radius Azim", measurements.getEllipsoid()[6]);
			resultsTable.addValue("Ellipsiod radius Elev", measurements.getEllipsoid()[7]);
			resultsTable.addValue("Ellipsiod radius Roll", measurements.getEllipsoid()[8]);
			resultsTable.addValue("Ellongation R1/R2", measurements.getElongations()[0]);
			resultsTable.addValue("Ellongation R1/R3", measurements.getElongations()[1]);
			resultsTable.addValue("Ellongation R2/R3", measurements.getElongations()[2]);
			resultsTable.addValue("Inscribed Sphere centerX", measurements.getInscribedSphere()[0]);
			resultsTable.addValue("Inscribed Sphere centerY", measurements.getInscribedSphere()[1]);
			resultsTable.addValue("Inscribed Sphere centerZ", measurements.getInscribedSphere()[2]);
			resultsTable.addValue("Inscribed Sphere radius", measurements.getInscribedSphere()[3]);

			resultsTable.addValue("Mean Gray", measurements.getMeanIntensity3D());
			resultsTable.addValue("StandardDeviation3D", measurements.getStandardDeviation3D());
			resultsTable.addValue("MinimumGrayValue", measurements.getMinimum3D());
			resultsTable.addValue("MaximumGrayValue", measurements.getMaximum3D());
			resultsTable.addValue("IntegratedDensity3D", measurements.getIntegratedDensity3D());
			resultsTable.addValue("Volume Pixels", measurements.getVolumePixels());
			resultsTable.addValue("Volume Unit", measurements.getVolumeUnit());
			resultsTable.addValue("Area Pixels", measurements.getAreaPixels());
			resultsTable.addValue("Area Unit", measurements.getAreaUnit());
			resultsTable.addValue("Compactness", measurements.getCompactness());
			resultsTable.addValue("Sphericity", measurements.getSphericity());
			resultsTable.addValue("Elongatio", measurements.getElongatio());
			resultsTable.addValue("Flatness", measurements.getFlatness());
			resultsTable.addValue("Spareness", measurements.getSpareness());
		}

		return resultsTable;
	}


	public static ResultsTable summaryCellTable(final Cell3D[] aCells)
	{

		final ResultsTable resultsTable = new ResultsTable();
		for (final Cell3D cell : aCells)
		{
			final Nucleus3D nucleus = cell.getNucleus();
			resultsTable.incrementCounter();
			resultsTable.addValue("Label", nucleus.getLabel());

			final SegmentMeasurements measurements = cell.getMeasurements();
			resultsTable.addValue("MeanGray", measurements.getMeanIntensity());
			resultsTable.addValue("StdDev", measurements.getStandardDeviation());
			resultsTable.addValue("Max", measurements.getMaxIntensity());
			resultsTable.addValue("Min", measurements.getMinIntensity());
			resultsTable.addValue("Median", measurements.getMedianIntensity());
			resultsTable.addValue("Mode", measurements.getModeIntensity());
			resultsTable.addValue("Skewness", measurements.getSkewness());
			resultsTable.addValue("Kurtiosis", measurements.getKurtosis());
			final List<SegmentMeasurements> signalMeasurements = cell.getSignalMeasurements();
			if (signalMeasurements != null && !signalMeasurements.isEmpty())
			{
				int i = 0;
				for (final SegmentMeasurements measure : signalMeasurements)
				{
					resultsTable.addValue("Additional Channel " + i + " Mean Intensity Signal ", measure.getMeanIntensity());
					i++;
				}
			}
			if (nucleus.getDistanceToCentre() != nucleus.getDistanceToCore())
			{
				resultsTable.addValue("Migration distance", nucleus.getDistanceToCore());
			}
			resultsTable.addValue("NumberOfVoxels", nucleus.getNumberOfVoxels());
			resultsTable.addValue("Volume", nucleus.getVolume());
			resultsTable.addValue("SurfaceArea", measurements.getSurfaceArea());
			resultsTable.addValue("Sphericities", measurements.getSphericities());
			resultsTable.addValue("EulerNumber", measurements.getEulerNumber());

			resultsTable.addValue("Ellipsiod centerX", measurements.getEllipsoid()[0]);
			resultsTable.addValue("Ellipsiod centerY", measurements.getEllipsoid()[1]);
			resultsTable.addValue("Ellipsiod centerZ", measurements.getEllipsoid()[2]);
			resultsTable.addValue("Ellipsiod radius 1", measurements.getEllipsoid()[3]);
			resultsTable.addValue("Ellipsiod radius 2", measurements.getEllipsoid()[4]);
			resultsTable.addValue("Ellipsiod radius 3", measurements.getEllipsoid()[5]);
			resultsTable.addValue("Ellipsiod radius Azim", measurements.getEllipsoid()[6]);
			resultsTable.addValue("Ellipsiod radius Elev", measurements.getEllipsoid()[7]);
			resultsTable.addValue("Ellipsiod radius Roll", measurements.getEllipsoid()[8]);
			resultsTable.addValue("Ellongation R1/R2", measurements.getElongations()[0]);
			resultsTable.addValue("Ellongation R1/R3", measurements.getElongations()[1]);
			resultsTable.addValue("Ellongation R2/R3", measurements.getElongations()[2]);
			resultsTable.addValue("Inscribed Sphere centerX", measurements.getInscribedSphere()[0]);
			resultsTable.addValue("Inscribed Sphere centerY", measurements.getInscribedSphere()[1]);
			resultsTable.addValue("Inscribed Sphere centerZ", measurements.getInscribedSphere()[2]);
			resultsTable.addValue("Inscribed Sphere radius", measurements.getInscribedSphere()[3]);

			resultsTable.addValue("Mean Intensity Value 3D", measurements.getMeanIntensity3D());
			resultsTable.addValue("Standard Deviation 3D", measurements.getStandardDeviation3D());
			resultsTable.addValue("Minimum Intensity Value 3D", measurements.getMinimum3D());
			resultsTable.addValue("Maximum Intensity Value 3D", measurements.getMaximum3D());
			resultsTable.addValue("Integrated Density 3D", measurements.getIntegratedDensity3D());
			resultsTable.addValue("Volume Pixels", measurements.getVolumePixels());
			resultsTable.addValue("Volume Unit", measurements.getVolumeUnit());
			resultsTable.addValue("Area Pixels", measurements.getAreaPixels());
			resultsTable.addValue("Area Unit", measurements.getAreaUnit());
			resultsTable.addValue("Compactness", measurements.getCompactness());
			resultsTable.addValue("Sphericity", measurements.getSphericity());
			resultsTable.addValue("Elongatio", measurements.getElongatio());
			resultsTable.addValue("Flatness", measurements.getFlatness());
			resultsTable.addValue("Spareness", measurements.getSpareness());
		}

		return resultsTable;
	}


	public static void summaryOfTheImages(final ResultsTable aResultsTable, final double[][] aAmountOfNuclei, final double[] aAmountOfNucleiMigration, final Cell3D_Group aCell3DGroup,
			final int aNumberOfSeeds, final int aNumberOfMarkers, final int[] aMarkerCounts, final String aTitle, final boolean aMigrationModeInfo)
	{
		aResultsTable.incrementCounter(); // Fill the new table
		aResultsTable.addValue("Amount", aCell3DGroup.getMemberCount()); // Add the amount value of the image

		// TODO: check this
		aResultsTable.addValue("% Nucleus correct segmented", aCell3DGroup.getPercentageNucleusCorrectSegmented());
		aResultsTable.addValue("% Nucleus oversegmented", aCell3DGroup.getPercentageNucleusOverSegmented());
		aResultsTable.addValue("% Nucleus undersegmented", aCell3DGroup.getPercentageNucleusUnderSegmented());

		aResultsTable.addValue("Nucleus correct segmented", aCell3DGroup.getNucleusCorrectSegmented());
		aResultsTable.addValue("Nucleus oversegmented", aCell3DGroup.getNucleusOverSegmented());
		aResultsTable.addValue("Nucleus undersegmented", aCell3DGroup.getNucleusUnderSegmented());

		aResultsTable.addValue("Nucleus selected", aCell3DGroup.getNucleusSelected());
		aResultsTable.addValue("Nucleus excluded (correct segmented)", aCell3DGroup.getNucleusExcludedType()[0]);
		aResultsTable.addValue("Nucleus excluded (oversegmented)", aCell3DGroup.getNucleusExcludedType()[1]);
		aResultsTable.addValue("Nucleus excluded (undersegmented)", aCell3DGroup.getNucleusExcludedType()[2]);
		aResultsTable.addValue("Nucleus deselected on borderand regression", aCell3DGroup.getNucleusTwiceExcluded());
		aResultsTable.addValue("Excluded nuclei on size and border", aCell3DGroup.getNucleusExcludedBorderAndSize());
		aResultsTable.addValue("Border nucleus", aCell3DGroup.getNucleusExcludedBorder());
		aResultsTable.addValue("Excluded nuclei on size", aCell3DGroup.getNucleusToSmall());
		aResultsTable.addValue("Excluded nucleus", aCell3DGroup.getNucleusNOTSelected());

		if (aMigrationModeInfo)
		{
			// TODO: check this
			final double totalNuclei = aAmountOfNuclei[0][0];
			final double percentageSpheroid = (aAmountOfNuclei[1][0] / totalNuclei) * 100.0;
			final double percentageCollective = (aAmountOfNuclei[2][0] / totalNuclei) * 100.0;
			final double percentageDualCell = (aAmountOfNuclei[3][0] / totalNuclei) * 100.0;
			final double percentageSingleCell = (aAmountOfNuclei[4][0] / totalNuclei) * 100.0;
			final double percentageVolumeSpheroid = (aAmountOfNuclei[1][1] / aAmountOfNuclei[0][1]) * 100.0;
			final double percentageVolumeCollective = (aAmountOfNuclei[2][1] / aAmountOfNuclei[0][1]) * 100.0;
			final double percentageVolumeDualCell = (aAmountOfNuclei[3][1] / aAmountOfNuclei[0][1]) * 100.0;
			final double percentageVolumeSingleCell = (aAmountOfNuclei[4][1] / aAmountOfNuclei[0][1]) * 100.0;
			final double percentageVolumeSpheroidCell = (aAmountOfNuclei[1][2] / aAmountOfNuclei[0][2]) * 100.0;
			final double percentageVolumeCollectiveCell = (aAmountOfNuclei[2][2] / aAmountOfNuclei[0][2]) * 100.0;
			final double percentageVolumeDualCellCell = (aAmountOfNuclei[3][2] / aAmountOfNuclei[0][2]) * 100.0;
			final double percentageVolumeSingleCellCell = (aAmountOfNuclei[4][2] / aAmountOfNuclei[0][2]) * 100.0;

			final double amountOfCells = aAmountOfNucleiMigration[0] + aAmountOfNucleiMigration[1] + aAmountOfNucleiMigration[2];
			aResultsTable.addValue("% Nucleus correct migrationMode", (aAmountOfNucleiMigration[0] / amountOfCells) * 100.0);
			aResultsTable.addValue("% Nucleus wrong migration mode", (aAmountOfNucleiMigration[1] / amountOfCells) * 100.0);
			aResultsTable.addValue("% Nucleus without migrationMode", (aAmountOfNucleiMigration[2] / amountOfCells) * 100.0);

			final double amountOfSingleCells = aAmountOfNucleiMigration[3] + aAmountOfNucleiMigration[4] + aAmountOfNucleiMigration[5];
			aResultsTable.addValue("% SingleCell correct migrationMode", (aAmountOfNucleiMigration[3] / amountOfSingleCells) * 100.0);
			aResultsTable.addValue("% SingleCell wrong migration mode", (aAmountOfNucleiMigration[4] / amountOfSingleCells) * 100.0);
			aResultsTable.addValue("% SingleCell without migrationMode", (aAmountOfNucleiMigration[5] / amountOfSingleCells) * 100.0);

			aResultsTable.addValue("True positive single cells", aAmountOfNucleiMigration[3]);
			aResultsTable.addValue("False positivie single cells", aAmountOfNucleiMigration[14]);
			aResultsTable.addValue("False negative single cells", aAmountOfNucleiMigration[4] + aAmountOfNucleiMigration[5]);

			aResultsTable.addValue("Single cell segmentation accuracy", (aAmountOfNucleiMigration[3] / (aAmountOfNucleiMigration[3] + aAmountOfNucleiMigration[14])) * 100.0);
			aResultsTable.addValue("Single cell sensitivity", (aAmountOfNucleiMigration[3] / (aAmountOfNucleiMigration[3] + aAmountOfNucleiMigration[4] + aAmountOfNucleiMigration[5])) * 100.0);

			final double amountOfCoreCells = aAmountOfNucleiMigration[6] + aAmountOfNucleiMigration[7];
			if (amountOfCoreCells > 0)
			{
				aResultsTable.addValue("% Core cells without migration mode", (aAmountOfNucleiMigration[6] / amountOfCoreCells) * 100.0);
				aResultsTable.addValue("% Core cells with migration mode", (aAmountOfNucleiMigration[7] / amountOfCoreCells) * 100.0);
			}

			final double amountOfDualCells = aAmountOfNucleiMigration[8] + aAmountOfNucleiMigration[9] + aAmountOfNucleiMigration[10];
			if (amountOfDualCells > 0)
			{
				aResultsTable.addValue("% Dual cell correct migrationMode", (aAmountOfNucleiMigration[8] / amountOfDualCells) * 100.0);
				aResultsTable.addValue("% Dual cell wrong migration mode", (aAmountOfNucleiMigration[9] / amountOfDualCells) * 100.0);
				aResultsTable.addValue("% Dual cell without migrationMode", (aAmountOfNucleiMigration[10] / amountOfDualCells) * 100.0);
			}

			final double amountOfMultiCells = aAmountOfNucleiMigration[11] + aAmountOfNucleiMigration[12] + aAmountOfNucleiMigration[13];
			if (amountOfMultiCells > 0)
			{
				aResultsTable.addValue("% Multi cell correct migration mode", (aAmountOfNucleiMigration[11] / amountOfMultiCells) * 100.0);
				aResultsTable.addValue("% Multi cell wrong migration mode", (aAmountOfNucleiMigration[12] / amountOfMultiCells) * 100.0);
				aResultsTable.addValue("% Multi cell without migration mode", (aAmountOfNucleiMigration[13] / amountOfMultiCells) * 100.0);
			}

			aResultsTable.addValue("% Nuclei in Spheroid", percentageSpheroid);
			aResultsTable.addValue("% Nuclei in Collective mode", percentageCollective);
			aResultsTable.addValue("% Nuclei in Dual mode", percentageDualCell);
			aResultsTable.addValue("% Nuclei in SingleCell mode", percentageSingleCell);
			aResultsTable.addValue("% Volume nucleus in Spheroid", percentageVolumeSpheroid);
			aResultsTable.addValue("% Volume nucleus in Collective mode", percentageVolumeCollective);
			aResultsTable.addValue("% Volume nucleus in Dual mode", percentageVolumeDualCell);
			aResultsTable.addValue("% Volume nucleus in SingleCell mode", percentageVolumeSingleCell);
			aResultsTable.addValue("% Volume cell in Spheroid", percentageVolumeSpheroidCell);
			aResultsTable.addValue("% Volume cell in Collective mode", percentageVolumeCollectiveCell);
			aResultsTable.addValue("% Volume cell in Dual mode", percentageVolumeDualCellCell);
			aResultsTable.addValue("% Volume cell in SingleCell mode", percentageVolumeSingleCellCell);

			aResultsTable.addValue("# Nuclei in Spheroid", aAmountOfNuclei[1][0]);
			aResultsTable.addValue("# Nuclei in Collective mode", aAmountOfNuclei[2][0]);
			aResultsTable.addValue("# Nuclei in Dual mode", aAmountOfNuclei[3][0]);
			aResultsTable.addValue("# Nuclei in SingleCell mode", aAmountOfNuclei[4][0]);
			aResultsTable.addValue("Total Nuclei counted", totalNuclei);
			aResultsTable.addValue("# Volume in Spheroid", aAmountOfNuclei[1][1]);
			aResultsTable.addValue("# Volume in Collective mode", aAmountOfNuclei[2][1]);
			aResultsTable.addValue("# Volume in SingleCell mode", aAmountOfNuclei[3][1]);
		}

		aResultsTable.addValue("Mean volume", aCell3DGroup.getMeanVolume()); // Add the mean area value of the image
		aResultsTable.addValue("Mean number of Voxels", aCell3DGroup.getMeanNumberOfVoxels());
		aResultsTable.addValue("Mean Gray Value", aCell3DGroup.getMeanGrayValue());
		aResultsTable.addValue("Mean STDV", aCell3DGroup.getMeanSTDV());
		aResultsTable.addValue("Mean Skewness", aCell3DGroup.getMeanSkewness());
		aResultsTable.addValue("Mean Kurtosis", aCell3DGroup.getMeanKurtosis());

		aResultsTable.addValue("SurfaceArea", aCell3DGroup.getMeanSurfaceArea());
		aResultsTable.addValue("Mean Sphericities", aCell3DGroup.getMeanSphericities());
		aResultsTable.addValue("Mean Elongation 1", aCell3DGroup.getMeanElongation()[0]);
		aResultsTable.addValue("Mean Elongation 2", aCell3DGroup.getMeanElongation()[1]);
		aResultsTable.addValue("Mean Elongation 3", aCell3DGroup.getMeanElongation()[2]);

		aResultsTable.addValue("Mean Volume Pixels", aCell3DGroup.getMeanVolumePixels());
		aResultsTable.addValue("Mean Volume Unit", aCell3DGroup.getMeanVolumeUnits());
		aResultsTable.addValue("Mean Area Pixels", aCell3DGroup.getMeanAreaPixels());
		aResultsTable.addValue("Mean Area Unit", aCell3DGroup.getMeanAreaUnits());

		aResultsTable.addValue("Mean Compactness", aCell3DGroup.getMeanCompactness());
		aResultsTable.addValue("Mean Sphericity", aCell3DGroup.getMeanSphericity());
		aResultsTable.addValue("Mean Elongatio", aCell3DGroup.getMeanElongatio());
		aResultsTable.addValue("Mean Flatness", aCell3DGroup.getMeanFlatness());
		aResultsTable.addValue("Mean Sparness", aCell3DGroup.getMeanSpareness());

		aResultsTable.addValue("Total seeds", aNumberOfSeeds);

		// Handle the manual marker results
		if (aNumberOfMarkers > 0)
		{
			aResultsTable.addValue("Total markers", aNumberOfMarkers);
			aResultsTable.addValue("Marker in nucleus", aMarkerCounts[0]);
			aResultsTable.addValue("Markers double in nucleus", aMarkerCounts[1]);
			aResultsTable.addValue("Marker without nucleus", aMarkerCounts[2]);
			aResultsTable.addValue("Markers in excluded nucleus", aMarkerCounts[3]);
			aResultsTable.addValue("Marker in disqualified nucleus", aMarkerCounts[4]);

			final double amountMarkers = aMarkerCounts[0] + aMarkerCounts[1] + aMarkerCounts[2];
			final double percentageMarkersFound = ((aMarkerCounts[0]) / amountMarkers) * 100.0;
			final double percentageMarkersDouble = ((aMarkerCounts[1]) / amountMarkers) * 100.0;
			final double percentageMarkersNotFound = ((aMarkerCounts[2]) / amountMarkers) * 100.0;

			aResultsTable.addValue("% Markers in nucleus", percentageMarkersFound);
			aResultsTable.addValue("% Markers outside nucleus", percentageMarkersNotFound);
			aResultsTable.addValue("% Markers double in nucleus", percentageMarkersDouble);
		}

		aResultsTable.setLabel(aTitle, aResultsTable.getCounter() - 1);
	}


	public ResultsTable summaryNucleusGroup(final ArrayList<Cell3D_Group> nucleusGroup)
	{

		final ResultsTable resultsTable = new ResultsTable();

		for (int i = 0; i < nucleusGroup.size(); i++)
		{
			resultsTable.incrementCounter();

			resultsTable.addValue("Mode", nucleusGroup.get(i).getMigrationmode());
			resultsTable.addValue("Manual migration moode", nucleusGroup.get(i).getManualMigrationModeGroup());
			resultsTable.addValue("XCordinate center", nucleusGroup.get(i).getCenterXYZ()[0]);
			resultsTable.addValue("YCordinate center", nucleusGroup.get(i).getCenterXYZ()[1]);
			resultsTable.addValue("ZCordinate center", nucleusGroup.get(i).getCenterXYZ()[2]);
			resultsTable.addValue("Amount of cells", nucleusGroup.get(i).getMemberCount());
			resultsTable.addValue("Total Volume", nucleusGroup.get(i).getTotalVolume());

			resultsTable.addValue("MeanGray", nucleusGroup.get(i).getMeanGrayValue());
			// resultstable.addValue("Mean gray value focal plane", nucleus[k].getGrayValueSecondCannelOnFocalPlane());
			resultsTable.addValue("StdDev", nucleusGroup.get(i).getMeanSTDV());
			// resultsTable.addValue("Max", );
			// resultsTable.addValue("Min", );
			//// resultsTable.addValue("Median", );
			// resultsTable.addValue("Mode", );
			resultsTable.addValue("Skewness", nucleusGroup.get(i).getMeanSkewness());
			resultsTable.addValue("Kurtiosis", nucleusGroup.get(i).getMeanKurtosis());
			resultsTable.addValue("Volume", nucleusGroup.get(i).getMeanVolume());
			resultsTable.addValue("NumberOfVoxels", nucleusGroup.get(i).getMeanNumberOfVoxels());

			resultsTable.addValue("SurfaceArea", nucleusGroup.get(i).getMeanSurfaceArea());
			resultsTable.addValue("Sphericites", nucleusGroup.get(i).getMeanSphericities());
			// resultsTable.addValue("EulerNumber", );

			// resultsTable.addValue("Ellipsiod centerX",nucleus[k].getEllipsoid()[0]);
			// resultsTable.addValue("Ellipsiod centerY",nucleus[k].getEllipsoid()[1]);
			// resultsTable.addValue("Ellipsiod centerZ",nucleus[k].getEllipsoid()[2]);
			// resultsTable.addValue("Ellipsiod radius 1",);
			// resultsTable.addValue("Ellipsiod radius 2",);
			// resultsTable.addValue("Ellipsiod radius 3",);
			// resultsTable.addValue("Ellipsiod radius Azim",);
			// resultsTable.addValue("Ellipsiod radius Elev",);
			// resultsTable.addValue("Ellipsiod radius Roll",);
			resultsTable.addValue("Ellongation R1/R2", nucleusGroup.get(i).getMeanElongation()[0]);
			resultsTable.addValue("Ellongation R1/R3", nucleusGroup.get(i).getMeanElongation()[1]);
			resultsTable.addValue("Ellongation R2/R3", nucleusGroup.get(i).getMeanElongation()[2]);
			// resultsTable.addValue("Inscribed Sphere centerX",);
			// resultsTable.addValue("Inscribed Sphere centerY",);
			// resultsTable.addValue("Inscribed Sphere centerZ",);
			// resultsTable.addValue("Inscribed Sphere radius",);

			resultsTable.addValue("Mean Gray", nucleusGroup.get(i).getMeanGray3D());
			resultsTable.addValue("STDV", nucleusGroup.get(i).getMeanSTDV3D());
			// resultsTable.addValue("MinimumGrayValue", );
			// resultsTable.addValue("MaximumGrayValue", );
			// resultsTable.addValue("IntegratedDensity3D", );
			resultsTable.addValue("Volume Pixels", nucleusGroup.get(i).getMeanVolumePixels());
			resultsTable.addValue("Volume Units", nucleusGroup.get(i).getMeanVolumeUnits());
			resultsTable.addValue("Area Pixels", nucleusGroup.get(i).getMeanAreaPixels());
			resultsTable.addValue("Area Units", nucleusGroup.get(i).getMeanAreaUnits());
			resultsTable.addValue("Compactness", nucleusGroup.get(i).getMeanCompactness());
			resultsTable.addValue("Sphericity", nucleusGroup.get(i).getMeanSphericity());
			resultsTable.addValue("Elongatio", nucleusGroup.get(i).getMeanElongatio());
			resultsTable.addValue("Flatness", nucleusGroup.get(i).getMeanFlatness());
			resultsTable.addValue("Spareness", nucleusGroup.get(i).getMeanSpareness());
		}

		return resultsTable;
	}

}
