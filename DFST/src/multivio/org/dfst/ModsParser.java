package multivio.org.dfst;

import java.util.HashMap;
import java.util.LinkedList;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ModsParser implements ParserInterface {

	// private String parserId;
	private CoreDocumentModel cdm;
	private int localId;
	private int pageNumber;
	private String modsWriting = null;

	public ModsParser() {
		// this.parserId = "DublinCore";
		this.cdm = new CoreDocumentModel();
		this.localId = 0;
		this.pageNumber = 0;
	}

	/*
	 * public ModsParser(String sequenceId){ this.cdm = new CoreDocumentModel();
	 * this.localId = Integer.parseInt(sequenceId); }
	 */

	public String generateID() {
		String recordId;
		localId++;
		if (localId < 10)
			recordId = "n0000" + Integer.toString(localId);
		else if (9 < localId && localId < 100)
			recordId = "n000" + Integer.toString(localId);
		else if (99 < localId && localId < 1000)
			recordId = "n00" + Integer.toString(localId);
		else if (999 < localId && localId < 10000)
			recordId = "n0" + Integer.toString(localId);
		else if (9999 < localId && localId < 100000)
			recordId = "n" + Integer.toString(localId);
		else
			recordId = "undefined";
		return recordId;
	}

	public int generateSequenceNumber() {
		pageNumber++;
		System.out.println("pageNumber " + pageNumber);
		return pageNumber;
	}

	public CoreDocumentModel parseDocument(Document doc) {
		System.out.println("Mods parser");

		// TODO Auto-generated method stub
		return null;
	}

	public Record parseNode(Document doc, String nodeId) {
		System.out.println("mods = " + doc.getDocumentElement().getFirstChild().getFirstChild().getFirstChild().getNodeName() );
		this.modsWriting = doc.getDocumentElement().getFirstChild().getFirstChild().getFirstChild().getNodeName().substring(0, 5) ;
		Record rootNode = new Record(nodeId);
		//System.out.println("dedans parseNode " + doc.getNodeName());
		//System.out.println("length " + doc.getChildNodes().getLength());
		HashMap<String, Object> descriptiveMetadata = new HashMap<String, Object>();

		descriptiveMetadata.put("title", doc.getElementsByTagName(this.modsWriting+"title")
				.item(0).getFirstChild().getTextContent());
		descriptiveMetadata.put("language", doc.getElementsByTagName(
				this.modsWriting+"languageTerm").item(0).getTextContent());
		NodeList auts = doc.getElementsByTagName(this.modsWriting+"name");
		for (int i = 0; i < auts.getLength(); i++) {
			Node name = auts.item(i);
			NodeList childs = name.getChildNodes();
			String key = "";
			String value = "";
			for (int j = 0; j < childs.getLength(); j++) {
				Node temp = childs.item(j);
				if (temp.getNodeName().equals(this.modsWriting+"role")) {
					// key = temp.getFirstChild().getTextContent();
					key = "creator";
				}
				if (temp.getNodeName().equals(this.modsWriting+"namePart")) {
					value += " " + temp.getFirstChild().getTextContent();
				}
			}
			if (key != null & value != null) {
				descriptiveMetadata.put(key, value);
			}
		}
		rootNode.setMeta(descriptiveMetadata);
		LinkedList<String> parentId = new LinkedList<String>();
		parentId.add("undefined");
		rootNode.setParentId(parentId);
		rootNode.setPreviousId("undefined");
		rootNode.setLabel(doc.getElementsByTagName(this.modsWriting+"title").item(0)
				.getFirstChild().getTextContent());
		return rootNode;
	}
}
