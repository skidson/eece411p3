package ca.ubc.ece.nio.crawler;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class SteveDriver {

	public static void main(String[] args) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter("lolol.txt"));
			bw.write("ur mom");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
