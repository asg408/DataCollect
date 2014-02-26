package com.att.paas.lj.webextract;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import com.att.paas.lj.webextract.db.DataUsage;
import com.att.paas.lj.webextract.db.HostConnection;
import com.att.paas.lj.webextract.db.LogLocation;
import com.att.paas.lj.webextract.util.ExecuteThroughSSH;
import com.att.paas.lj.webextract.util.ExecuteThroughSSH.ExecuteThroughSSHException;
import com.att.paas.lj.webextract.util.ExecuteThroughSSH.Response;
import com.att.paas.lj.webextract.util.GetOpt;
import com.att.paas.lj.webextract.util.SimpleLog4jConfig;

/**
 * @author Amarpreet geadhoke
 *
 */
public class LjWebDataExtract {

/** Log4j logger */
private static final Logger logger = Logger.getLogger(LjWebDataExtract.class);

/** Local access log file */
private static final String ACCESS_LOG_FILE = "data/access.log";

/** Map to hold bytes used */
private final Map<TenantTimeIntervalKey, Integer> mapBytesUsed = new TreeMap<TenantTimeIntervalKey, Integer>();

/** Hibernate session factory */
private SessionFactory sessionFactory = null;

/**
 * Main
 *
 * @param args as described in usage
 */
public static void main(String[] args) {
	new LjWebDataExtract().process(args);
}

/**
 * Does all the processing for this class
 *
 * @param args described in usage
 */
private void process(String[] args) {

	// If log4j is not configured, create a simple configuration
	SimpleLog4jConfig.doIt();

	// Date format in web access logs
	SimpleDateFormat sdfAccessLog = new SimpleDateFormat("[dd/MMM/yyyy:HH:mm:ss Z");

	// This pattern is when the request for a page has the tenant id
	Pattern pattern1 =
			Pattern.compile("(.*?) - - (\\[.*?\\]) \".*?\" \\d*? (\\d*?) \"http://.*?/networking/Service\\?.*?[\\&\\?]id=(.*?)[\\&\"].*");

	// This is when the referring page has the tenant id
	Pattern pattern2 =
			Pattern.compile("(.*?) - - (\\[.*?\\]) \"GET /networking/Service\\?.*?[\\&\\?]id=(.*?)[\\&\"].*? \\d*? (\\d*?) .*");

	// Interval duration for data collection
	int intervalDuration = 15;

	// DB user name
	String dbUser = null;

	// DB password
	String dbPassword = null;

	// DB connection string
	String dbConnectString = null;

	// GetOpt is a class that processes command line args.
	GetOpt go = new GetOpt(args, "?c:i:p:u:");
	int ch = -1;

	/** Indicates whether to display a usage message. */
	boolean bUsagePrint = (args.length == 0); // If no args passed, show usage message.

	// Loop through the command line args.
	while ((ch = go.getopt()) != GetOpt.optEOF) {
		if ((char) ch == '?') {
			bUsagePrint = true;
		} else if ((char) ch == 'c') {
			dbConnectString = go.optArgGet();
		} else if ((char) ch == 'i') {
			intervalDuration = go.processArg(go.optArgGet(), 0);
		} else if ((char) ch == 'p') {
			dbPassword = go.optArgGet();
		} else if ((char) ch == 'u') {
			dbUser = go.optArgGet();
		}
	}

	// Show the user how to call this program.
	if (bUsagePrint || intervalDuration == 0 || dbUser == null || dbPassword == null || dbConnectString == null) {
		logger.error("Usage: com.att.paas.lj.webextract.LjWebDataExtract -i <data collection interval> dn"
				+ "\t -c<db connection string> -u<db user> -p<db password>\n"
				+ "\t-v <allowed variance (in muinutes) between mainframe time and audit log time (default 1)]");
		return;
	}

	Configuration hibernateConfiguration = new Configuration();
	hibernateConfiguration.setProperty("hibernate.connection.url", dbConnectString);
	hibernateConfiguration.setProperty("hibernate.connection.username", dbUser);
	hibernateConfiguration.setProperty("hibernate.connection.password", dbPassword);

	// Get Hibernate session factory
	sessionFactory = hibernateConfiguration.configure().buildSessionFactory();

	// Open a hibernate session
	Session session = sessionFactory.openSession();

	// Get host connection and directory information
	session.beginTransaction();
	@SuppressWarnings("unchecked")
	List<HostConnection> hostConnections = session.createQuery("from HostConnection").list();
	session.getTransaction().commit();

	ExecuteThroughSSH ets = new ExecuteThroughSSH();

	// Loop through all locations for all hosts
	for (HostConnection hcCurrent : hostConnections) {
		for (LogLocation llCurrent : hcCurrent.getLogLocations()) {

			// If there is no log location, go on to the next
			if (llCurrent == null) {
				continue;
			}

			// Perform an ls on the log location, to get all the "access_log*" files (e.g. access_log, access_log.1,
			// access_log.2, ...
			Response response = null;
			try {
				response =
						ets.executeCommand(hcCurrent.getHost(), hcCurrent.getUsername(), hcCurrent.getPassword(), null,
								"ls -1 " + llCurrent.getDirectoryname() + "/access_log*");
			} catch (IOException ex) {
				logger.error("Trying to perform 'ls' command on " + hcCurrent.getUsername() + "@" + hcCurrent.getHost()
						+ ":" + llCurrent.getDirectoryname() + ": " + ex);
				return;
			}

			// Get stdout from the ls command
			InputStream inputstreamToUseOut = response.getStdOut();

			// Reader for standard error
			BufferedReader brErr = new BufferedReader(new InputStreamReader(response.getStdErr()));

			// Buffer for error messages
			StringBuffer sbErr = new StringBuffer();

			// Line of output
			String strLineErr = null;
			try {
				while ((strLineErr = brErr.readLine()) != null) {
					sbErr.append("\t").append(strLineErr);
				}
			} catch (IOException ex) {
				logger.error("Reading stderr after  'ls' command on " + hcCurrent.getUsername() + "@"
						+ hcCurrent.getHost() + ":" + llCurrent.getDirectoryname() + ": " + ex);
				return;
			}

			if (sbErr.length() != 0) {
				logger.warn("No stderr for 'ls' command on " + hcCurrent.getUsername() + "@" + hcCurrent.getHost()
						+ ":" + llCurrent.getDirectoryname());
				return;
			}

			// Close the ssh session to release resources
			ets.close();

			// Create a set to hold the file names, ordered by name to make the earliest first, like:
			// access_log.2, access_log.1, access_log
			// NB: It's not necessary to put the files in time order, it just makes it easier to track when debugging.
			Set<String> setLogFiles = new TreeSet<String>(new Comparator<String>() {

				public int compare(String string1, String string2) {
					// Get the numeric extension, if any
					int positionNumericExtension1 = string1.lastIndexOf('.');
					int positionNumericExtension2 = string2.lastIndexOf('.');

					// The current access_log has no extension, put it at the end of the list
					if (positionNumericExtension1 == -1 || positionNumericExtension1 == string1.length() - 1) {
						return 1;
					}

					if (positionNumericExtension2 == -1 || positionNumericExtension2 == string2.length() - 1) {
						return -1;
					}

					// Convert numeric extensions into int
					int numericExtension1 = 0;
					try {
						numericExtension1 = Integer.parseInt(string1.substring(positionNumericExtension1 + 1));
					} catch (NumberFormatException ex) {
						return 1;
					}

					int numericExtension2 = 0;
					try {
						numericExtension2 = Integer.parseInt(string2.substring(positionNumericExtension2 + 1));
					} catch (NumberFormatException ex) {
						return -1;
					}

					return numericExtension1 < numericExtension2 ? 1 : numericExtension2 < numericExtension1 ? -1 : 0;

				}
			});

			// Reader for standard out
			LineNumberReader lnrOut = new LineNumberReader(new InputStreamReader(inputstreamToUseOut));

			String line = null;
			try {
				while ((line = lnrOut.readLine()) != null) {
					setLogFiles.add(line.trim());
				}
			} catch (IOException ex) {
				logger.error("Reading stdout after  'ls' command on " + hcCurrent.getUsername() + "@"
						+ hcCurrent.getHost() + ":" + llCurrent.getDirectoryname() + ": " + ex);
				return;
			}

			// Get an iterator, earliest files first
			Iterator<String> iteratorSetLogFiles = setLogFiles.iterator();

			boolean append = false;
			while (iteratorSetLogFiles.hasNext()) {
				String filenameCurrent = iteratorSetLogFiles.next();

				try {
					// Create(if first file)/append local access log file
					ExecuteThroughSSH.getRemoteFile(hcCurrent.getHost(), hcCurrent.getUsername(),
							hcCurrent.getPassword(), null, filenameCurrent, new File(ACCESS_LOG_FILE), append);
				} catch (ExecuteThroughSSHException ex) {
					logger.error("Getting file " + hcCurrent.getUsername() + "@" + hcCurrent.getHost() + ":"
							+ filenameCurrent + ": " + ex);
					return;
				}

				append = true;
			}

			// Read the combined access log file
			LineNumberReader accessLogReader = null;
			try {
				accessLogReader = new LineNumberReader(new FileReader(ACCESS_LOG_FILE));
			} catch (FileNotFoundException ex) {
				logger.warn("Access log file " + ACCESS_LOG_FILE + " not found. Nothing to do for location "
						+ hcCurrent.getHost() + ":" + llCurrent.getDirectoryname());
				continue;
			}

			String accessLogLine = null;
			try {
				while ((accessLogLine = accessLogReader.readLine()) != null) {

					// We capture remote ip, but we don't use it now. We will use it later to filter AVPN traffic
					@SuppressWarnings("unused")
					String remoteIpAddress = null;

					// Interval start date/time
					String intervalStartString = null;

					// Number of bytes delivered for the request
					String numberOfBytesString = null;

					// tenant id
					String tenantIdString = null;

					// There are two patterns to try and match against
					Matcher matcher1 = pattern1.matcher(accessLogLine);

					if (matcher1.find()) {

						remoteIpAddress = matcher1.group(1);

						intervalStartString = matcher1.group(2);

						numberOfBytesString = matcher1.group(3);

						tenantIdString = matcher1.group(4);
					} else {
						Matcher matcher2 = pattern2.matcher(accessLogLine);

						if (matcher2.find()) {
							remoteIpAddress = matcher2.group(1);

							intervalStartString = matcher2.group(2);

							tenantIdString = matcher2.group(3);

							numberOfBytesString = matcher2.group(4);
						} else {
							// No match against either pattern. Skip this record.
							continue;
						}
					}
					Date timeStamp = null;
					try {
						timeStamp = sdfAccessLog.parse(intervalStartString);
					} catch (ParseException ex) {
						continue;
					}

					int numberOfBytes = 0;
					try {
						numberOfBytes = Integer.parseInt(numberOfBytesString);
					} catch (NumberFormatException ex) {
						continue;
					}

					int tenantId = 0;
					try {
						tenantId = Integer.parseInt(tenantIdString);
					} catch (NumberFormatException ex) {
						continue;
					}

					if (tenantId == -1) {
						continue;
					}

					Date startInterval =
							new Date((timeStamp.getTime() / (intervalDuration * 60 * 1000)) * intervalDuration * 60
									* 1000);

					// Create key object
					TenantTimeIntervalKey ttik = new TenantTimeIntervalKey(tenantId, startInterval, intervalDuration);

					// Create map entry if it doesn't exist. If it exists, add to the value there.
					mapBytesUsed.put(ttik, mapBytesUsed.containsKey(ttik) ? mapBytesUsed.get(ttik) + numberOfBytes
							: numberOfBytes);

				}
			} catch (IOException ex) {
				logger.error("Reading file " + hcCurrent.getHost() + ":" + llCurrent.getDirectoryname() + "/"
						+ ACCESS_LOG_FILE);
				continue;
			}

		} // log location loop
	} // host connection loop

	// Store the data
	storeUsageData();
} // main

/**
 * Put the collected data in the db
 */
private void storeUsageData() {

	// Open a hibernate session
	Session session = sessionFactory.openSession();

	Iterator<TenantTimeIntervalKey> iterator = mapBytesUsed.keySet().iterator();

	Transaction transaction = session.beginTransaction();

	// Process each buckwet
	while (iterator.hasNext()) {
		TenantTimeIntervalKey ttikCurrent = iterator.next();

		int numberOfBytes = mapBytesUsed.get(ttikCurrent);

		// Find out if the bucket is already in the db
		Query query =
				session.createQuery("from DataUsage where idTenant=:idtenant and intervalStart=:intervalstart and intervalDuration=:intervalduration");
		query.setInteger("idtenant", ttikCurrent.getTenantId());
		query.setTimestamp("intervalstart", ttikCurrent.getIntervalStart());
		query.setInteger("intervalduration", ttikCurrent.getInterval());
		DataUsage dataUsageExisting = (DataUsage) query.uniqueResult();

		if (dataUsageExisting != null) {
			// Bucket exists, update byte count and replace
			dataUsageExisting.setNumberOfBytes(numberOfBytes);
			session.update(dataUsageExisting);
		} else {
			// Bucket does not exist, create and insert.
			DataUsage dataUsage = new DataUsage();
			dataUsage.setIdTenant(ttikCurrent.getTenantId());
			dataUsage.setIntervalStart(ttikCurrent.getIntervalStart());
			dataUsage.setIntervalDuration(ttikCurrent.getInterval());
			dataUsage.setNumberOfBytes(numberOfBytes);

			session.save(dataUsage);
		}
	} // data usage map iterator loop

	transaction.commit();
}

/**
 * Inner class to hold the keys to the byte collecting buckets
 */
private class TenantTimeIntervalKey implements Comparable<TenantTimeIntervalKey> {

/** Tenant id */
private final int tenantId;

/** Interval start date/time */
private final Date intervalStart;

/** Interval duration */
private final int intervalDuration;

/**
 * 
 * Constructor.
 *
 * @param tenantIdArg tenant id
 * @param dateTimeStampArg timestamp
 * @param intervalArg interval
 */
public TenantTimeIntervalKey(int tenantIdArg, Date dateTimeStampArg, int intervalArg) {
	this.tenantId = tenantIdArg;
	this.intervalStart = dateTimeStampArg;
	this.intervalDuration = intervalArg;
}

/**
 * Gets tenantId
 *
 * @return tenantId
 */
public int getTenantId() {
	return tenantId;
}

/**
 * Gets dateTimeStamp
 *
 * @return dateTimeStamp
 */
public Date getIntervalStart() {
	return intervalStart;
}

/**
 * Gets interval
 *
 * @return interval
 */
public int getInterval() {
	return intervalDuration;
}

/**
 * Is this object equal to another? 
 *
 * @param other other object
 * @return true if all the instance vars have the same value. false otherwise.
 */
@SuppressWarnings("unused")
// Not used locally, but used by Map to hash.
public boolean equals(TenantTimeIntervalKey other) {
	return compareTo(other) == 0;
}

/**
 * compares objects of this type
 * 
 * @param other other object to comapre to
 * @return 1 if this object is greater than other, -1 if this object is less then other, 0 if they are equal.
 */
public int compareTo(TenantTimeIntervalKey other) {
	if (this.getTenantId() != other.getTenantId()) {
		return this.getTenantId() > other.getTenantId() ? 1 : -1;
	}

	int dateComparison = this.getIntervalStart().compareTo(other.getIntervalStart());
	if (dateComparison != 0) {
		return dateComparison;
	}

	if (this.getInterval() != other.getInterval()) {
		return this.getInterval() > other.getInterval() ? 1 : -1;
	}

	return 0;
}
}
} // class
