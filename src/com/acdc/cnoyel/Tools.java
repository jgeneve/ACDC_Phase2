package com.acdc.cnoyel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import javax.swing.JDialog;

import com.acdc.component.DialogWaitingScreen;
import com.hexidec.ekit.Ekit;

public class Tools {

	private static Process process;
	
	/**
	 * Method that wait user to enter text
	 * 
	 * @return userInput - String entered by the user
	 * @throws IOException on invalid user input
	 */
	public static String getStringUserInput() {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {
			return br.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Method that returns a list of elements separated by a separator in a String
	 * 
	 * @param str - String with elements separated with a separator
	 * @param separator - String to use to split elements
	 * @return list - List<String> elements from the input string split by separator
	 */
	public static List<String> stringToList(String str, String separator) {
		List<String> list = new ArrayList<>();
		list = new ArrayList<String>();
		
		if (str.isEmpty()) {
			return list;
		}
		
		String[] arrayLink = str.split(separator);
		for(int i=0; i<arrayLink.length; i++) {
			list.add(arrayLink[i]);
		}
		return list;
	}

	/** Create a file into the '_post' directory
	 * @param markdownString - String containing all the markdown to insert in the file
	 * @param filePath - String of the filepath to create
	 * @return file - File containing the generated markdown
	 * @throws IOException
	 */
	public static File createMarkdownFile(String markdownString, String filePath) {
		File file = new File(filePath);
		BufferedWriter output;
		try {
			output = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8);
			output.write(markdownString);
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return file;
	}

	private static class StreamGobbler implements Runnable {
	    private InputStream inputStream;
	    private InputStream errorStream;
	    private Consumer<String> consumer;
	    public StreamGobbler(InputStream inputStream, Consumer<String> consumer, InputStream errorStream) {
	        this.inputStream = inputStream;
	        this.consumer = consumer;
	        this.errorStream = errorStream;
	    }		 
	    @Override
	    public void run() {
	        new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(consumer);
	    }
	}
		
	/**
	 * Method to execute a command depending of the OS
	 * @param commande - String of the command to execute
	 * @param path - String of the path where the command should be executed
	 * @param stopThread - Boolean used to know if we need to stop the process by ourself or not 
	 */
	public static void executeCmd(String commande, String path) {
		final boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");	// To verify if the OS is windows or another
		ProcessBuilder builder = new ProcessBuilder();
		if (isWindows) {
			builder.command("cmd.exe", "/c", commande);	
		} else {
			builder.command("sh", "-c", commande);
		}
		builder.directory(new File(path));
		Process processCmd = null;
		try {
			processCmd = builder.start();
			StreamGobbler streamGobbler = new StreamGobbler(processCmd.getInputStream(), System.out::println, processCmd.getErrorStream());		
			Executors.newSingleThreadExecutor().submit(streamGobbler); 
			processCmd.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		processCmd.destroy();
	}
	
	public static void killJekyll() {
		if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
			executeCmd("TASKKILL -F -IM ruby.exe", PropertiesAccess.getInstance().getLocalRepository());
		} else {
			process.destroyForcibly();
		}
	}
	
	/**
	 * Method that runs the commit and push commands of Git
	 * @param githubDirectory : String - link to the remote git adress
	 * @param gitDirectory : String - link to the local website
	 */
	public static void gitCommitAndPush(String localDirectory) {
		Tools.executeCmd("git add .", localDirectory);
		Tools.executeCmd("git commit -m \"Add markdown file\"", localDirectory);
		Tools.executeCmd("git push origin master", localDirectory);
	}
	
	public static void launchServer() {
		boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
		
		ProcessBuilder builder = new ProcessBuilder();
		if (isWindows) {
			builder.command("cmd.exe", "/c", "bundle exec jekyll serve -o");
		} else {
			builder.command("sh", "-c", "bundle exec jekyll serve -o", ">&-");
		}
		builder.directory(new File(PropertiesAccess.getInstance().getLocalRepository()));
		
		try {
			process = builder.start();
			Thread launchWebsite = new Thread(new LaunchWebsite(process));
			launchWebsite.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String deAccent(String str) {
	    String nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD); 
	    Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
	    return pattern.matcher(nfdNormalizedString).replaceAll("");
	}
	
	public static void copyFile(String pSource, String pDestination) {
		try {
			Path source = Paths.get(pSource);
			Path destination = Paths.get(pDestination);
			Files.copy(source, destination);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

