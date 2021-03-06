package gov.loc.repository.bagit.utilities.namevalue.impl;

import gov.loc.repository.bagit.utilities.namevalue.NameValueReader;

import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class NameValueReaderImpl implements NameValueReader {

	private static final Log log = LogFactory.getLog(NameValueReaderImpl.class);
	
	private Deque<String> lines = new ArrayDeque<String>();
	private String type;
	
	private Pattern continueLinePattern = Pattern.compile("^( |\\t)+.+$");
	private Pattern continueLineReplacePattern = Pattern.compile("^( |\\t)+");
	private Pattern continueNewlinePattern = Pattern.compile("^( |\\t)*$");
	
	public NameValueReaderImpl(String encoding, InputStream in, String type) {
		this.type = type;

		InputStreamReader fr = null;
		BufferedReader reader = null;
		try
		{
			// Replaced FileReader with InputStreamReader since all bagit manifest and metadata files must be UTF-8
			// encoded.  If UTF-8 is not explicitly set then data will be read in default native encoding.
			fr = new InputStreamReader(in, encoding);
			reader = new BufferedReader(fr);
			String line = reader.readLine();
			while(line != null) {
				lines.addLast(line);
				line = reader.readLine();
			}
		}
		catch(Exception ex)
		{
			throw new RuntimeException(ex);
		}
		finally {
			IOUtils.closeQuietly(fr);
			IOUtils.closeQuietly(reader);
			IOUtils.closeQuietly(in);
		}
	}
		
	public boolean hasNext() {
		if (lines.isEmpty()) {
			return false;
		}
		return true;
	}	
		
	public NameValue next() {
		if (! this.hasNext()) {
			throw new NoSuchElementException();
		}
		//Split the first line
		String line = this.lines.removeFirst();
		String[] splitString = line.split(" *: *", 2);
		String name = splitString[0];
		String value = null;
		if (splitString.length == 2) {
			value = splitString[1].trim();
			String nextLine = this.lines.peekFirst();
			Matcher continueLineMatcher = nextLine != null ? continueLinePattern.matcher(nextLine) : null;
			Matcher continueNewlineMatcher = nextLine != null ? continueNewlinePattern.matcher(nextLine) : null;
			boolean lastLineIsNewLine = false;
			while (nextLine != null && (continueLineMatcher.matches() || continueNewlineMatcher.matches())) {
				if (continueLineMatcher.matches() && ! continueNewlineMatcher.matches()) {
					if (! lastLineIsNewLine) value += " ";
					value += continueLineReplacePattern.matcher(nextLine).replaceAll("");
					lastLineIsNewLine = false;					
				} else {
					value += "\n";
					lastLineIsNewLine = true;
				}
				
				this.lines.removeFirst();
				nextLine = this.lines.peekFirst();
				continueLineMatcher = nextLine != null ? continueLinePattern.matcher(nextLine) : null;
				continueNewlineMatcher = nextLine != null ? continueNewlinePattern.matcher(nextLine) : null;				
			}
			while(! this.lines.isEmpty() && this.lines.getFirst().matches("^( |\\t)+.+$")) {
				value += " " + this.lines.removeFirst().replaceAll("^( |\\t)+", "");
			}			
		} else {
			throw new RuntimeException("Improperly formatted line: " + line);
		}
		//If ends in \n then trim
		if (value.endsWith("\n")) value = value.substring(0, value.length()-1);
		NameValue ret = new NameValue(name, value);
		log.debug(MessageFormat.format("Read from {0}: {1}", this.type, ret.toString()));
		return ret;
		
	}
	
	public void remove() {
		throw new UnsupportedOperationException();		
	}

}
