package multivio.org.dfst;

import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import multivio.org.communication.ServerDocument;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MetsDFGParser implements ParserInterface {

	// private String parserId;
	private CoreDocumentModel cdm;
	private int localId;
	private int pageNumber;

	public MetsDFGParser() {
		// this.parserId = "DublinCore";
		this.cdm = new CoreDocumentModel();
		this.localId = 0;
		this.pageNumber = 0;
	}

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
		return pageNumber;
	}

	public CoreDocumentModel parseDocument(Document doc) {
		// retreive structure map
		NodeList structureMap = doc.getElementsByTagName("METS:structMap");
		int nbOfStruct = structureMap.getLength();
		Node logicalStruct = null, physicalStruct = null, files = null, links = null;
		// dfg-viewer need to have 2 structure map
		if (nbOfStruct != 2)
			System.out.println("Invalid Mets document");
		else {
			// save logical-physical Structure, files (DEFAULT), links Structure
			for (int j = 0; j < nbOfStruct; j++) {
				Node oneStruct = structureMap.item(j);
				NamedNodeMap attrs = oneStruct.getAttributes();
				for (int i = 0; i < attrs.getLength(); i++) {
					Attr attribute = (Attr) attrs.item(i);
					if (attribute.getName().equals("TYPE")) {
						if (attribute.getNodeValue().equals("LOGICAL")) {
							logicalStruct = oneStruct;
						}
						if (attribute.getNodeValue().equals("PHYSICAL")) {
							physicalStruct = oneStruct;
						}
					}
				}
			}
			// save list of files
			NodeList fileGroup = doc.getElementsByTagName("METS:fileGrp");
			// System.out.println("fileGrp nb : "+ fileGroup.getLength());
			for (int i = 0; i < fileGroup.getLength(); i++) {
				Node oneFileGroup = fileGroup.item(i);
				NamedNodeMap fileGrAttr = oneFileGroup.getAttributes();
				for (int j = 0; j < fileGrAttr.getLength(); j++) {
					Attr useAttr = (Attr) fileGrAttr.item(j);
					if (useAttr.getNodeName().equals("USE")
							& useAttr.getNodeValue().equals("DEFAULT")) {
						files = oneFileGroup;
						i = fileGroup.getLength();
						j = fileGrAttr.getLength();
					}
				}
			}
			links = doc.getElementsByTagName("METS:structLink").item(0);
		}
		Node firstChild = logicalStruct.getFirstChild();
		NamedNodeMap attrs = firstChild.getAttributes();
		String modsId = "";
		// first node of logical structure contains link to descriptive metadata
		// (MODS)
		for (int i = 0; i < attrs.getLength(); i++) {
			Attr attribute = (Attr) attrs.item(i);
			if (attribute.getNodeName().equals("DMDID")) {
				modsId = attribute.getNodeValue();
			}
		}
		NodeList descriptiveDataNodes = doc.getElementsByTagName("METS:dmdSec");
		int nb = descriptiveDataNodes.getLength();
		Document modsPart = null;
		// save as a new document the node that contains MODS data
		try {
			modsPart = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Record root = null;
		for (int j = 0; j < nb; j++) {
			Node temp = descriptiveDataNodes.item(j);
			NamedNodeMap attributes = temp.getAttributes();
			for (int i = 0; i < attributes.getLength(); i++) {
				Attr attribute = (Attr) attributes.item(i);
				if (attribute.getName().equals("ID")
						&& attribute.getValue().equals(modsId)) {
					modsPart.appendChild(modsPart.importNode(temp, true));
					String recId = this.generateID();
					ModsParser mods = new ModsParser();
					root = mods.parseNode(modsPart, recId);
					i = attributes.getLength();
					j = nb;
				}
			}
		}
		// get logical structure
		NodeList logics = logicalStruct.getFirstChild().getChildNodes();
		// create Records for logical node
		LinkedList<String> parentId = new LinkedList<String>();
		parentId.add("undefined");
		LinkedList<String> children = new LinkedList<String>();
		Record leafNode = null;
		String previousNumber = root.getId();
		// System.out.println("length de logics "+logics.getLength());
		for (int i = 0; i < logics.getLength(); i++) {
			Node oneLogic = logics.item(i);
			Record labelNode = new Record(this.generateID());
			if (i == 0) {
				root.setNextId(labelNode.getId());
			}
			parentId = new LinkedList<String>();
			parentId.add(root.getId());
			labelNode.setParentId(parentId);
			children.add(labelNode.getId());
			labelNode.setPreviousId(previousNumber);
			previousNumber = labelNode.getId();
			String logicalId = "";
			NamedNodeMap attributes = oneLogic.getAttributes();
			logicalId = attributes.getNamedItem("ID").getNodeValue();
			labelNode.setLabel(attributes.getNamedItem("LABEL").getNodeValue());
			LinkedList<String> listOffilesId = new LinkedList<String>();
			NodeList smLinks = links.getChildNodes();
			// retreive files link to this logical Node
			for (int k = 0; k < smLinks.getLength(); k++) {
				Node oneSmLink = smLinks.item(k);
				NamedNodeMap smAttr = oneSmLink.getAttributes();
				if (smAttr.getNamedItem("xlink:from").getNodeValue().equals(
						logicalId)) {
					listOffilesId.add(smAttr.getNamedItem("xlink:to")
							.getNodeValue());
				}
			}

			LinkedList<String> childs = new LinkedList<String>();
			NodeList divs = physicalStruct.getFirstChild().getChildNodes();
			// create Records for each files (leaf node)
			for (int l = 0; l < listOffilesId.size(); l++) {
				String physicalId = listOffilesId.get(l);
				String fileId = "";
				String recordId = null;
				if (leafNode != null) {
					if (l == 0) {
						leafNode.setNextId(labelNode.getId());
					} else {
						recordId = generateID();
						leafNode.setNextId(recordId);
					}
					this.cdm.addRecord(leafNode);
				}
				if (recordId == null) {
					leafNode = new Record(generateID());
				} else {
					leafNode = new Record(recordId);
				}
				parentId = new LinkedList<String>();
				parentId.add(labelNode.getId());
				leafNode.setParentId(parentId);
				leafNode.setPreviousId(previousNumber);
				previousNumber = leafNode.getId();
				if (l == 0) {
					labelNode.setNextId(leafNode.getId());
				}
				childs.add(leafNode.getId());
				for (int t = 0; t < divs.getLength(); t++) {
					Node div = divs.item(t);
					NamedNodeMap divAttr = div.getAttributes();
					if (divAttr.getNamedItem("ID").getNodeValue().equals(
							physicalId)) {
						leafNode.setSequenceNumber(Integer.parseInt(divAttr
								.getNamedItem("ORDER").getNodeValue()));
						NamedNodeMap fptrAttr = div.getFirstChild()
								.getAttributes();
						fileId = fptrAttr.getNamedItem("FILEID").getNodeValue();
						// System.out.println("FileId = "+ fileId);
						t = divs.getLength();
					}
				}
				NodeList filesList = files.getChildNodes();
				for (int h = 0; h < filesList.getLength(); h++) {
					Node oneFile = filesList.item(h);
					// System.out.println("oneFile "+ oneFile.getNodeName());
					NamedNodeMap fileAttr = oneFile.getAttributes();
					if (fileAttr.getNamedItem("ID").getNodeValue().equals(
							fileId)) {
						Node FLocat = oneFile.getFirstChild();
						NamedNodeMap FlocatAttr = FLocat.getAttributes();
						leafNode.setDefaultUrl(FlocatAttr.getNamedItem(
								"xlink:href").getNodeValue());
						// System.out.println("adr = "+FlocatAttr.getNamedItem("xlink:href").getNodeValue());
						h = filesList.getLength();
					}
				}

				if (l == listOffilesId.size() - 1
						&& i == logics.getLength() - 1) {
					leafNode.setNextId("undefined");
					this.cdm.addRecord(leafNode);
				}
			}
			labelNode.setChildren(childs);
			this.cdm.addRecord(labelNode);
		}
		root.setChildren(children);
		this.cdm.addRecord(root);
		return this.cdm;
	}

}
