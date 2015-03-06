package common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class SaveInfo {
	File outFile;
	BufferedWriter writer;

	public File open(String filename) {
	    try {
		    outFile = new File(filename); // File to write to
			writer = new BufferedWriter(new FileWriter(outFile));

		} catch (IOException e) {
			System.err.println(e);
//			System.exit(1);
		}

		return outFile;
	}

	public synchronized void  writeLine(String line) {
		System.out.println(line);
		try {
			writer.write(line+"\r\n");

		} catch (IOException e) {
			System.err.println(e);
		}
	}
	
	public synchronized void  write(String str) {
		System.out.print(str);
		try {
			writer.write(str);
		} catch (IOException e) {
			System.err.println(e);
		}
	}

	public void close() {
		try {
			writer.close(); 

		} catch (IOException e) {
			System.err.println(e);
//			System.exit(1);
		}
	}

	public static void main(String args[]) {
		SaveInfo saveInfo = new SaveInfo();
		saveInfo.open("./aa.txt");
		saveInfo.writeLine("hello");
		saveInfo.writeLine("world");
		saveInfo.close();
}
}
