package featureextractor.measurements;

import java.util.ArrayList;

import data.Cell3D;
import ij.ImagePlus;
import ij.measure.Calibration;
import mcib3d.image3d.ImageInt;
import mcib3d.image3d.ImageLabeller;
import mcib_plugins.analysis.simpleMeasure;

/**
 * Class Suite_3D is a edited version of the 3D ImageJ Suite plugins. In this version the following plugins were combined: - 3D Intensity measure - 3D Geometric measure - 3D Shape Measure
 *
 * http://imagejdocu.tudor.lu/doku.php?id=plugin:stacks:3d_ij_suite:start
 *
 * @author Esther
 *
 */
class MCIB3DMeasurements
{
	/**
	 * Method getAreaPixels calculate the areaPixels and add to the nucleus
	 *
	 * @param aLables
	 */
	private static void getAreaPixels(final Cell3D[] aCells, final ArrayList<double[]> aResBase)
	{
		for (int i = 0; i < aCells.length; i++)
		{
			aCells[i].getNucleus().getMeasurements().setAreaPixels(aResBase.get(i)[3]);
		}
	}


	/**
	 * Method getAreaUnit calculate the areaUnits and add to the nucleus
	 *
	 * @param aLables
	 */
	private static void getAreaUnit(final Cell3D[] aCells, final ArrayList<double[]> aResBase)
	{
		for (int i = 0; i < aCells.length; i++)
		{
			aCells[i].getNucleus().getMeasurements().setAreaUnit(aResBase.get(i)[4]);
		}
	}


	/**
	 * Method getCompactness calculate the compactness and add to the nucleus
	 *
	 * @param aLables
	 */
	private static void getCompactness(final Cell3D[] aCells, final ArrayList<double[]> aRes)
	{
		for (int i = 0; i < aCells.length; i++)
		{
			aCells[i].getNucleus().getMeasurements().setCompactness(aRes.get(i)[1]);
		}
	}


	/**
	 * Method getElongatio calculate the elongatio and add to the nucleus
	 *
	 * @param aLables
	 */
	private static void getElongatio(final Cell3D[] aCells, final ArrayList<double[]> aRes)
	{

		for (int i = 0; i < aCells.length; i++)
		{
			aCells[i].getNucleus().getMeasurements().setElongatio(aRes.get(i)[3]);
		}
	}


	/**
	 * Method getFlatness calculate the flatness and add to the nucleus
	 *
	 * @param aLables
	 */
	private static void getFlatness(final Cell3D[] aCells, final ArrayList<double[]> aRes)
	{
		for (int i = 0; i < aCells.length; i++)
		{
			aCells[i].getNucleus().getMeasurements().setFlatness(aRes.get(i)[4]);
		}
	}


	/**
	 * Method getGraySTDValue calculate the graySTDValue and add to the nucleus
	 *
	 * @param aLables
	 */
	private static void getGraySTDValue(final Cell3D[] aCells, final ArrayList<double[]> aResIntens)
	{
		for (int i = 0; i < aCells.length; i++)
		{
			aCells[i].getNucleus().getMeasurements().setStandardDeviation3D(aResIntens.get(i)[1]);
		}
	}


	/**
	 * Method getIntegratedDensity calculate the integratedDensity and add to the nucleus
	 *
	 * @param aLables
	 */
	private static void getIntegratedDensity(final Cell3D[] aCells, final ArrayList<double[]> aResIntens)
	{
		for (int i = 0; i < aCells.length; i++)
		{
			aCells[i].getNucleus().getMeasurements().setIntegratedDensity3D(aResIntens.get(i)[4]);
		}
	}


	/**
	 * Method getMaxGrayValue calculate the maxGrayValue and add to the nucleus
	 *
	 * @param aLables
	 */
	private static void getMaxGrayValue(final Cell3D[] aCells, final ArrayList<double[]> aResIntens)
	{
		for (int i = 0; i < aCells.length; i++)
		{
			aCells[i].getNucleus().getMeasurements().setMaximum3D(aResIntens.get(i)[3]);
		}
	}


	/**
	 * Method getMeanGrayValue calculate the meanGrayValue and add to the nucleus
	 *
	 * @param aLables
	 */
	private static void getMeanGrayValue(final Cell3D[] aCells, final ArrayList<double[]> aResIntens)
	{
		for (int i = 0; i < aCells.length; i++)
		{
			aCells[i].getNucleus().getMeasurements().setMeanIntensity3D(aResIntens.get(i)[0]);
		}
	}


	/**
	 * Method getMinGrayValue calculate the minGrayValue and add to the nucleus
	 *
	 * @param aLables
	 */
	private static void getMinGrayValue(final Cell3D[] aCells, final ArrayList<double[]> aResIntens)
	{
		for (int i = 0; i < aCells.length; i++)
		{
			aCells[i].getNucleus().getMeasurements().setMinimum3D(aResIntens.get(i)[2]);
		}
	}


	/**
	 * Method getSpareness calculate the spareness and add to the nucleus
	 *
	 * @param aLables
	 */
	private static void getSpareness(final Cell3D[] aCells, final ArrayList<double[]> aRes)
	{
		for (int i = 0; i < aCells.length; i++)
		{
			aCells[i].getNucleus().getMeasurements().setSpareness(aRes.get(i)[5]);
		}
	}


	/**
	 * Method getSphericity calculate the sphericity and add to the nucleus
	 *
	 * @param aLables
	 */
	private static void getSphericity(final Cell3D[] aCells, final ArrayList<double[]> aRes)
	{
		for (int i = 0; i < aCells.length; i++)
		{
			aCells[i].getNucleus().getMeasurements().setSphericity(aRes.get(i)[2]);
		}
	}


	/**
	 * Method getVolumePixels calculate the volumePixels and add to the nucleus
	 *
	 * @param aLables
	 */
	private static void getVolumePixels(final Cell3D[] aCells, final ArrayList<double[]> aResBase)
	{
		for (int i = 0; i < aCells.length; i++)
		{
			aCells[i].getNucleus().getMeasurements().setVolumePixels(aResBase.get(i)[1]);
		}
	}


	/**
	 * Method getVolumeUnit calculate the volumeUnits and add to the nucleus
	 *
	 * @param aLables
	 */
	private static void getVolumeUnit(final Cell3D[] aCells, final ArrayList<double[]> aResBase)
	{
		for (int i = 0; i < aCells.length; i++)
		{
			aCells[i].getNucleus().getMeasurements().setVolumeUnit(aResBase.get(i)[2]);
		}
	}


	public static void setMeasurements(final Cell3D[] aCells, final boolean[] aMeasurementChoice, final ImagePlus aOriginalImage, final ImagePlus aLabelImage)
	{
		final Calibration cal = aOriginalImage.getCalibration();
		// Create a scaled image
		final ImagePlus labeldImageScaled = aLabelImage.duplicate();
		labeldImageScaled.setCalibration(cal);

		final ImageInt img = ImageInt.wrap(labeldImageScaled);
		ImagePlus seg;
		if (img.isBinary(0))
		{
			final ImageLabeller label = new ImageLabeller();
			seg = label.getLabels(img).getImagePlus();
			seg.show("Labels");
		}
		else
		{
			seg = labeldImageScaled;
		}
		final simpleMeasure mes = new simpleMeasure(seg);

		final ArrayList<double[]> resBase = mes.getMeasuresBase();
		final ArrayList<double[]> res = mes.getMeasuresShape();
		final ArrayList<double[]> resIntens = mes.getMeasuresStats(aOriginalImage);

		if (aMeasurementChoice[0])
		{
			getMeanGrayValue(aCells, resIntens);
		}
		if (aMeasurementChoice[1])
		{
			getGraySTDValue(aCells, resIntens);
		}
		if (aMeasurementChoice[2])
		{
			getMinGrayValue(aCells, resIntens);
		}
		if (aMeasurementChoice[3])
		{
			getMaxGrayValue(aCells, resIntens);
		}
		if (aMeasurementChoice[4])
		{
			getIntegratedDensity(aCells, resIntens);
		}
		if (aMeasurementChoice[5])
		{
			getVolumePixels(aCells, resBase);
		}
		if (aMeasurementChoice[6])
		{
			getVolumeUnit(aCells, resBase);
		}
		if (aMeasurementChoice[7])
		{
			getAreaPixels(aCells, resBase);
		}
		if (aMeasurementChoice[8])
		{
			getAreaUnit(aCells, resBase);
		}
		if (aMeasurementChoice[9])
		{
			getCompactness(aCells, res);
		}
		if (aMeasurementChoice[10])
		{
			getSphericity(aCells, res);
		}
		if (aMeasurementChoice[11])
		{
			getElongatio(aCells, res);
		}
		if (aMeasurementChoice[12])
		{
			getFlatness(aCells, res);
		}
		if (aMeasurementChoice[13])
		{
			getSpareness(aCells, res);
		}

		labeldImageScaled.changes = false;
		labeldImageScaled.close();
	}
}
