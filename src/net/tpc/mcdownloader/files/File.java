package net.tpc.mcdownloader.files;

public class File extends Entry {

	private int	length	= 0;

	public File(String name, int length) {
		super(name);
		this.length = length;
	}

	public int getLength() {
		return this.length;
	}
}
