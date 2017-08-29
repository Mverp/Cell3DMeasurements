package migrationmodeanalysis;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import data.Cell3D;
import data.Cell3D_Group;
import data.Sort_Groups;
import featureextractor.Visualiser;
import ij.IJ;
import ij.ImagePlus;
import ij.measure.ResultsTable;

public class MigrationModeAnalyser
{
	private final double[][] amountOfNuclei = new double[5][3];
	private final double[] amountOfNucleiMigration = new double[15];
	private final ImagePlus dapiImage;
	private final List<Cell3D_Group> cellGroups;
	private ResultsTable resultsPerGroup;
	private final ImagePlus[] resultImages = new ImagePlus[3];
	private final String imageTitle;


	public MigrationModeAnalyser(final ImagePlus aDAPIImage, final Cell3D_Group aTotalCellList, final String aImageTitle)
	{
		this.dapiImage = aDAPIImage;
		this.imageTitle = aImageTitle;
		this.cellGroups = migrationModeAnalysis(aTotalCellList);
	}


	private void extractInfo(final List<Cell3D_Group> aNucleusGroups)
	{
		double amountNucleusSpheroid = 0, volumeNucleiSpheroid = 0, volumeCellSpheroid = 0;
		double amountNucleiCollective = 0, volumeNucleiCollective = 0, volumeCellsCollective = 0;
		double amountNucleiDualCell = 0, volumeNucleiDualCell = 0, volumeCellDualCell = 0;
		double amountNucleiSingleCell = 0, volumeNucleiSingle = 0, volumeCellSingle = 0;
		double amountNucleiCorrectMigration = 0;
		double amountNucleiWrongMigration = 0;
		double amountNucleuWithoutMigration = 0;
		double amountSingleCellCorrectMigration = 0;
		double amountSingleCellWrongMigration = 0;
		double amountSingleCellWithoutMigration = 0;
		double amountCoreCellsWithoutMigration = 0;
		double amountCoreCellsWithMigration = 0;
		double amountDualCellsWithCorrectMigration = 0;
		double amountDualCellsWrongMigration = 0;
		double amountDualCellsWithoutMigration = 0;
		double amountMultiCellsWithCorrectMigration = 0;
		double amountMultiCellsWrongMigration = 0;
		double amountMultiCellsWithoutMigration = 0;
		double amountSingleCellFalsePositives = 0;
		for (int h = 0; h < aNucleusGroups.size(); h++)
		{
			final Cell3D_Group nucGroup = aNucleusGroups.get(h);
			if (h == 0)
			{
				nucGroup.setMigrationmode(Cell3D_Group.UNKNOWN);
				amountNucleusSpheroid += nucGroup.getMemberCount();
				volumeNucleiSpheroid += nucGroup.getTotalVolume();
				volumeCellSpheroid += nucGroup.getTotalVolumeCell();
			}
			else if (aNucleusGroups.get(h).getMemberCount() > 2)
			{
				nucGroup.setMigrationmode(Cell3D_Group.MULTI);
				amountNucleiCollective += nucGroup.getMemberCount();
				volumeNucleiCollective += nucGroup.getTotalVolume();
				volumeCellsCollective += nucGroup.getTotalVolumeCell();
			}
			else if (nucGroup.getMemberCount() == 2)
			{
				nucGroup.setMigrationmode(Cell3D_Group.DUAL);
				amountNucleiDualCell += nucGroup.getMemberCount();
				volumeNucleiDualCell += nucGroup.getTotalVolume();
				volumeCellDualCell += nucGroup.getTotalVolumeCell();
			}
			else if (nucGroup.getMemberCount() == 1)
			{
				nucGroup.setMigrationmode(Cell3D_Group.SINGLE);
				amountNucleiSingleCell += nucGroup.getMemberCount();
				volumeNucleiSingle += nucGroup.getTotalVolume();
				volumeCellSingle += nucGroup.getTotalVolumeCell();
			}

			for (final Cell3D cell : nucGroup.getMembers())
			{
				cell.setMigrationMode(nucGroup.getMigrationmode());
			}

			amountNucleiCorrectMigration += nucGroup.getNucleusWithCorrectMigrationMode();
			amountNucleiWrongMigration += nucGroup.getNucleusWithWrongMigrationMode();
			amountNucleuWithoutMigration += nucGroup.getNucleusWithoutMigrationMode();
			amountSingleCellCorrectMigration += nucGroup.getSingleCellWithCorrectMigrationMode();
			amountSingleCellWrongMigration += nucGroup.getSingleCellWithWrongMigrationMode();
			amountSingleCellWithoutMigration += nucGroup.getSingleCellWithoutMigrationMode();

			amountCoreCellsWithoutMigration += nucGroup.getCoreCellWithoutMigrationMode();
			amountCoreCellsWithMigration += nucGroup.getCoreCellWithMigrationMode();

			amountDualCellsWithCorrectMigration += nucGroup.getDualCellWithCorrectMigrationMode();
			amountDualCellsWrongMigration += nucGroup.getDualCellWithWrongMigrationMode();
			amountDualCellsWithoutMigration += nucGroup.getDualCellWithoutMigrationMode();

			amountMultiCellsWithCorrectMigration += nucGroup.getMultiCellWithCorrectMigrationMode();
			amountMultiCellsWrongMigration += nucGroup.getMultiCellWithWrongMigrationMode();
			amountMultiCellsWithoutMigration += nucGroup.getMultiCellWithoutMigrationMode();
			amountSingleCellFalsePositives += nucGroup.getSingleCellFalsePositive();

			IJ.log("Group " + h + " migration mode: " + nucGroup.getMigrationmode() + " Members are:");
			IJ.log(nucGroup.getMemberNames());
		}

		this.resultsPerGroup = summaryNucleusGroup(aNucleusGroups);

		this.resultsPerGroup.show("ResultsOfTheGroup");
		this.amountOfNuclei[0][0] = amountNucleusSpheroid + amountNucleiCollective + amountNucleiDualCell + amountNucleiSingleCell;
		this.amountOfNuclei[0][1] = volumeNucleiSpheroid + volumeNucleiCollective + volumeNucleiDualCell + volumeNucleiSingle;
		this.amountOfNuclei[0][2] = volumeCellSpheroid + volumeCellsCollective + volumeCellDualCell + volumeCellSingle;
		this.amountOfNuclei[1][0] = amountNucleusSpheroid;
		this.amountOfNuclei[1][1] = volumeNucleiSpheroid;
		this.amountOfNuclei[1][2] = volumeCellSpheroid;
		this.amountOfNuclei[2][0] = amountNucleiCollective;
		this.amountOfNuclei[2][1] = volumeNucleiCollective;
		this.amountOfNuclei[2][2] = volumeCellsCollective;
		this.amountOfNuclei[3][0] = amountNucleiDualCell;
		this.amountOfNuclei[3][1] = volumeNucleiDualCell;
		this.amountOfNuclei[3][2] = volumeCellDualCell;
		this.amountOfNuclei[4][0] = amountNucleiSingleCell;
		this.amountOfNuclei[4][1] = volumeNucleiSingle;
		this.amountOfNuclei[4][2] = volumeCellSingle;

		this.amountOfNucleiMigration[0] = amountNucleiCorrectMigration;
		this.amountOfNucleiMigration[1] = amountNucleiWrongMigration;
		this.amountOfNucleiMigration[2] = amountNucleuWithoutMigration;
		this.amountOfNucleiMigration[3] = amountSingleCellCorrectMigration;
		this.amountOfNucleiMigration[4] = amountSingleCellWrongMigration;
		this.amountOfNucleiMigration[5] = amountSingleCellWithoutMigration;
		this.amountOfNucleiMigration[6] = amountCoreCellsWithoutMigration;
		this.amountOfNucleiMigration[7] = amountCoreCellsWithMigration;
		this.amountOfNucleiMigration[8] = amountDualCellsWithCorrectMigration;
		this.amountOfNucleiMigration[9] = amountDualCellsWrongMigration;
		this.amountOfNucleiMigration[10] = amountDualCellsWithoutMigration;
		this.amountOfNucleiMigration[11] = amountMultiCellsWithCorrectMigration;
		this.amountOfNucleiMigration[12] = amountMultiCellsWrongMigration;
		this.amountOfNucleiMigration[13] = amountMultiCellsWithoutMigration;
		this.amountOfNucleiMigration[14] = amountSingleCellFalsePositives;

		IJ.log("Total amount of cells: " + this.amountOfNuclei[0][0]);
		IJ.log("Total amount of spheroid cells: " + this.amountOfNuclei[1][0]);
		IJ.log("Total amount of collective cells: " + this.amountOfNuclei[2][0]);
		IJ.log("Total amount of dual cells: " + this.amountOfNuclei[3][0]);
		IJ.log("Total amount of single cells: " + this.amountOfNuclei[4][0]);
	}


	public double[][] getAmountOfNuclei()
	{
		return this.amountOfNuclei;
	}


	public double[] getAmountOfNucleiMigrationMode()
	{
		return this.amountOfNucleiMigration;
	}


	public List<Cell3D_Group> getAnalyzedCellGroups()
	{
		return this.cellGroups;
	}


	public ImagePlus[] getResultsImages()
	{
		return this.resultImages;
	}


	public ResultsTable getResultsTableGroups()
	{
		return this.resultsPerGroup;
	}


	private List<Cell3D> getTouchingNeighbours(final Cell3D aSeed, final List<Cell3D> aTodoCells)
	{
		final ArrayList<Cell3D> neighbours = new ArrayList<>();
		for (final Cell3D cell : aTodoCells)
		{
			if (aSeed.getConnectedNeighbours().contains(cell.getNucleus().getLabel()))
			{
				neighbours.add(cell);
			}
		}

		final List<Cell3D> cellGroup = new ArrayList<>();
		cellGroup.addAll(neighbours);
		aTodoCells.removeAll(neighbours);
		for (final Cell3D neighbour : neighbours)
		{
			final List<Cell3D> foundNeighbours = getTouchingNeighbours(neighbour, aTodoCells);
			cellGroup.addAll(foundNeighbours);
		}

		return cellGroup;
	}


	/**
	 * Method groupBasedOnTouchingNeighbours grouped the nucleus based on thouchingN neighbor characteristics
	 *
	 * @return the nucleus groups
	 */
	private List<Cell3D_Group> groupBasedOnTouchingNeighbours(final Cell3D_Group aTotalListOfCells)
	{
		/*
		 * for cells get new seed cell find connected cells for connected cells find further connections - seed + connected cells + connected connected cells etc. remove group from search
		 *
		 */

		final List<Cell3D_Group> nucleusGroups = new ArrayList<>();
		final List<Cell3D> todoCells = (List<Cell3D>) ((ArrayList) aTotalListOfCells.getMembers()).clone();
		for (final Cell3D seedCell : aTotalListOfCells.getMembers())
		{
			if (todoCells.contains(seedCell))
			{
				final List<Cell3D> cellGroup = new ArrayList<>();
				cellGroup.add(seedCell);
				todoCells.remove(seedCell);
				cellGroup.addAll(getTouchingNeighbours(seedCell, todoCells));
				nucleusGroups.add(new Cell3D_Group(cellGroup));
			}
		}

		return nucleusGroups;
	}


	private List<Cell3D_Group> migrationModeAnalysis(final Cell3D_Group aTotalListOfCells)
	{

		final List<Cell3D_Group> nucleusGroups = groupBasedOnTouchingNeighbours(aTotalListOfCells);
		final List<Cell3D_Group> nucleusGroupsTouchingNeighbours = removeAndSortNucleusGroups(nucleusGroups);

		final ImagePlus migrationModeImage = this.dapiImage.duplicate();
		Visualiser.drawCellGroups(migrationModeImage, nucleusGroupsTouchingNeighbours);
		this.resultImages[0] = migrationModeImage;
		this.resultImages[0].setTitle(this.imageTitle + "_MigrationGroups");
		this.resultImages[0].show();

		extractInfo(nucleusGroupsTouchingNeighbours);

		// TODO What to do about the 'correctness' measure
		// this.resultImages[1] = Visualiser.drawCorrectMigrationMode(this.dapiImage, nucleusGroupsTouchingNeighbours);
		// this.resultImages[1].setTitle(this.imageTitle + "_CorrectMigrationMode");
		this.resultImages[2] = Visualiser.drawMigrationMode(this.dapiImage, nucleusGroupsTouchingNeighbours);
		this.resultImages[2].setTitle(this.imageTitle + "_GroupMigrationMode");
		return nucleusGroupsTouchingNeighbours;
	}


	/**
	 * Method removeAndSortNucleusGroups removed the empty nucleusGroups and sort the groups on amount of members
	 *
	 * @param nucleusGroupsToSort
	 * @return
	 */
	private ArrayList<Cell3D_Group> removeAndSortNucleusGroups(final List<Cell3D_Group> nucleusGroupsToSort)
	{
		final ArrayList<Cell3D_Group> sortedNucleusGroup = new ArrayList<>();
		for (int g = 0; g < nucleusGroupsToSort.size(); g++)
		{
			if (nucleusGroupsToSort.get(g).getMember(0) != null)
			{
				sortedNucleusGroup.add(nucleusGroupsToSort.get(g));
			}
		}
		Collections.sort(sortedNucleusGroup, new Sort_Groups());
		return sortedNucleusGroup;
	}


	/**
	 * Method saveOutputImage save the output images in a directory
	 *
	 * @param aDirectory
	 */
	public void saveResultImages(final File aDirectory)
	{

		final File fileImages = new File(aDirectory.getPath());
		if (!fileImages.exists())
		{
			if (fileImages.mkdir())
			{
				IJ.log("Directory " + fileImages.getName() + " has been created!");
			}
			else
			{
				IJ.log("ERROR: Failed to create directory " + fileImages.getName() + " !");
			}
		}
		final String name1 = fileImages.getPath() + "\\" + this.resultImages[0].getTitle() + ".tiff";
		// final String name2 = fileImages.getPath() + "\\" + this.resultImages[1].getTitle() + ".tiff";
		final String name3 = fileImages.getPath() + "\\" + this.resultImages[2].getTitle() + ".tiff";
		IJ.saveAs(this.resultImages[0], "Tif", name1);
		// IJ.saveAs(this.resultImages[1], "Tif", name2);
		IJ.saveAs(this.resultImages[2], "Tif", name3);
	}


	private ResultsTable summaryNucleusGroup(final List<Cell3D_Group> nucleusGroup)
	{

		final ResultsTable resultsTable = new ResultsTable();

		for (int i = 0; i < nucleusGroup.size(); i++)
		{
			resultsTable.incrementCounter();

			resultsTable.addValue("Mode", nucleusGroup.get(i).getMigrationmode());
			resultsTable.addValue("Manual migration mode", nucleusGroup.get(i).getManualMigrationModeGroup());
			resultsTable.addValue("Amount of cells", nucleusGroup.get(i).getMemberCount());

			resultsTable.addValue("% Nucleus correct migrationMode", nucleusGroup.get(i).getPercentageNucleusWithCorrectMigrationMode());
			resultsTable.addValue("% Nucleus wrong migration mode", nucleusGroup.get(i).getPercentageNucleusWrongMigrationMode());
			resultsTable.addValue("% Nucleus without migrationMode", nucleusGroup.get(i).getPercentageNucleusWithoutMigrationMode());

			resultsTable.addValue("% Nucleus correct segmented", nucleusGroup.get(i).getPercentageNucleusCorrectSegmented());
			resultsTable.addValue("% Nucleus oversegmented", nucleusGroup.get(i).getPercentageNucleusOverSegmented());
			resultsTable.addValue("% Nucleus undersegmented", nucleusGroup.get(i).getPercentageNucleusUnderSegmented());

			resultsTable.addValue("XCordinate center", nucleusGroup.get(i).getCenterXYZ()[0]);
			resultsTable.addValue("YCordinate center", nucleusGroup.get(i).getCenterXYZ()[1]);
			resultsTable.addValue("ZCordinate center", nucleusGroup.get(i).getCenterXYZ()[2]);

			resultsTable.addValue("Total Volume", nucleusGroup.get(i).getTotalVolume());

			resultsTable.addValue("MeanGray", nucleusGroup.get(i).getMeanGrayValue());
			// resultstable.addValue("Mean gray value focal plane", nucleus[k].getGrayValueSecondCannelOnFocalPlane());
			resultsTable.addValue("StdDev", nucleusGroup.get(i).getMeanSTDV());
			// resultsTable.addValue("Max", );
			// resultsTable.addValue("Min", );
			//// resultsTable.addValue("Median", );
			// resultsTable.addValue("Mode", );
			resultsTable.addValue("Skewness", nucleusGroup.get(i).getMeanSkewness());
			resultsTable.addValue("Kurtosis", nucleusGroup.get(i).getMeanKurtosis());
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

			resultsTable.addValue("Member Core", nucleusGroup.get(i).getCoreCellLabels());
			resultsTable.addValue("Member Cluster", nucleusGroup.get(i).getMultiCellLabels());
			resultsTable.addValue("Member Dual", nucleusGroup.get(i).getDualCellLabels());
			resultsTable.addValue("Member Signle", nucleusGroup.get(i).getSingleCellLabels());
			resultsTable.addValue("Member None", nucleusGroup.get(i).getNoneCellNames());
		}

		return resultsTable;
	}

}
