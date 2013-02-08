package net.tpc.mcdownloader.xml;

public class XMLTagReader {
	public static XMLTag getXMLTag(String name, String data) {
		if ((data.indexOf("<" + name) == -1) || (data.indexOf(">", data.indexOf("<" + name)) == -1)) {
			return null;
		}
		boolean noCloseTag = false;
		if ((data.indexOf("/>", data.indexOf("<" + name)) == -1)) {
			if (data.indexOf("</" + name + ">") == -1) {
				return null;
			}
		} else {
			noCloseTag = true;
		}
		XMLTag tag = new XMLTag(name);
		String properties = "";
		if (noCloseTag) {
			tag.setContent("");
			properties = data.substring(data.indexOf("<" + name), data.indexOf("/>") - 1);
		} else {
			tag.setContent(data.substring(
					data.indexOf(">", data.indexOf("<" + name)) + ">".length(),
					data.indexOf("</" + name + ">")));
			properties = data.substring(data.indexOf("<" + name) + ("<" + name).length(),
					data.indexOf(">", data.indexOf("<" + name) + ("<" + name).length()));
		}
		properties.trim();
		String propName = null;
		String propValue = null;
		while ((properties.length() > 0) && (properties.indexOf("=") != -1)) {
			propName = properties.substring(0, data.indexOf("=") - 1).trim();
			properties = properties.substring(properties.indexOf("=") + 1).trim();
			if (properties.startsWith("\"")) {
				if (properties.indexOf("\"", 1) != -1) {
					propValue = properties.substring(1, properties.indexOf("\"") - 1);
					properties = properties.substring(properties.indexOf("\"") + 1).trim();
				} else {
					break;
				}
			} else {
				if (properties.indexOf(" ") != -1) {
					propValue = properties.substring(1, properties.indexOf(" ") - 1);
					properties = properties.substring(properties.indexOf(" ")).trim();
				} else {
					propValue = properties;
					properties = null;
				}
			}
			tag.addProperty(propName, propValue);
		}
		return tag;
	}
}
