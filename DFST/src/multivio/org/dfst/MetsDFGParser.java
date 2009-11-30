package multivio.org.dfst;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import multivio.org.communication.ServerDocument;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class MetsDFGParser implements ParserInterface {

	// private String parserId;
	private CoreDocumentModel cdm;
	private int localId;
	//private int pageNumber;
	private Record lastRecord = null;
	private Record newRecord = null;
	private Node filesNode = null;
	private LinkedList<Node> files = null;
	private LinkedList<String> filesId = null;
	private Node links = null;
	private Node physicalStructure = null;
	private Node logicalStructure = null;
	private String metsWriting = null;

	public MetsDFGParser() {
		// this.parserId = "MetsDFGParser";
		this.cdm = new CoreDocumentModel();
		this.localId = 0;
		//this.pageNumber = 0;
	}

	/*
	 * Return a new unique ID
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

	/*
	 * Determine if the mets document is a single document or a complex document
	 * @see multivio.org.dfst.ParserInterface#parseDocument(org.w3c.dom.Document)
	 */
	public CoreDocumentModel parseDocument (Document doc) {
		//set metsWriting
		this.metsWriting = doc.getDocumentElement().getNodeName().substring(0, 5);
		if(!this.metsWriting.toLowerCase().equals("mets:")){
			System.out.println("not a mets document");
			if(this.metsWriting.indexOf("OAI") != -1){
				System.out.println("OAI");
				NodeList metadata = doc.getElementsByTagName("metadata");
				//System.out.println("length "+ metadata.getLength());
				this.metsWriting = metadata.item(0).getFirstChild().getNodeName().substring(0, 5);
			}
		}
		System.out.println("metsWriting = "+ this.metsWriting);
		// retreive structure map
		int nbOfStruct = doc.getElementsByTagName(this.metsWriting+"structMap").getLength();
		System.out.println("nbStruct = " + nbOfStruct);
		// dfg-viewer with 2 structure map is a single document
		// dfg-viewer with 1 logical structure map is a complex document
		if (nbOfStruct >  2 || nbOfStruct == 0){
			System.out.println("Invalid Mets document");
			return null;
		}
		else{
			if(nbOfStruct == 1){
				System.out.println("multi-level work");
				parseStructuralMets(doc);
			}
			else{
				System.out.println("single document");
				parseMetsFile(doc, null);
			}
		}
		return this.cdm;
	}
	
	/*
	 * Retreive metadata of the global mets document and load each child
	 * to parse it
	 */
	public void parseStructuralMets (Document doc) {
		NodeList structureMap = doc.getElementsByTagName(this.metsWriting+"structMap");
		if (structureMap.item(0).getAttributes().getNamedItem("TYPE").getNodeValue().equals("LOGICAL")) {
			this.logicalStructure = structureMap.item(0);
		}
		if (this.logicalStructure == null) {
			System.out.println("Invalid Mets document");
			return;
		}
		else{
			// create root record
			Record root = this.createRootRecord(doc.getElementsByTagName(this.metsWriting+"dmdSec"));
			setNextAndPrevious(root);
			LinkedList<String> children = new LinkedList<String>();
			NodeList composiesMets = this.logicalStructure.getFirstChild().getChildNodes();
			ServerDocument server = new ServerDocument();
			for (int i = 0; i < composiesMets.getLength(); i++) {
				Node oneFile = composiesMets.item(i);
				Node mptr = oneFile.getFirstChild();
				String fileUrl = mptr.getAttributes().getNamedItem("xlink:href").getNodeValue();
				System.out.println("fileURl = "+ fileUrl);
				
				Document newDoc = server.getMetadataDocument(fileUrl);
				children.add(parseMetsFile(newDoc, root));
			}
			root.setChildren(children);
			//root.setNextId(children.getFirst());
			if (this.cdm.getRecord(root.getId()) != null){
				root = this.cdm.getRecord(root.getId());
			}
			root.setChildren(children);
			this.cdm.addRecord(root);
			
			this.lastRecord.setNextId(this.newRecord.getId());
			this.newRecord.setPreviousId(this.lastRecord.getId());
			this.cdm.addRecord(this.lastRecord);
			this.newRecord.setNextId("undefined");
			this.cdm.addRecord(this.newRecord);
		}
	}
	
	/*
	 * Parse the mets document and save global information needed to create the cdm
	 */
	public String parseMetsFile (Document doc, Record root) {
		//Set<Node> listOfimages = new HashSet<Node>();  
		//retreive physical and logical Map
		NodeList structureMap = doc.getElementsByTagName(this.metsWriting+"structMap");
		for (int j = 0; j < structureMap.getLength(); j++) {
			Node oneStruct = structureMap.item(j);
			if (oneStruct.getAttributes().getNamedItem("TYPE").getNodeValue().equals("LOGICAL")) {
				this.logicalStructure = oneStruct;
			}
			if (oneStruct.getAttributes().getNamedItem("TYPE").getNodeValue().equals("PHYSICAL")) {
				this.physicalStructure = oneStruct;
			}
		}
		// save list of files and links
		NodeList fileGroup = doc.getElementsByTagName(this.metsWriting+"fileGrp");
		this.files = new LinkedList<Node>();
		//System.out.println("fileGrp nb : " + fileGroup.getLength());
		for (int i = 0; i < fileGroup.getLength(); i++) {
			Node oneFileGroup = fileGroup.item(i);
			if(oneFileGroup.getAttributes().getNamedItem("USE").getNodeValue().equals("DEFAULT")){
				this.filesNode = oneFileGroup;
				break;
			}
		}
		NodeList listOfFiles = this.filesNode.getChildNodes();	
		this.filesId = new LinkedList<String>();
		for (int j = 0; j < listOfFiles.getLength(); j++) {
			this.files.add(listOfFiles.item(j));
			this.filesId.add(listOfFiles.item(j).getAttributes().getNamedItem("ID").getNodeValue());
		}
		
		this.links = doc.getElementsByTagName(this.metsWriting+"structLink").item(0);
		System.out.println("nb of files = " + filesNode.getChildNodes().getLength() + " = " + files.size());

		if (root == null) {
			root = createRootRecord(doc.getElementsByTagName(this.metsWriting+"dmdSec"));
			setNextAndPrevious(root);
		}
		else {
			if(this.logicalStructure.getFirstChild().getAttributes().getNamedItem("ID") == null) {
				NodeList temp = this.logicalStructure.getFirstChild().getChildNodes();
				for (int j = 0; j < temp.getLength(); j++) {
					if(temp.item(j).getAttributes().getNamedItem("ID") == null) {
						this.logicalStructure.getFirstChild().removeChild(temp.item(j));
					}
				}
			}
			this.logicalStructure = this.logicalStructure.getFirstChild();
			root = createLabel(root, this.logicalStructure.getFirstChild());
		}
		//verify if some files are linked with the root Node
		String parentLogicalId = this.logicalStructure.getFirstChild().getAttributes().getNamedItem("ID").getNodeValue();
		LinkedList<String> childrenLogicalId = new LinkedList<String>();
		NodeList childrenNode = this.logicalStructure.getFirstChild().getChildNodes();
		for (int k = 0; k < childrenNode.getLength(); k++) {
			Node logicalID = childrenNode.item(k).getAttributes().getNamedItem("ID");
			if(logicalID != null){
				childrenLogicalId.add(logicalID.getNodeValue());
			}
		}
		//LinkedList<String> resultIds = getParentFileId(parentLogicalId, childrenLogicalId);
		LinkedList<String> temp = parentFiles(parentLogicalId, childrenLogicalId);
		if(!temp.isEmpty()){
			LinkedList<String> children = new LinkedList<String>();
			for ( int j = 0; j < temp.size(); j++){
				children.add(createLeaf(root, temp.get(j)));
			}
			//first children is null because there is no files
			children.removeFirst();
			root.setChildren(children);
		}
		createStructure(root, this.logicalStructure.getFirstChild());

		this.lastRecord.setNextId(this.newRecord.getId());
		this.newRecord.setPreviousId(this.lastRecord.getId());
		this.cdm.addRecord(this.lastRecord);
		this.newRecord.setNextId("undefined");
		this.cdm.addRecord(this.newRecord);
		return root.getId();
	}
	
	/*
	 * Create the global structure of the Mets document
	 */
	public void createStructure (Record parent, Node oneLogicalNode){
		NodeList logicalChildren = oneLogicalNode.getChildNodes();
		System.out.println("createLabel nb of child " + logicalChildren.getLength());
		LinkedList<String> parentId = new LinkedList<String>();
		parentId.add(parent.getId());
		//LinkedList<String> children = new LinkedList<String>();
		LinkedList<String> children = parent.getChildren();
		for (int i = 0; i < logicalChildren.getLength(); i++){
			Node oneChild = logicalChildren.item(i);
			if (oneChild.getAttributes().getNamedItem("ID") != null) {
				LinkedList<String>childs = new LinkedList<String>();
				Record oneRecord = createLabel(parent,oneChild);
				//setNextAndPrevious(oneRecord);
				//if oneChild hasChildNodes => there is another labelRecord
				if (oneChild.hasChildNodes()) {				
					//first find if this node has leaf (= files)
					System.out.println("label has children...");
					String parentLogicalId = oneChild.getAttributes().getNamedItem("ID").getNodeValue();
					LinkedList<String> childrenLogicalId = new LinkedList<String>();
					NodeList childrenNode = oneChild.getChildNodes();
					for (int k = 0; k < childrenNode.getLength(); k++) {
						childrenLogicalId.add(childrenNode.item(k).getAttributes().getNamedItem("ID").getNodeValue());
					}
					//LinkedList<String> resultIds = getParentFileId(parentLogicalId, childrenLogicalId);
					LinkedList<String> temp = parentFiles(parentLogicalId, childrenLogicalId);
					if(!temp.isEmpty()){
						System.out.println("label has leaf files and labels");
						for(int n = 0; n < temp.size(); n++){
							String leafId = createLeaf(oneRecord,temp.get(n));
							if (leafId != null){
								childs.add(leafId);
							}
						}
					}
					createStructure(oneRecord, oneChild);
					childs.addAll(oneRecord.getChildren());
				}
				else {
					System.out.println("only leaf children");
					String logicalId = oneChild.getAttributes().getNamedItem("ID").getNodeValue();
					LinkedList<String>listOfFilesId = new LinkedList<String>();
					NodeList smLinks = this.links.getChildNodes();
					for (int l = 0; l < smLinks.getLength(); l++) {
						Node oneSmLink = smLinks.item(l);
						if (oneSmLink.getAttributes().getNamedItem("xlink:from")
							.getNodeValue().equals(logicalId)) {
							listOfFilesId.add(oneSmLink.getAttributes()
								.getNamedItem("xlink:to").getNodeValue());
						}
					}
					
					for (int j = 0; j <listOfFilesId.size(); j++) {
						String leafId = createLeaf(oneRecord,listOfFilesId.get(j));
						if (leafId != null){
							childs.add(leafId);
						}
					}
				}
				if(this.cdm.getRecord(oneRecord.getId()) != null){
					Record temp = this.cdm.getRecord(oneRecord.getId());
					temp.setChildren(childs);
					this.cdm.addRecord(temp);
				}
				else{
					oneRecord.setChildren(childs);
					this.cdm.addRecord(oneRecord);
				}
				children.add(oneRecord.getId());
			}
		}
		if (this.cdm.getRecord(parent.getId()) != null) {
			Record temp = this.cdm.getRecord(parent.getId());
			temp.setChildren(children);
			this.cdm.addRecord(temp);
		}
		else{
			parent.setChildren(children);
			this.cdm.addRecord(parent);
		}
	}
	
	/*
	 * Return the Id of a record of type Leaf. A record of type leaf has a sequenceNumber 
	 * and a default url. 
	 */
	public String createLeaf (Record parent, String physicalId) {
		NodeList divs = this.physicalStructure.getFirstChild().getChildNodes();
		String fileId = null;
		String defaultUrl = null;
		int order = -1;
		boolean alreadyExist = false;
		for (int t = 0; t < divs.getLength(); t++) {
			Node div = divs.item(t);
			if (div.getAttributes().getNamedItem("ID").getNodeValue().equals(physicalId)) {
				order = (Integer.parseInt(div.getAttributes().getNamedItem("ORDER").getNodeValue()));
				for (int j = 0; j < div.getChildNodes().getLength(); j++) {
					Node oneChild = div.getChildNodes().item(j);
					String tempId = oneChild.getAttributes().getNamedItem("FILEID").getNodeValue();
					if (this.filesId.contains(tempId)) {
						fileId = tempId;
						break;
					}
				}
				break;
			}
		}
		// no files => stop here
		if (fileId == null){
			return null;
		}
		NodeList filesList = this.filesNode.getChildNodes();
		for (int p = 0; p < filesList.getLength(); p++) {
			Node oneFile = filesList.item(p);
			if (oneFile.getAttributes().getNamedItem("ID").getNodeValue().equals(fileId)) {
				Node Flocat = oneFile.getFirstChild();
				defaultUrl = (Flocat.getAttributes().getNamedItem("xlink:href").getNodeValue());
				alreadyExist = !this.files.remove(oneFile);
				break;
			}
		}
		if(alreadyExist){
			String recordId = retreiveRecord(parent.getId(),defaultUrl);
			return recordId;
		}
		LinkedList<String> parentId = new LinkedList<String>();
		parentId.add(parent.getId());
		Record oneRecord = new Record(generateID());
		oneRecord.setParentId(parentId);
		oneRecord.setSequenceNumber(order);
		oneRecord.setDefaultUrl(defaultUrl);
		setNextAndPrevious(oneRecord);
		return oneRecord.getId();
	}
	
	/*
	 * Return a record of type label has a label that comes from logical structural map node
	 */
	public Record createLabel (Record parent, Node logicalNode) {
		Record oneRecord = new Record(this.generateID());
		LinkedList<String> parentId = new LinkedList<String>();
		parentId.add(parent.getId());
		oneRecord.setParentId(parentId);
		if (logicalNode.getAttributes().getNamedItem("LABEL") == null) {
			oneRecord.setLabel(logicalNode.getAttributes().getNamedItem("TYPE").getNodeValue());
		}
		else {
			oneRecord.setLabel(logicalNode.getAttributes().getNamedItem("LABEL").getNodeValue());
		}
		setNextAndPrevious(oneRecord);
		return oneRecord;
	}
	
	/*
	 * Return the list of files link with the parent but not link with children
	 */
	public LinkedList<String> parentFiles (String parentId, LinkedList<String> children) {
		LinkedList<String> list = new LinkedList<String>();
		NodeList smLinks = links.getChildNodes();
		// retreive files link to this logical Node
		for (int k = 0; k < smLinks.getLength(); k++) {
			Node oneSmLink = smLinks.item(k);
			if (oneSmLink.getAttributes().getNamedItem("xlink:from")
				.getNodeValue().equals(parentId)) {
				list.add(oneSmLink.getAttributes()
					.getNamedItem("xlink:to").getNodeValue());
			}
		}
		// remove if the child has the same link
		for (int i = 0; i < children.size(); i++){
			String logicalId = children.get(i);
			for (int k = 0; k < smLinks.getLength(); k++) {
				Node oneSmLink = smLinks.item(k);
				if (oneSmLink.getAttributes().getNamedItem("xlink:from")
						.getNodeValue().equals(logicalId)) {
					if(list.contains(oneSmLink.getAttributes().getNamedItem("xlink:to").getNodeValue())){
						list.remove(oneSmLink.getAttributes().getNamedItem("xlink:to").getNodeValue());
					}
				}
			}
		}		
		System.out.println("list size = "+ list.size());
		return list;
	}
	
	/*
	 * Root Record is the Record with parentId = undefine and with descriptive metedata
	 */
	public Record createRootRecord (NodeList descriptiveDataNodes){
		String modsId = "";
		// first node of logical structure must contain link to descriptive metadata
		// (MODS) if its not the case have a look to its first child
		if(this.logicalStructure.getFirstChild()
				.getAttributes().getNamedItem("DMDID") == null){
			if(this.logicalStructure.getFirstChild()
					.getFirstChild().getAttributes().getNamedItem("DMDID") == null) {
				System.out.println("Not able to retreive dmdid");
				return null;
			}
			else{
				modsId = this.logicalStructure.getFirstChild()
					.getFirstChild().getAttributes().getNamedItem("DMDID").getNodeValue();
			}
		}
		else{
			modsId = this.logicalStructure.getFirstChild()
				.getAttributes().getNamedItem("DMDID").getNodeValue();
		}
		int nb = descriptiveDataNodes.getLength();
		System.out.println("modsId = " + modsId + "nbofNode = " + nb);
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
			Node metadataNode = descriptiveDataNodes.item(j);
			if (metadataNode.getAttributes().getNamedItem("ID").getNodeValue().equals(modsId)) {
					modsPart.appendChild(modsPart.importNode(metadataNode, true));
					String recId = this.generateID();
					System.out.println("recID = " + recId);
					ModsParser mods = new ModsParser();
					root = mods.parseNode(modsPart, recId);
					break;
			}
		}
		return root;
	}
	
	/*
	 * Set the next and previous Id of the new Record
	 */
	public void setNextAndPrevious (Record newRecord){
		if(this.lastRecord != null){
			this.lastRecord.setNextId(this.newRecord.getId());
			this.cdm.addRecord(this.lastRecord);
		}
		if (this.newRecord != null) {
			if (this.lastRecord != null) {
				this.newRecord.setPreviousId(this.lastRecord.getId());
			}
			this.lastRecord = this.newRecord;	
		}
		this.newRecord = newRecord;
	}
	
	/*
	 * Return the ID of the Record that have (parentID and defaultUrl ) as property
	 */
	public String retreiveRecord(String parentId,String defaultUrl) {
		//System.out.println("parentID " + parentId+ " = "+ defaultUrl);
		LinkedList<String> parent = new LinkedList<String>();
		parent.add(parentId);
		if(this.lastRecord != null && this.lastRecord.getDefaultUrl().equals(defaultUrl)){
			parent.addAll(this.lastRecord.getParentId());
			this.lastRecord.setParentId(parent);
			return this.lastRecord.getId();
		}
		if(this.newRecord != null && this.newRecord.getDefaultUrl().equals(defaultUrl)){
			parent.addAll(this.newRecord.getParentId());
			this.newRecord.setParentId(parent);
			return this.newRecord.getId();
		}
		Set<String> keys = this.cdm.list.keySet();
		Iterator<String> it = keys.iterator();
		while(it.hasNext()) {
			String oneKey = (String) it.next();
			if(this.cdm.list.containsKey(oneKey)){
				Record oneRecord = this.cdm.getRecord(oneKey);
				if(oneRecord.getDefaultUrl().equals(defaultUrl)){
					//System.out.println("oneRec "+ oneRecord.getId());
					parent.addAll(oneRecord.getParentId());
					oneRecord.setParentId(parent);
					this.cdm.addRecord(oneRecord);
					return oneRecord.getId();
				}
			}
			else{
				return "a problem";
			}
		}
		return "not found";
	}
	
}
