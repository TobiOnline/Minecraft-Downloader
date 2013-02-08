package net.tpc.mcdownloader.files;

public abstract class Entry {
	private String	name	= null;

	public Entry(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}
}
