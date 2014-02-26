package com.att.paas.lj.webextract.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

import org.apache.log4j.Logger;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

/**
 * This class establishes an SSH connection with a remote host, executes a command there, then sends back the standard
 * out and standard error output of the command.
 * 
 * @author Amarpreet Geadhoke
 */
public class ExecuteThroughSSH {

private static final String LINE_SEPARATOR = System.getProperty("line.separator");

/** Log4j logger */
private static Logger logger = Logger.getLogger(ExecuteThroughSSH.class);

/** SSH connection */
private Connection connection = null;

/** SSH session */
private Session session = null;

/**
 * Connect to a remote host via SSH and execute a command. Print standard out and standard error of the command
 * output to standard out and standard error of the local terminal. In addition to the required command line
 * arguments, the java system variable "log4j.configuration" must be set and point to the url of a log4j
 * configuration file.
 * 
 * @param args -c <command to execute on remote machine> -h <remote hostname> -u <username on remote host> -p
 *        <password on remote host>
 */
public static void main(String[] args) {

	String strCommand = null;
	String strHostname = null;
	String strUsername = null;
	String strPassword = null;
	String strPrivateKeyFile = null;

	// First, we process options in command line arguments.

	// GetOpt is a class that processes command line args.
	GetOpt go = new GetOpt(args, "?c:h:p:u:");
	int ch = -1;

	/** Indicates whether to display a usage message. */
	boolean bUsagePrint = (args.length == 0); // If no args passed, show usage message.

	// Loop through the command line args.
	while ((ch = go.getopt()) != GetOpt.optEOF) {
		if ((char) ch == '?')
			bUsagePrint = true;
		else if ((char) ch == 'c')
			strCommand = go.optArgGet();
		else if ((char) ch == 'h')
			strHostname = go.optArgGet();
		else if ((char) ch == 'k')
			strPrivateKeyFile = go.optArgGet();
		else if ((char) ch == 'p')
			strPassword = go.optArgGet();
		else if ((char) ch == 'u')
			strUsername = go.optArgGet();
	}

	String strUsage =
			"Usage: java -dlog4j.configuration=<log4j config url> gov.ed.fsa.ita.ExecuteThroughSSH\n"
					+ "\t-c <command to execute on remote machine> -h <remote hostname> -u <username on remote host> -p <password on remote host>\n"
					+ "\t-k <private key file>\n\n"
					+ "Executes a command on a remote host via an SSH connection and writes the output to standard out and standard error.";

	ExecuteThroughSSH executeThroughSSH = new ExecuteThroughSSH();

	Response response = null;
	try {
		response =
				executeThroughSSH.executeCommand(strHostname, strUsername, strPassword, strPrivateKeyFile, strCommand);
		if (bUsagePrint) {
			throw new IllegalArgumentException();
		}
	} catch (IOException ex) {
		logger.error(ex.getMessage());
		System.exit(-1);
	} catch (IllegalArgumentException ex) {
		if (ex.getMessage().length() > 0) {
			logger.error(ex.getMessage());
		}
		logger.fatal(strUsage);
		System.exit(-1);
	}

	BufferedReader brStdout = new BufferedReader(new InputStreamReader(response.getStdOut()));

	while (true) {
		String line = null;
		try {
			line = brStdout.readLine();
		} catch (IOException ex) {
			logger.fatal("Error reading standard out from remote host: " + ex.getMessage());
			System.exit(-1);
		}

		if (line == null) {
			break;
		}

		System.out.println(line);
	}

	BufferedReader brStderr = new BufferedReader(new InputStreamReader(response.getStdErr()));

	while (true) {
		String line = null;
		try {
			line = brStderr.readLine();
		} catch (IOException ex) {
			logger.fatal("Error reading standard error from remote host: " + ex.getMessage());
			System.exit(-1);
		}

		if (line == null) {
			break;
		}

		System.err.println(line);
	}

	/* Show exit status, if available (otherwise "null") */
	logger.info("ExitCode: " + response.getExitStatus());

	executeThroughSSH.close();

}

/**
 * Creates an SSH connection with a remote host and executes a command there. Returns a response object that
 * contains the standard out and standard error input streams, along with the exit status code form the SSH session.
 * After calling this method, and accessing the standard out and standard error streams (if needed), the close()
 * method must be called to release local resources.
 * 
 * @param hostname Name or IP address of the remote host.
 * @param username User name on the remote host.
 * @param password Password for the user name on the remote host.
 * @param keyfile Name of private key file. If there is none, specify null.
 * @param command Command to be executed on the remote host.
 * @return Response object containing standard out and standard error input streams, along with the exit status code
 *         from the SSH session.
 * @throws IOException Each step of creating the session and executing the command can throw an exeception.
 */
public Response executeCommand(String hostname, String username, String password, String keyfile, String command)
		throws IOException {

	// Hostname.
	String strHostname = hostname;

	// Username.
	String strUsername = username;

	// Password.
	String strPassword = password;

	// Key file.
	String strKeyFile = keyfile;

	// Command.
	String strCommand = command;

	// Check that all args are not null.
	if (strHostname == null) {
		throw new IllegalArgumentException("Hostname not specified.");
	}

	if (strUsername == null) {
		throw new IllegalArgumentException("Username not specified.");
	}
	//
	// if (strPassword == null) {
	// throw new IllegalArgumentException("Pasword not specified.");
	// }

	if (strCommand == null) {
		throw new IllegalArgumentException("Command not specified.");
	}

	/* Create a connection instance */
	connection = new Connection(strHostname);

	try {
		/* Now connect */
		connection.connect();
	} catch (IOException ex) {
		String strError = "Error connecting: " + ex.getMessage();
		throw new IOException(strError);
	}

	try {
		// Authenticate. That is, log in. Use private key if specified. If not, just user/pw.
		if (strKeyFile == null) {
			if (!connection.authenticateWithPassword(strUsername, strPassword)) {
				throw new IOException();
			}
		} else {
			if (!connection.authenticateWithPublicKey(strUsername, new File(strKeyFile), strPassword)) {
				throw new IOException();
			}
		}

	} catch (IOException ex) {
		String strError = "Authentication failed: " + ex.getMessage();
		throw new IOException(strError);
	}

	try {
		/* Create a session */
		session = connection.openSession();
	} catch (IOException ex) {
		String strError = "Open session failed: " + ex.getMessage();
		throw new IOException(strError);
	}

	try {
		// Execute the command.
		session.execCommand(strCommand);
	} catch (IOException ex) {
		String strError = "Error executing command on remote host: " + ex.getMessage();
		throw new IOException(strError);
	}

	// Create the response object.
	Response response =
			new Response(new StreamGobbler(session.getStdout()), new StreamGobbler(session.getStderr()),
					session.getExitStatus());

	return response;
}

/**
 * Closes the SSH session and connection. After calling this method, the standard out and standard error input
 * streams will be unusable. ALWAYS INVOKE THIS METHOD TO RELEASE LOCAL RESOURCES AND PREVENT A MEMORY LEAK.
 */
public void close() {

	/* Close this session */
	session.close();

	/* Close the connection */
	connection.close();

}

/**
 * Gets a remote file via an SSH connection, using the cat command on the remote host, and capturing its stdout.
 *
 * @param strHost Remote host file resides on
 * @param strUser User on remote host
 * @param strPassword Password for user on remote host
 * @param strKeyFile Name of key file for SSH connection. If there is none, specify null.
 * @param strRemoteFileName Name of remote file to get.
 * @param fileOutput File to be (over)written with remote file contents
 * @throws ExecuteThroughSSHException On error
 */
public static void getRemoteFile(String strHost, String strUser, String strPassword, String strKeyFile,
		String strRemoteFileName, File fileOutput) throws ExecuteThroughSSHException {
	getRemoteFile(strHost, strUser, strPassword, strKeyFile, strRemoteFileName, fileOutput, false);
}

/**
 * Gets a remote file via an SSH connection, using the cat command on the remote host, and capturing its stdout.
 *
 * @param strHost Remote host file resides on
 * @param strUser User on remote host
 * @param strPassword Password for user on remote host
 * @param strKeyFile Name of key file for SSH connection. If there is none, specify null.
 * @param strRemoteFileName Name of remote file to get.
 * @param fileOutput File to be (over)written with remote file contents
 * @param append Append to file?
 * @throws ExecuteThroughSSHException On error
 */
public static void getRemoteFile(String strHost, String strUser, String strPassword, String strKeyFile,
		String strRemoteFileName, File fileOutput, boolean append) throws ExecuteThroughSSHException {

	String strFileContents = getRemoteFile(strHost, strUser, strPassword, strKeyFile, strRemoteFileName);

	BufferedReader br = new BufferedReader(new StringReader(strFileContents));

	BufferedWriter bw;
	try {
		bw = new BufferedWriter(new FileWriter(fileOutput, append));
	} catch (IOException ex) {
		throw new ExecuteThroughSSHException("Opening file '" + fileOutput + "' for writing after command 'cat "
				+ strRemoteFileName + "' on host '" + strHost + "' as user '" + strUser + "': " + ex.getMessage(), ex,
				"Opening output file", fileOutput.toString());
	}

	String strInput = null;
	try {
		while ((strInput = br.readLine()) != null) {
			try {
				bw.write(strInput);
				bw.write(LINE_SEPARATOR);
			} catch (IOException ex) {
				throw new ExecuteThroughSSHException("Writing file '" + fileOutput + "' after command 'cat "
						+ strRemoteFileName + "' on host '" + strHost + "' as user '" + strUser + "': "
						+ ex.getMessage(), ex, "Opening output file", fileOutput.toString());
			}
		}
	} catch (IOException ex) {
		throw new ExecuteThroughSSHException("Unexpected error reading string output of results of command 'cat "
				+ strRemoteFileName + "' on host '" + strHost + "' as user '" + strUser + "': " + ex.getMessage(), ex,
				"Opening output file", fileOutput.toString());
	}

	try {
		bw.close();
	} catch (IOException ex) {
		throw new ExecuteThroughSSHException("Closing file '" + fileOutput + "' after command 'cat "
				+ strRemoteFileName + "' on host '" + strHost + "' as user '" + strUser + "': " + ex.getMessage(), ex,
				"Opening output file", fileOutput.toString());
	}
}

/**
 * Gets a remote file via an SSH connection, using the cat command on the remote host, and capturing its stdout.
 *
 * @param strHost Remote host file resides on
 * @param strUser User on remote host
 * @param strPassword Password for user on remote host
 * @param strKeyFile Name of key file for SSH connection. If there is none, specify null.
 * @param strRemoteFileName Name of remote file to get.
 * @return File contents as a string
 * @throws ExecuteThroughSSHException Problem with SSH session or remote cat command.
 */
public static String getRemoteFile(String strHost, String strUser, String strPassword, String strKeyFile,
		String strRemoteFileName) throws ExecuteThroughSSHException {

	ExecuteThroughSSH exec = new ExecuteThroughSSH();

	ExecuteThroughSSH.Response response = null;

	try {
		response = exec.executeCommand(strHost, strUser, strPassword, null, "cat " + strRemoteFileName);
	} catch (IOException ex) {
		throw new ExecuteThroughSSHException("Executing command 'cat " + strRemoteFileName + "' on host '" + strHost
				+ "' as user '" + strUser + "': " + ex.getMessage(), ex, "Executing command", "cat "
				+ strRemoteFileName);
	}

	BufferedReader brStdOut = new BufferedReader(new InputStreamReader(response.getStdOut()));

	BufferedReader brStdErr = new BufferedReader(new InputStreamReader(response.getStdErr()));

	StringBuffer sbStdErr = new StringBuffer();
	String strLineStdErr = null;
	try {
		for (boolean bFirst = true; (strLineStdErr = brStdErr.readLine()) != null; bFirst = false) {
			if (!bFirst) {
				sbStdErr.append('\n');
			}
			sbStdErr.append(strLineStdErr);
		}
	} catch (IOException ex) {
		throw new ExecuteThroughSSHException("Reading standard error from command 'cat " + strRemoteFileName
				+ "' on host '" + strHost + ": " + ex.getMessage(), ex, "Reading standard error", "cat "
				+ strRemoteFileName);
	}

	StringBuffer sbRemoteFileContents = new StringBuffer();
	String strLineStdOut = null;
	try {
		for (boolean bFirst = true; (strLineStdOut = brStdOut.readLine()) != null; bFirst = false) {
			if (!bFirst) {
				sbRemoteFileContents.append('\n');
			}
			sbRemoteFileContents.append(strLineStdOut).append('\n');
		}
	} catch (IOException ex) {
		throw new ExecuteThroughSSHException("Reading standard out from command 'cat " + strRemoteFileName
				+ "' on host '" + strHost + ": " + ex.getMessage(), ex, "Reading standard error", "cat "
				+ strRemoteFileName);
	}

	try {
		brStdOut.close();
	} catch (IOException ex) {
		throw new ExecuteThroughSSHException("Closing standard out from command 'cat " + strRemoteFileName
				+ "' on host '" + strHost + ": " + ex.getMessage(), ex, "Reading standard error", "cat "
				+ strRemoteFileName);
	}

	try {
		brStdErr.close();
	} catch (IOException ex) {
		throw new ExecuteThroughSSHException("Closing standard error from command 'cat " + strRemoteFileName
				+ "' on host '" + strHost + ": " + ex.getMessage(), ex, "Reading standard error", "cat "
				+ strRemoteFileName);
	}

	if (sbStdErr.length() > 0) {
		throw new ExecuteThroughSSHException("Error on results while executing command 'cat " + strRemoteFileName
				+ "' on host '" + strHost + ": " + sbStdErr, null, "Results from command", "cat " + strRemoteFileName);
	}

	return sbRemoteFileContents.toString();
}

/**
 * This class holds the output of the SSH session.
 */
public class Response {

/** Standard out stream. */
private InputStream stdout = null;

/** Standard error stream. */
private InputStream stderr = null;

/** Exit status from last command executed. */
private Integer exitStatus = null;

/**
 * Constructor.
 * 
 * @param stdout Standard out stream from the SSH session.
 * @param stderr Standard error stream from the SSH session.
 * @param exitStatus Exit status code from the SSH session.
 */
public Response(InputStream stdout, InputStream stderr, Integer exitStatus) {
	this.stdout = stdout;
	this.stderr = stderr;
	this.exitStatus = exitStatus;
}

/**
 * @return Returns the standard out from the remote command.
 */
public InputStream getStdOut() {
	return stdout;
}

/**
 * @return Returns the standard out from the remote command.
 */
public InputStream getStdErr() {
	return stderr;
}

/**
 * @return Returns the exit status.
 */
public int getExitStatus() {
	return exitStatus == null ? 0 : exitStatus.intValue();
}
}

public static class ExecuteThroughSSHException extends Exception {

/** Operation */
private String strOperation = null;

/** Name of file causing error */
private String strCommand = null;

public ExecuteThroughSSHException(String strMessage, Throwable throwableCause, String strOperationArg,
		String strCommandArg) {
	super(strMessage, throwableCause);
	this.strCommand = strCommandArg;
	this.strOperation = strOperationArg;
}

public String getCommand() {
	return strCommand;
}

public String getOperation() {
	return strOperation;
}

}
}
