
import java.io.File;

import javax.swing.JFileChooser;

import ij.IJ;
import ij.Prefs;
import ij.plugin.PlugIn;

/**
 * The plugin Create_Marker_Image_3D is able to create marker images with different point detection methods in 2D and 3D. The plugin recognize the type of image and switch to 2D or 3D. For the 3D part the options are: Manual points, Manual points
 * translate and Mexican Hat. For the 2D part the options are: Manual points, mexican Hat, distancetransform and watershedpoints. - Mexican Hat: Fiji -> Plugin -> LoG3D (Daniel Sage) - Maxima finder: Fiji -> Plugin -> 3D -> 3D Maxima finder (code is
 * adapted)
 *
 * @author Esther
 *
 */
public abstract class Base_Segmentation_3D_Plugin implements PlugIn
{
	protected final static String NONE = "None";


	/**
	 * Ask the user to select the input or output directory
	 *
	 * @param aDialogTitle
	 *            The title of the file chooser
	 * @param aPreferenceKey
	 *            The key for getting the stored preference
	 * @param aIsDirectory
	 *            Are we retrieving a directory
	 * @return the directory which is selected by the user as input or output file
	 */
	protected File fileLoadOrFileSave(final String aDialogTitle, final String aPreferenceKey, final boolean aIsDirectory)
	{
		final String prefInputFile = Prefs.get(aPreferenceKey, null);
		final JFileChooser fileChooser = new JFileChooser(prefInputFile);
		fileChooser.setDialogTitle(aDialogTitle);
		if (aIsDirectory)
		{
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		}

		final int number = fileChooser.showOpenDialog(IJ.getInstance());
		if (number == JFileChooser.CANCEL_OPTION)
		{
			return null;
		}

		final File file = fileChooser.getSelectedFile();
		Prefs.set(aPreferenceKey, aIsDirectory ? file.getParent() : file.getPath());

		return file;
	}


	@Override
	public abstract void run(final String arg);
}