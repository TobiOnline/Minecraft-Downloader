package net.tpc.mcdownloader;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import net.tpc.mcdownloader.gui.DownloaderWindow;

public class Program {

	public static String getOsName() {
		String os = "";
		if (System.getProperty("os.name").toLowerCase().indexOf("windows") > -1) {
			os = "windows";
		} else if (System.getProperty("os.name").toLowerCase().indexOf("linux") > -1) {
			os = "linux";
		} else if (System.getProperty("os.name").toLowerCase().indexOf("mac") > -1) {
			os = "mac";
		} else if (System.getProperty("os.name").toLowerCase().indexOf("solaris") > -1) {
			os = "solaris";
		}
		return os;
	}

	public static void main(String[] args) {

		// Set System-Dependend Look And Feel
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
		}

		new DownloaderWindow();
	}
}
