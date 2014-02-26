package com.att.paas.lj.webextract.db;

import java.sql.Timestamp;

/**
 * @author Amarpreet geadhoke
 *
 */
public class DataUsage {

/** Key */
private int idDataUsage;

/** Tenant */
private int idTenant;

/** Start date/time for interval */
private Timestamp intervalStart;

/** Interval length */
private int intervalDuration;

/** Number of bytes sent during interval */
private int numberOfBytes;

/**
 * Gets idDataUsage
 *
 * @return idDataUsage
 */
public int getIdDataUsage() {
	return idDataUsage;
}

/**
 * Sets idDataUsage
 *
 * @param idDataUsage idDataUsage
 */
public void setIdDataUsage(int idDataUsage) {
	this.idDataUsage = idDataUsage;
}

/**
 * Gets idTenant
 *
 * @return idTenant
 */
public int getIdTenant() {
	return idTenant;
}

/**
 * Sets idTenant
 *
 * @param idTenant idTenant
 */
public void setIdTenant(int idTenant) {
	this.idTenant = idTenant;
}

/**
 * Gets dateTimeStart
 *
 * @return dateTimeStart
 */
public Timestamp getIntervalStart() {
	return intervalStart;
}

/**
 * Sets dateTimeStart
 *
 * @param dateTimeStart dateTimeStart
 */
public void setIntervalStart(Timestamp intervalStartArg) {
	this.intervalStart = intervalStartArg;
}

/**
 * Sets dateTimeStart
 *
 * @param intervalStart dateTimeStart
 */
public void setIntervalStart(java.util.Date intervalStart) {
	this.intervalStart = new Timestamp(intervalStart.getTime());
}

/**
 * Gets interval
 *
 * @return interval
 */
public int getIntervalDuration() {
	return intervalDuration;
}

/**
 * Sets interval
 *
 * @param interval interval
 */
public void setIntervalDuration(int interval) {
	this.intervalDuration = interval;
}

/**
 * Gets numberOfBytes
 *
 * @return numberOfBytes
 */
public int getNumberOfBytes() {
	return numberOfBytes;
}

/**
 * Sets numberOfBytes
 *
 * @param numberOfBytes numberOfBytes
 */
public void setNumberOfBytes(int numberOfBytes) {
	this.numberOfBytes = numberOfBytes;
}

}
