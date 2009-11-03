package multivio.org.dfst;

import java.util.HashMap;

/*
 * CoreDocumentModel is a list of Records
 */
public class CoreDocumentModel {

	public HashMap<String, Record> list;

	public CoreDocumentModel() {
		this.list = new HashMap<String, Record>();
	}

	public HashMap<String, Record> getCDM() {
		return this.list;
	}
	
	public Record getRecord(String key) {
		return this.list.get(key);
	}

	public void addRecord(Record rec) {
		this.list.put(rec.getId(), rec);
	}
}
