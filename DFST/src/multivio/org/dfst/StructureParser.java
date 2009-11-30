package multivio.org.dfst;

import multivio.org.communication.*;

import java.util.Set;
import java.util.Arrays;
import java.io.IOException;
import java.io.*;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.w3c.dom.Document;

/**
 * StructureParser is a singleton. The goal of this class is to choose witch
 * parser to use
 * 
 * @author heritierc
 * 
 */
public class StructureParser {
	
	private ParserInterface parser;

	public StructureParser() {}
	
	public String getDoc (HttpServletRequest request, HttpServletResponse response) throws IOException {
		String res = "do get";
		String fileNumber = request.getParameter("recid");
		
		if(fileNumber != null){
			System.out.println("value of the request " + fileNumber);
			ServerDocument serv = new ServerDocument();
			if (fileNumber.indexOf("PPN") != -1) {
				fileNumber = "http://gdz.sub.uni-goettingen.de/mets_export.php?PPN="+fileNumber;
			}
			else {
				fileNumber = "http://doc.rero.ch/record/" + fileNumber + "/export/xd?";
			}
			Document doc = serv.getMetadataDocument(fileNumber);
			res = this.selectStrategy(doc);
		}
		return res;
	}


	public String selectStrategy(Document doc) {
		// to do choose the rigth parser
		// today only dublinCore or Mets
		if (doc.getDocumentElement().getNodeName().indexOf("mets") != -1) {
			System.out.println("mets parser selected");
			this.parser = new MetsDFGParser();
		}
		else {
			this.parser = new DublinCoreParser();
		}
		CoreDocumentModel cdm = this.parser.parseDocument(doc);
		String res = writeCDM(cdm);
		return res;
	}

	public String writeCDM(CoreDocumentModel cdm) {
		//
		Set<String> keys = cdm.getCDM().keySet();
		StringBuffer myString = new StringBuffer();

		myString.append("{");
		String[] tl = keys.toArray(new String[keys.size()]);
		Arrays.sort(tl);
		for (int h = 0; h < keys.size(); h++) {
			String oneKey = tl[h].toString();
			Record oneRecord = cdm.getCDM().get(oneKey);
			myString.append("\"" + oneRecord.getId() + "\": {");
			myString.append(oneRecord.getRecordToString() + "}");
			if (h != keys.size() - 1) {
				myString.append(", ");
			}

		}
		myString.append("}");
		return myString.toString();
	}
}
