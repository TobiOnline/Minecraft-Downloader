package net.tpc.mcdownloader.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import net.tpc.mcdownloader.MinecraftDownloader;
import net.tpc.mcdownloader.listener.ProgressChangedListener;
import net.tpc.mcdownloader.listener.StoppedListener;

public class DownloaderWindow extends JFrame implements ActionListener, WindowListener,
		StoppedListener, ProgressChangedListener {

	private static final long	serialVersionUID	= 3428150718495407844L;

	private MinecraftDownloader	dloader				= null;

	private JProgressBar		progress			= null;
	private JButton				startButton			= null;

	private Container			outContainer		= null;
	private JButton				chooseOutputButton	= null;
	private JTextField			outputFolder		= null;

	private boolean				downloading;

	public DownloaderWindow() {
		super("Minecraft Downloader");

		this.dloader = new MinecraftDownloader();
		this.dloader.addProgressListener(this);
		this.dloader.addStoppedListener(this);

		this.setLayout(new BorderLayout());

		this.outContainer = new Container();
		this.outContainer.setLayout(new BorderLayout());

		this.outputFolder = new JTextField();
		this.outputFolder.setText(this.dloader.getOut().getAbsolutePath());
		this.outputFolder.setEditable(false);
		this.outContainer.add(this.outputFolder, BorderLayout.CENTER);

		this.chooseOutputButton = new JButton();
		this.chooseOutputButton.setText("Output folder ...");
		this.chooseOutputButton.setActionCommand("chooseOut");
		this.chooseOutputButton.addActionListener(this);
		this.outContainer.add(this.chooseOutputButton, BorderLayout.LINE_END);

		this.add(this.outContainer, BorderLayout.PAGE_START);

		this.startButton = new JButton();
		this.startButton.setText("Start");
		this.startButton.setActionCommand("Start");
		this.startButton.addActionListener(this);
		this.add(this.startButton, BorderLayout.CENTER);

		this.progress = new JProgressBar();
		this.add(this.progress, BorderLayout.AFTER_LAST_LINE);

		this.pack();
		this.setSize(450, this.getSize().height);
		this.setResizable(false);

		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		this.setBounds((screen.width - this.getWidth()) / 2,
				(screen.height - this.getHeight()) / 2, this.getWidth(), this.getHeight());

		this.addWindowListener(this);
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		this.progress.setStringPainted(true);
		this.progress.setMaximum(1000);
		this.progress.setMinimum(0);

		this.setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("Start")) {
			this.dloader.startDownloading();
			this.downloading = true;
			this.startButton.setText("Stop");
			this.startButton.setActionCommand("Stop");
			this.chooseOutputButton.setEnabled(false);
			this.progress.setString(null);

		} else if (e.getActionCommand().equals("Stop")) {
			this.dloader.setError(true);
			this.dloader.setException("Download stopped by user");
			this.dloader.stopDownloading();
			this.startButton.setText("Stopping ...");
			this.startButton.setActionCommand("stopping");
			this.progress.setValue(0);
			this.progress.setString("Download stopped by user");

		} else if (e.getActionCommand().equals("chooseOut")) {
			JFileChooser fchooser = new JFileChooser();
			fchooser.setDialogTitle("Choose the output directory ...");
			fchooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			fchooser.setAcceptAllFileFilterUsed(false);
			if (fchooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				this.dloader.setOut(fchooser.getSelectedFile());
				this.outputFolder.setText(fchooser.getSelectedFile().getAbsolutePath());
			}
		}
	}

	@Override
	public void onProgressChanged(String value, float f) {
		if (f == -1.0f) {
			this.progress.setString(value);
			this.progress.setValue(0);
		} else {
			this.progress.setString(value + " - " + (Math.floor(f * 1000.0f) / 10) + "%");
			this.progress.setValue((int) (f * 1000.0f));
		}
	}

	@Override
	public void onStopped() {
		this.downloading = false;

		this.progress.setValue(0);
		if (this.dloader.hadError()) {
			this.progress.setString(this.dloader.getException());
		} else {
			this.progress.setString("Finished!");
		}

		File out = this.dloader.getOut();

		this.dloader = new MinecraftDownloader();
		this.dloader.addProgressListener(this);
		this.dloader.addStoppedListener(this);
		this.dloader.setOut(out);

		this.chooseOutputButton.setEnabled(true);
		this.startButton.setText("Start");
		this.startButton.setActionCommand("Start");

	}

	@Override
	public void windowActivated(WindowEvent e) {
	}

	@Override
	public void windowClosed(WindowEvent e) {
	}

	@Override
	public void windowClosing(WindowEvent e) {
		if (this.downloading == false) {
			this.setVisible(false);
			this.dispose();
		} else {
			this.dloader.stopDownloading();
			while (!this.dloader.isStopped()) {
			}
			this.setVisible(false);
			this.dispose();
		}
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowOpened(WindowEvent e) {
	}
}
