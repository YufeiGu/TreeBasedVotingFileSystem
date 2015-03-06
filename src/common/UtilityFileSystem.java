package common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.Random;

public class UtilityFileSystem {
	/**
	 * get local host name
	 * 
	 * @return
	 */
	public static String getHostname() {
		String hostname = "";
		try {
			InetAddress ownIP = InetAddress.getLocalHost();
			hostname = ownIP.getHostName();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return hostname;
	}

	/**
	 * get random integer between min and max
	 * 
	 * @param min
	 * @param max
	 * @return
	 */
	public static int createRandom(int min, int max) {
		int s = -1;
		Random random = new Random();
		if (min > 0) {
			s = random.nextInt(max) % (max - min + 1) + min;
		} else if (min == 0) {
			min++;
			max++;
			s = random.nextInt(max) % (max - min + 1) + min - 1;
		}
		return s;
	}

	/**
	 * get current directory
	 * @return
	 */
	public static String getCurrentDir(){
		java.io.File dir1 = new java.io.File(".");
		String currentDir = null;
		try {
			currentDir = dir1.getCanonicalPath();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return currentDir;
	}
	
	/**
	 * read file to String
	 * @param fileName
	 * @return
	 */
	public static String readFile(String fileName) {
		String fileContent =null;
		try {
			FileInputStream fstream = new FileInputStream(fileName);
			fileContent = "";
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			// Read File Line By Line
			while ((strLine = br.readLine()) != null) {
				fileContent = fileContent + strLine + "\r\n";
			}
			// Close the input stream
			in.close();
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}
		return fileContent;
	}
	
	
	/**
	 * save string to  file
	 * @param fileName
	 * @return
	 */
	public static void saveString2File(String filename,String content) {
		try {
			java.io.File outFile;
			BufferedWriter writer;
			outFile = new java.io.File(filename); // File to write to
			writer = new BufferedWriter(new FileWriter(outFile));
			writer.write(content);
			writer.close();
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}
	}
	
	/**
	 * get file list by directory name 
	 * @param dirName
	 * @return
	 */
	public static String[] getFileList(String dirName){
		java.io.File dir = new java.io.File(dirName);
		return dir.list();
	}
	
	/**
	 * for test
	 * @param args
	 */
	public static void main(String args[]) {
		
		 getFileList(getCurrentDir()+"/files/");
	}
	
	
}
