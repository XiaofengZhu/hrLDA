package com.hrLDA.doc;

import java.io.File;
import java.util.HashMap;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ling.CoreLabel;

public class Document {	
	
    protected  String cont, txtFileName, originalFilePath, datePublished, dateModified, authors;

	public HashMap<String, String> getSlidesRelationhashMap() {
		return null;
	}   
    public Document readContent(File file){
    	return null;
    }
    public Document readContent(String filePath){
    	return null;
    }
	public void setClassifier(AbstractSequenceClassifier<CoreLabel> classifier) {

	}    
	public String getCont() {
		return cont;
	}
	
	public String [] getMetaData (String s){		
		return null;
		
	}
	
	public void setCont(String cont) {
		this.cont = cont;
	}

	public String getTxtFileName() {
		return txtFileName;
	}

	public void setTxtFileName(String txtFileName) {
		this.txtFileName = txtFileName;
	}

	public String getOriginalFilePath() {
		return originalFilePath;
	}

	public void setOriginalFilePath(String originalFilePath) {
		this.originalFilePath = originalFilePath;
	}

	public String getDatePublished() {
		return datePublished;
	}

	public void setDatePublished(String datePublished) {
		this.datePublished = datePublished;
	}

	public String getDateModified() {
		return dateModified;
	}

	public void setDateModified(String dateModified) {
		this.dateModified = dateModified;
	}

	public String getAuthors() {
		return authors;
	}

	public void setAuthors(String authors) {
		this.authors = authors;
	}

	public Document(){
		this.txtFileName = "";
		this.originalFilePath= "";
		this.datePublished = ""; 
		this.dateModified= ""; 
		this.authors= "";
	}

}
