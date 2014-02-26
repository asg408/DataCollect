package com.att.paas.lj.webextract.util;

import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.helpers.NullEnumeration;

/**
 * If log4j is not configured, the doIt methods will set up a basic ConsoleAppender and tell log4j to use it. This 
 * makes it easy to call the main methods of utility classes. The default level of logging is "DEBUG". This can be
 * overriden by calling one of the methods that allow the specification of level, or by setting the system
 * property "gov.ed.fsa.ita.util.simplelog4jconfig.level".
 * 
 * @author Amarpreet geadhoke
 */
public class SimpleLog4jConfig {

/** System property for log4j level. If this is set, then its value will be the minimum level output by log4j. */
public final static String LOG4J_LEVEL_PROPERTY = "gov.ed.fsa.ita.util.simplelog4jconfig.level";

/**
 * Set up a log4j console appender if at least one is not set up already. All messages will be logged.
 */
public static void doIt() {
	doIt(System.getProperty(LOG4J_LEVEL_PROPERTY));
}

/**
 * Set up a log4j console appender if at least one is not set up already
 * 
 * @param strLevel Minimum message leve to log. If null, defaults to "DEBUG"
 */
public static void doIt(String strLevel) {

	// Convert arg to level. If the arg is not tha name of a valid level, or if it is null, level is set to debug.
	doIt(Level.toLevel(strLevel, Level.DEBUG));

}

/**
 * Set up a log4j console appender if at least one is not set up already
 * 
 * @param level Minimum message leve to log.
 */
public static void doIt(Level level) {

	// If there are any appenders already, abort.
	if (!(LogManager.getRootLogger().getAllAppenders() instanceof NullEnumeration)) {
		return;
	}

	// Create properties object and populate with simple ConsoleAppender configuration.
	Properties props = new Properties();

	props.setProperty("log4j.rootLogger", level.toString() + ", A1");
	props.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
	props.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
	props.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d  %p  %c - %m%n");

	// Configure log4j with the appender in the properties object.
	PropertyConfigurator.configure(props);
}

}
