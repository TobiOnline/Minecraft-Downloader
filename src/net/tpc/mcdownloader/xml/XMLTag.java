package net.tpc.mcdownloader.xml;

import java.util.HashMap;

public class XMLTag {

	private String					name		= null;
	private String					content		= "";
	private HashMap<String, String>	properties	= new HashMap<String, String>();

	public XMLTag(String name) {
		this.name = name;
	}

	public XMLTag(String name, String content) {
		this(name);
		this.content = content;
	}

	public void addProperty(String name, String value) {
		this.properties.put(name, value);
	}

	public String getContent() {
		return this.content;
	}

	public String getName() {
		return this.name;
	}

	public String getProperty(String name) {
		return this.properties.get(name);
	}

	public void setContent(String content) {
		this.content = content;
	}
}
