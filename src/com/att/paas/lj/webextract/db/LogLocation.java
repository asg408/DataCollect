package com.att.paas.lj.webextract.db;

/**
 * @author amarpreet geadhoke
 *
 */
public class LogLocation {

/** Index */
private int idloglocation;

/** Foreign key */
private int idhostconnection;

/** Directory name */
private String directoryname;

/**
 * Gets idloglocation
 *
 * @return idloglocation
 */
public int getIdloglocation() {
	return idloglocation;
}

/**
 * Sets idloglocation
 *
 * @param idloglocation idloglocation
 */
public void setIdloglocation(int idloglocation) {
	this.idloglocation = idloglocation;
}

/**
 * Gets idhostconnection
 *
 * @return idhostconnection
 */
public int getIdhostconnection() {
	return idhostconnection;
}

/**
 * Sets idhostconnection
 *
 * @param idhostconnection idhostconnection
 */
public void setIdhostconnection(int idhostconnection) {
	this.idhostconnection = idhostconnection;
}

/**
 * Gets directoryname
 *
 * @return directoryname
 */
public String getDirectoryname() {
	return directoryname;
}

/**
 * Sets directoryname
 *
 * @param directoryname directoryname
 */
public void setDirectoryname(String directoryname) {
	this.directoryname = directoryname;
}

public String toString() {
	return directoryname;
}
}
