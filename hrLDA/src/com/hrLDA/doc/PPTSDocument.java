package com.hrLDA.doc;

import java.util.HashMap;

public class PPTSDocument extends FTXDocument{

	private HashMap <String, String> slidesRelationhashMap = new HashMap <String, String> ();
	
	public void setSlidesRelationhashMap(
			HashMap<String, String> slidesRelationhashMap) {
		this.slidesRelationhashMap = slidesRelationhashMap;
	}

	public HashMap<String, String> getSlidesRelationhashMap() {
		return this.slidesRelationhashMap;
	}

	public void addToSlidesRelationhashMap(String currentText, String parentText) {
		this.slidesRelationhashMap.put(currentText, parentText);
	}
	public PPTSDocument() {
		this.slidesRelationhashMap = null;
	}

}
