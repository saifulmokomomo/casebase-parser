
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.SimpleDateFormat;

import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

public class ToText {
	public static void main(String[] args) {

		if (args.length == 0 || args.length > 1) {
			System.out.println("Usage: java ToText <Directory>");
			System.exit(1);
		}
		File dir = new File(args[0]);
		if (!dir.exists()) {
			System.out.println("directory doesn't exists");
			System.exit(1);
		} else if (!dir.isDirectory()) {
			System.out.println("not a directory");
			System.exit(1);
		}
		try {
			File[] files = dir.listFiles();
			FileOutputStream toText = new FileOutputStream("toText.txt");
			for (File file : files) {
				if (!file.getName().matches("(.*).pdf")) continue;
				System.out.println(file.getName());
				try {
					//initialize sources
					FileInputStream fileInputStream = new FileInputStream(file);
					PDFParser parser = new PDFParser(fileInputStream);
					parser.parse();
					COSDocument cosDoc = parser.getDocument();
					PDDocument pdDoc = new PDDocument(cosDoc);
					PDFTextStripper pdfStripper = new PDFTextStripper();
					String content = pdfStripper.getText(pdDoc);
					//set header and footer for easy manipulation
					pdfStripper.setPageStart("\n-THIS IS A HEADER-");
					pdfStripper.setPageEnd("-THIS IS A FOOTER-");
					//string to be manipulated
					String parsedText = pdfStripper.getText(pdDoc);
					//release sources
					pdfStripper.resetEngine();
					pdDoc.close();
					cosDoc.close();
					parser.clearResources();
					fileInputStream.close();
					//prepare string header
					Pattern pattern = Pattern.compile("-THIS IS A HEADER-\\d");
					Matcher matcher = pattern.matcher(parsedText);
					matcher.find();
					int indexHeaderStart = parsedText.indexOf("-THIS IS A HEADER-[") < matcher.start() ? parsedText.indexOf("-THIS IS A HEADER-[") : matcher.start();
					int indexHeaderEnd = parsedText.indexOf("\n",indexHeaderStart);
					String tmpStr = parsedText.substring(indexHeaderStart,indexHeaderEnd);
					tmpStr = tmpStr.replace("-THIS IS A HEADER-","");

					toText.write(("========== SPLIT HERE ==========\n" + file.getName() + "\n").getBytes());
					toText.write((tmpStr + "\n").getBytes());

					//remove unnecessary initial page(s) (in some pdf)
					parsedText = parsedText.substring(indexHeaderStart);
					parsedText = parsedText.replaceAll("(.*)-THIS IS A FOOTER-\n","");
					parsedText = parsedText.replaceAll("(.*)-THIS IS A FOOTER-","");
					parsedText = parsedText.replaceAll("-THIS IS A HEADER-\\[(.*)\\d\n","");//odd page
					parsedText = parsedText.replaceAll("-THIS IS A HEADER-\\[(.*)\n(.*)\n(.*)\\d\n","");//odd page
					parsedText = parsedText.replaceAll("-THIS IS A HEADER-\\d(.*)\n","");//even page

					toText.write((parsedText + "\n").getBytes());
				} catch (IOException e) {
					System.out.println("Error I1: " + e.getMessage());
				}
			}//for (File file : files)
		} catch (FileNotFoundException e) {
			System.out.println("Error F: " + e.getMessage());
		}
		System.out.println("---ended");
	}
}
