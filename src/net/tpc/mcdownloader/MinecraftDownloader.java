package net.tpc.mcdownloader;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.tpc.mcdownloader.files.Entry;
import net.tpc.mcdownloader.files.Folder;
import net.tpc.mcdownloader.listener.ProgressChangedListener;
import net.tpc.mcdownloader.listener.StoppedListener;
import net.tpc.mcdownloader.xml.XMLTag;
import net.tpc.mcdownloader.xml.XMLTagReader;

public class MinecraftDownloader extends Thread {

	public static boolean deleteDir(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (String child : children) {
				boolean success = MinecraftDownloader.deleteDir(new File(dir, child));
				if (!success) {
					return false;
				}
			}
		}
		return dir.delete();
	}

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

	private File								out						= new File(
																				System.getenv("AppData"),
																				".minecraft/");
	private Boolean								downloading				= false;
	private Boolean								stop					= false;

	private ArrayList<ProgressChangedListener>	progressChangedListener	= new ArrayList<ProgressChangedListener>();
	private ArrayList<StoppedListener>			stoppedListener			= new ArrayList<StoppedListener>();
	private String								host					= "s3.amazonaws.com";

	private String								hostPath				= "MinecraftDownload/";
	private boolean								error					= false;
	private static HashMap<String, String>		files					= new HashMap<String, String>();

	private ArrayList<Entry>					allFiles				= new ArrayList<Entry>();

	private static String[]						natives					= new String[] {
			"linux_natives.jar", "macosx_natives.jar", "solaris_natives.jar",
			"windows_natives.jar",										};

	private String								exception				= null;

	static {
		// Launcher
		MinecraftDownloader.files.put("launcher/Minecraft.exe", "Minecraft.exe");
		MinecraftDownloader.files.put("launcher/minecraft.jar", "minecraft.jar");
		MinecraftDownloader.files.put("launcher/Minecraft.zip", "Minecraft.zip");

		// Minecraft
		MinecraftDownloader.files.put("minecraft.jar", "bin/minecraft.jar");

		// Minecraft Server
		MinecraftDownloader.files.put("launcher/minecraft_server.jar", "minecraft_server.jar");

		// Library
		MinecraftDownloader.files.put("lwjgl.jar", "bin/lwjgl.jar");
		MinecraftDownloader.files.put("jinput.jar", "bin/jinput.jar");
		MinecraftDownloader.files.put("lwjgl_util.jar", "bin/lwjgl_util.jar");
		MinecraftDownloader.files.put("jutils.jar", "bin/jutils.jar");
	}

	private boolean aborted() {
		if (!this.shouldContinue()) {
			this.downloading = false;
			this.onStopped();
			return true;
		}
		return false;
	}

	public void addProgressListener(ProgressChangedListener listener) {
		this.progressChangedListener.add(listener);
	}

	public void addStoppedListener(StoppedListener listener) {
		this.stoppedListener.add(listener);
	}

	private void downloadFile(String fname, String outName) {
		File file = new File(this.out.getAbsolutePath(), outName);
		this.onProgressChanged("Downloading " + file.getName(), -1.0f);

		if (file.exists() && (file.length() == this.getFileSize(fname))) {
			this.onProgressChanged(fname + " already exists", -1.0f);
			return;
		}

		File parentFolder = file.getParentFile();
		if (!parentFolder.exists()) {
			parentFolder.mkdirs();
		}

		DataInputStream in = null;
		FileOutputStream fout = null;
		URLConnection connection = null;
		try {

			fout = new FileOutputStream(file);

			byte[] buffer = new byte[1024];

			URL url = new URL("http://" + this.host + "/" + this.hostPath
					+ fname.replace(" ", "%20"));
			connection = url.openConnection();
			connection.setDoInput(true);
			connection.setUseCaches(false);
			in = new DataInputStream(connection.getInputStream());

			int numread = 0;
			int fileSize = this.getFileSize(fname);

			while (!this.aborted()) {
				int read = in.read(buffer);
				if (read == -1) {
					break;
				}
				numread += read;
				this.onProgressChanged("Downloading " + file.getName(), (float) numread
						/ (float) fileSize);
				fout.write(buffer, 0, read);
			}

		} catch (IOException e) {
			this.error = true;
			this.exception = "Could not download file " + fname;
			this.aborted();
			return;
		} finally {

			try {
				in.close();
			} catch (IOException | NullPointerException e) {
			}

			try {
				fout.close();
			} catch (IOException | NullPointerException e) {
			}
		}

	}

	private void downloadFileList() {
		this.onProgressChanged("Downloading file list", -1.0f);

		DataInputStream in = null;
		String data = null;
		URLConnection connection = null;
		try {

			URL url = new URL("http://" + this.host + "/" + this.hostPath);
			connection = url.openConnection();
			connection.setDoInput(true);
			connection.setUseCaches(false);

			in = new DataInputStream(connection.getInputStream());

			StringBuilder inData = new StringBuilder();
			try {
				while (!this.aborted()) {
					inData.append((char) in.readByte());
				}
			} catch (EOFException e) {
			}
			data = inData.toString();
		} catch (IOException e) {
			this.error = true;
			this.exception = "Could not download resource list";
			this.aborted();
		} finally {
			try {
				in.close();
			} catch (IOException e) {
			}
		}

		try {
			this.onProgressChanged("Parsing the resource list", -1.0f);

			String contents = null;
			XMLTag key = null;
			XMLTag size = null;
			while ((data.indexOf("<Contents>") != -1) && (data.indexOf("</Contents>") != -1)
					&& !this.aborted()) {
				contents = data.substring(data.indexOf("<Contents>") + "<Contents>".length(),
						data.indexOf("</Contents>"));
				key = XMLTagReader.getXMLTag("Key", contents);
				size = XMLTagReader.getXMLTag("Size", contents);

				if ((size == null) || size.getContent().equals("") || size.getContent().equals("0")) {
					this.allFiles.add(new Folder(key.getContent()));
				} else {
					try {
						this.allFiles.add(new net.tpc.mcdownloader.files.File(key.getContent(),
								Integer.parseInt(size.getContent())));
					} catch (NumberFormatException e) {
					}
				}
				data = data.substring(data.indexOf("</Contents>") + "</Contents>".length());
			}
		} catch (StringIndexOutOfBoundsException e) {
			this.error = true;
			this.exception = "Could not parse resource list";
			this.aborted();
		}
	}

	public String getException() {
		return this.exception;
	}

	private int getFileSize(String fname) {
		for (Entry entry : this.allFiles) {
			if (entry.getName().equalsIgnoreCase(fname)) {
				if (entry instanceof Folder) {
					return 0;
				} else {
					return ((net.tpc.mcdownloader.files.File) entry).getLength();
				}
			}
		}
		return -1;
	}

	public File getOut() {
		return this.out;
	}

	public boolean hadError() {
		return this.error;
	}

	public boolean isStopped() {
		return !this.downloading;
	}

	private void onProgressChanged(String value, float f) {
		for (ProgressChangedListener listener : this.progressChangedListener) {
			if (listener != null) {
				listener.onProgressChanged(value, f);
			}
		}
	}

	private void onStopped() {
		for (StoppedListener listener : this.stoppedListener) {
			if (listener != null) {
				listener.onStopped();
			}
		}
	}

	public void removeProgressListener(ProgressChangedListener listener) {
		this.progressChangedListener.remove(listener);
	}

	public void removeStoppedListener(StoppedListener listener) {
		this.stoppedListener.remove(listener);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void run() {
		this.downloading = true;
		this.error = false;
		this.stop = false;

		this.downloadFileList();

		for (Map.Entry file : MinecraftDownloader.files.entrySet()) {
			if (this.aborted()) {
				return;
			}
			this.downloadFile(file.getKey().toString(), file.getValue().toString());
		}
		if (this.aborted()) {
			return;
		}

		for (String file : MinecraftDownloader.natives) {
			if (this.aborted()) {
				return;
			}
			this.downloadFile(file, "bin/natives/jar/" + file);
		}

		for (Entry file : this.allFiles) {
			if (this.aborted()) {
				return;
			}
			if (!file.getName().contains("resources") || (file instanceof Folder)) {
				continue;
			}
			this.downloadFile(file.getName(), file.getName());
		}

		if (MinecraftDownloader.getOsName().equals("")) {
			this.onProgressChanged("Unknown OS", -1.0f);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
		} else {
			JarExtractor extractor = new JarExtractor(new File(this.out.getAbsolutePath(),
					"bin/natives/jar/" + MinecraftDownloader.getOsName() + "_natives.jar"));
			for (ProgressChangedListener listener : this.progressChangedListener) {
				if (listener != null) {
					extractor.addProgressListener(listener);
				}
			}
			extractor.extract(new File(this.out.getAbsolutePath(), "bin/natives/"));
			MinecraftDownloader.deleteDir(new File(this.out.getAbsolutePath(),
					"bin/natives/META-INF/"));
		}
		this.onStopped();
		this.downloading = false;
	}

	public void setError(boolean error) {
		this.error = error;
	}

	public void setException(String exception) {
		this.exception = exception;
	}

	public void setOut(File selectedFile) {
		this.out = selectedFile;
	}

	private boolean shouldContinue() {
		return !this.error && this.downloading && !this.stop;
	}

	public void startDownloading() {
		if (this.downloading) {
			return;
		}
		this.downloading = true;
		this.error = false;
		this.stop = false;
		this.start();
	}

	public void stopDownloading() {
		this.stop = true;
	}
}
