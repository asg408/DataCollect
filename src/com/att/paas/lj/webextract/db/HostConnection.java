package com.att.paas.lj.webextract.db;

import java.util.List;

/**
 * Holds the host connection information.
 * @author amarpreet geadhoke
 *
 */
public class HostConnection {

/** Index */
private int idhostconnection;

/** Host name or address */
private String host;

/** User name */
private String username;

/** Password for user name */
private String password;

private List<LogLocation> logLocations;

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
 * Gets host
 *
 * @return host
 */
public String getHost() {
	return host;
}

/**
 * Sets host
 *
 * @param host host
 */
public void setHost(String host) {
	this.host = host;
}

/**
 * Gets username
 *
 * @return username
 */
public String getUsername() {
	return username;
}

/**
 * Sets username
 *
 * @param username username
 */
public void setUsername(String username) {
	this.username = username;
}

/**
 * Gets password
 *
 * @return password
 */
public String getPassword() {
	return password;
}

/**
 * Sets password
 *
 * @param password password
 */
public void setPassword(String password) {
	this.password = password;
}

/**
 * Gets logLocations
 *
 * @return logLocations
 */
public List<LogLocation> getLogLocations() {
	return logLocations;
}

/**
 * Sets logLocations
 *
 * @param logLocations logLocations
 */
public void setLogLocations(List<LogLocation> logLocations) {
	this.logLocations = logLocations;
}

public String toString() {
	return "host='" + host + "', user='" + username + "', password='" + password + "' directories("
			+ logLocations.size() + ")='" + logLocations;
}
}
