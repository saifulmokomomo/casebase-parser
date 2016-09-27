import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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

public class CaseBaseFromPDFLaw {
	public static void main(String[] args) {

		if (args.length == 0 || args.length > 2) {
			System.out.println("Usage: java PDFTextParser <Directory> [-remote]");
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
		boolean remote = args.length > 1 && args[1].equals("-remote") ? true : false;
		int numFile = 0;
		int numFailed = 0;
		try {
			FileOutputStream logFile = remote ? new FileOutputStream("R_" + dir.getName() + ".log") : new FileOutputStream(dir.getName() + ".log");
			//Session session;
			//JSch jsch = new JSch();
			//session = jsch.getSession("","");
			Connection con;
			if (remote) {
				/*
				String strSshUser = "rails";          // SSH loging username
				String strSshPassword = "fNkwVGo5KQ"; // SSH login password
				String strSshHost = "128.199.138.153";// hostname or ip or SSH server
				int nSshPort = 22;                    // remote SSH host port number
				String strRemoteHost = "127.0.0.1";   // hostname or ip of your database server
				int nLocalPort = 3366;                // local port number use to bind SSH tunnel
				int nRemotePort = 3306;               // remote port number of your database
				String strDbUser = "rails";           // database loging username
				String strDbPassword = "r5PmAYJ4YA";  // database login password
				session = jsch.getSession( strSshUser, strSshHost, 22 );
				session.setPassword( strSshPassword );
				Properties config = new Properties();
				config.put( "StrictHostKeyChecking", "no" );
				session.setConfig( config );
				session.connect();
				session.setPortForwardingL(nLocalPort, strRemoteHost, nRemotePort);
				Class.forName("com.mysql.jdbc.Driver");
				con = DriverManager.getConnection("jdbc:mysql://localhost:"+nLocalPort+"/intellex_development", strDbUser, strDbPassword);
				*/
				MysqlDataSource mysqlds = new MysqlDataSource();
				mysqlds.setUser("root");
				mysqlds.setPassword("Intelll3x");
				mysqlds.setURL("jdbc:mysql://52.77.234.223:3306/intellex_development");
				con = mysqlds.getConnection();
			} else {
				MysqlDataSource mysqlds = new MysqlDataSource();
				mysqlds.setUser("intelllex");
				mysqlds.setPassword("Intelll3x");
				mysqlds.setURL("jdbc:mysql://127.0.0.1:3306/intellex_development");
				con = mysqlds.getConnection();
			}
			File[] files = dir.listFiles();
			for (File file : files) {
				if (!file.getName().matches("(.*).pdf")) continue;
				numFile++;
				String strOut = "\n" + numFile + "--" + file.getName() + "--";
				logFile.write((strOut + "\n").getBytes());
				System.out.println(strOut);
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
					//prepare string header for table reports
					Pattern pattern = Pattern.compile("-THIS IS A HEADER-\\d");
					Matcher matcher = pattern.matcher(parsedText);
					matcher.find();
					int indexHeaderStart = parsedText.indexOf("-THIS IS A HEADER-[") < matcher.start() ? parsedText.indexOf("-THIS IS A HEADER-[") : matcher.start();
					int indexHeaderEnd = parsedText.indexOf("\n",indexHeaderStart);

					String[] arrTmpStr;
					String tmpStr = parsedText.substring(indexHeaderStart,indexHeaderEnd);
					tmpStr = tmpStr.replace("-THIS IS A HEADER-","");
					//-- preparing values
					// table reports
					int idxRptYearS = tmpStr.indexOf("[");
					int idxRptYearE = tmpStr.indexOf("]") + 1;
					String reportsYear = tmpStr.substring(idxRptYearS,idxRptYearE);
					reportsYear = "'" + reportsYear.trim() + "'";

					int idxRptVolS = idxRptYearE + 1;
					int idxRptVolE = idxRptVolS + 1;//report's volume has only 1 digit
					String reportsVol = tmpStr.substring(idxRptVolS,idxRptVolE);
					reportsVol = "'" + reportsVol.trim() + "'";
					if (!Character.isDigit(reportsVol.charAt(1))) reportsVol = "null";

					int idxRptAbbrS = reportsVol.equals("null") ? idxRptVolS + 1 : idxRptVolE + 1;
					int idxRptAbbrE = tmpStr.charAt(0) == '[' ? tmpStr.indexOf(" ",idxRptAbbrS) : tmpStr.length();
					String reportsAbbr = tmpStr.substring(idxRptAbbrS,idxRptAbbrE);
					reportsAbbr = "'" + reportsAbbr.trim() + "'";

					int idxRptNumS = tmpStr.charAt(0) == '[' ?  tmpStr.lastIndexOf(" ") : 0;
					int idxRptNumE = tmpStr.charAt(0) == '[' ? tmpStr.length() : tmpStr.indexOf(" ");
					String reportsNum = tmpStr.substring(idxRptNumS,idxRptNumE);
					reportsNum = "'" + reportsNum.trim() + "'";

					//remove unnecessary initial page(s) (in some pdf)
					parsedText = parsedText.substring(indexHeaderStart);
					//remove header and footer
					parsedText = parsedText.replaceAll("(.*)-THIS IS A FOOTER-\n","");
					parsedText = parsedText.replaceAll("(.*)-THIS IS A FOOTER-","");
					parsedText = parsedText.replaceAll("-THIS IS A HEADER-\\[(.*)\\d\n","");//odd page
					parsedText = parsedText.replaceAll("-THIS IS A HEADER-\\[(.*)\n(.*)\n(.*)\\d\n","");//odd page
					parsedText = parsedText.replaceAll("-THIS IS A HEADER-\\d(.*)\n","");//even page

					// table cases
					tmpStr = parsedText.substring(0,parsedText.indexOf("["));
					arrTmpStr = tmpStr.split("v \n",2);

					String party1 = "'" + arrTmpStr[0].trim() + "'";
					if (party1.length() < 3 || party1.length() > 257) throw new Exception("case party1 : " + party1);
					String party2 = arrTmpStr.length > 1 ? "'" + arrTmpStr[1].trim() + "'" : "null";
					if (party1.length() > 257) throw new Exception("case party2 : " + party1);

					parsedText = parsedText.substring(parsedText.indexOf("["));
					tmpStr = parsedText.substring(0,parsedText.indexOf("\n"));
					arrTmpStr = tmpStr.split(" ");

					String year = "'" + arrTmpStr[0].substring(1,arrTmpStr[0].length()-1) + "'";
					if (!year.matches("'\\d+'")) throw new Exception("case year : " + year);
					String courtAbbr = arrTmpStr.length > 1 ? "'" + arrTmpStr[1].trim() + "'" : "null";
					if (!courtAbbr.matches("'SG(.*)'") || courtAbbr.equals("null")) throw new Exception("case courtAbbr : " + courtAbbr);
					String number = arrTmpStr.length > 2 ? "'" + arrTmpStr[2].trim() + "'" : "null";
					if (!number.matches("'\\d+'")) throw new Exception("case number : " + number);

					PreparedStatement pstmt = con.prepareStatement(
					"SELECT COUNT(*) FROM cases WHERE description1 = " + party1 + " AND court_abbreviation = " + courtAbbr + " AND year = " + year + " AND number = " + number);
					pstmt.execute();
					ResultSet rs = pstmt.getResultSet();
					rs.next();
					boolean duplicate = rs.getInt(1) > 0;
					rs.close();
					pstmt.close();
					//duplicate = false;
					if (duplicate) {
						System.out.println("Duplicate Case: " + file.getName());
						logFile.write("\n########## DUPLICATE CASE ##########\n".getBytes());
						logFile.write(("--- " + file.getName() + " ---\n\n").getBytes());
						File duplicateDir = new File(dir.getName() + "/duplicate");
						duplicateDir.mkdir();
						file.renameTo(new File(dir.getName() + "/duplicate/" + file.getName()));
					}	else {
						// table identifiers
						parsedText = parsedText.substring(parsedText.indexOf("\n")+1);
						arrTmpStr = parsedText.split("\\n");
						tmpStr = arrTmpStr[0];
						tmpStr = tmpStr.replaceFirst("Court of(.*)Appeal —","");
						tmpStr = tmpStr.replaceFirst("High Court —","");
						int next = 1;
						while (arrTmpStr[next].matches("(.*)\\p{Digit}(.*)")) {
							tmpStr = tmpStr.concat(" " + arrTmpStr[next]);
							next++;
						}
						String judgement = "'" + arrTmpStr[next].trim() + "'";
						if (judgement.length() < 3 || judgement.length() > 257) throw new Exception("case judgement : " + judgement);
						arrTmpStr = tmpStr.matches("(.*);(.*)") ? tmpStr.split(";") : tmpStr.split("\\(");
						String idenCaseNum = "'" + arrTmpStr[0].trim() + "'";
						idenCaseNum = idenCaseNum.replaceAll("[()]","");
						if (idenCaseNum.length() < 3 || idenCaseNum.length() > 257) throw new Exception("case idenCaseNum : " + idenCaseNum);
						String idenAppNum = arrTmpStr.length > 1 ? "'" + arrTmpStr[1] + "'" : "null";
						idenAppNum = idenAppNum.replaceAll("[()]","");
						if (idenAppNum.length() > 257) throw new Exception("case idenAppNum : " + idenAppNum);

						// table laws issues
						pattern = Pattern.compile("\\d(.*) \\p{Upper}\\p{Lower}(.*) \\d{4}\n");
						matcher = pattern.matcher(parsedText);
						matcher.find();
						tmpStr = parsedText.substring(matcher.start(),parsedText.indexOf("Facts\n"));
						arrTmpStr = tmpStr.split("\\n");
						String[] arrLaws = {"Abuse of Process — ",
						"Administrative Law — ",
						"Admiralty and Shipping — ",
						"Agency — ",
						"Arbitration — ",
						"Bailment — ",
						"Banking — ",
						"Betting, Gaming and Lotteries — ",
						"Bills of Exchange and other Negotiable Instruments — ",
						"Building and Construction Law — ",
						"Carriage of Goods by Air and Land — ",
						"Charities — ",
						"Choses in Action — ",
						"Civil Procedure",
						"Civil Procedure and Sentencing — ",
						"Civil Procedures — ",
						"Commercial Transactions — ",
						"Companies — ",
						"Conflict of Laws — ",
						"Constitutional Law — ",
						"Contempt of Court — ",
						"Contract — ",
						"Copyright — ",
						"Courts and Jurisdiction — ",
						"Credit and Security — ",
						"Criminal — ",
						"Criminal Law — ",
						"Criminal Procedure and Sentencing — ",
						"Damage — ",
						"Damages — ",
						"Debt — ",
						"Debt and Recovery — ",
						"Deeds and Other Instruments — ",
						"Designs — ",
						"Elections — ",
						"Employment Law — ",
						"English Law — ",
						"Equity — ",
						"Evidence — ",
						"Family Law — ",
						"Financial and Securities Markets — ",
						"Financial Markets and Securities — ",
						"Gifts — ",
						"Immigration — ",
						"Injunctions — ",
						"Inns and Innkeepers — ",
						"Insolvency — ",
						"Insolvency Law — ",
						"Insurance — ",
						"International Law — ",
						"Land — ",
						"Land Law — ",
						"Landlord and Tenant — ",
						"Legal Profession — ",
						"Limitation of Actions — ",
						"Limitations of Actions — ",
						"Mental Disorders and Treatment — ",
						"Military Law — ",
						"Muslim Law — ",
						"Partnership — ",
						"Patents and Invention — ",
						"Patents and Inventions — ",
						"Personal Property — ",
						"Planning Law — ",
						"Probate and Administration — ",
						"Professions — ",
						"Provident Fund — ",
						"Res judicata — ",
						"Restitution — ",
						"Revenue — ",
						"Revenue Law — ",
						"Road Traffic — ",
						"Sheriffs and Bailiffs — ",
						"Statutory Interpretation — ",
						"Succession and Wills — ",
						"Time — ",
						"Tort — ",
						"Tracing — ",
						"Trade Marks and Trade Names — ",
						"Trust — ",
						"Trusts — ",
						"Unincorporated Associations and Trade Unions — ",
						"Words — ",
						"Words and Phrases — ",
						};
					List<String> laws = new ArrayList<String>();
					String tmpLaw = arrTmpStr[1].trim();
					//System.out.println(tmpLaw);
					for (int i = 2; i < arrTmpStr.length; i++) {
						for (String s : arrLaws) {
							if (arrTmpStr[i].trim().matches(s+"(.*)")) {
								laws.add(tmpLaw.trim());
								tmpLaw = "";
								break;
							}
						}
						tmpLaw = tmpLaw.equals("") ? arrTmpStr[i].trim() : tmpLaw.concat(" " + arrTmpStr[i].trim());
					}
					laws.add(tmpLaw.trim());

					// table references
					List<String> references = new ArrayList<String>();
					int refExists = parsedText.indexOf("Case(s) referred to");
					int legExists = parsedText.indexOf("Legislation referred to");
					if (refExists > -1) {
						parsedText = parsedText.substring(refExists);
						if (legExists > -1) {
							legExists = parsedText.indexOf("Legislation referred to");
							tmpStr = parsedText.substring(parsedText.indexOf("\n")+1, legExists);
							arrTmpStr = tmpStr.split("\\n");
							for (int i = 0; i < arrTmpStr.length; i++) {
								if (arrTmpStr[i].contains("(folld)") ||
								arrTmpStr[i].contains("(refd)") ||
								arrTmpStr[i].contains("(distd)") ||
								arrTmpStr[i].contains("(not folld)") ||
								arrTmpStr[i].contains("(overd)")) {
									references.add(arrTmpStr[i].trim());
								} else {
									String tStr = "";
									while(!arrTmpStr[i].contains("(folld)") &&
									!arrTmpStr[i].contains("(refd)") &&
									!arrTmpStr[i].contains("(distd)") &&
									!arrTmpStr[i].contains("(not folld)") &&
									!arrTmpStr[i].contains("(overd)")) {
										tStr = tStr.concat(" " + arrTmpStr[i]);
										tStr = tStr.trim();
										i++;
										if (i >= arrTmpStr.length) break;
									}
									if (i < arrTmpStr.length) tStr = tStr.concat(" " + arrTmpStr[i]);
									references.add(tStr.trim());
								}
							}
						} else {
							tmpStr = parsedText.substring(parsedText.indexOf("\n")+1);
							arrTmpStr = tmpStr.split("\\n");
							for (int i = 0; i < arrTmpStr.length; i++) {
								if (arrTmpStr[i].contains("(folld)") ||
								arrTmpStr[i].contains("(refd)") ||
								arrTmpStr[i].contains("(distd)") ||
								arrTmpStr[i].contains("(not folld)") ||
								arrTmpStr[i].contains("(overd)")) {
									references.add(arrTmpStr[i].trim());
								} else {
									String tStr = "";
									int stop = 0;
									while(!arrTmpStr[i].contains("(folld)") &&
									!arrTmpStr[i].contains("(refd)") &&
									!arrTmpStr[i].contains("(distd)") &&
									!arrTmpStr[i].contains("(not folld)") &&
									!arrTmpStr[i].contains("(overd)")) {
										tStr = tStr.concat(" " + arrTmpStr[i]);
										tStr = tStr.trim();
										i++;
										stop++;
										if (i >= arrTmpStr.length || stop > 5) break;
									}
									if (i < arrTmpStr.length) tStr = tStr.concat(" " + arrTmpStr[i]);
									if (tStr.contains("(folld)") ||
									tStr.contains("(refd)") ||
									tStr.contains("(distd)") ||
									tStr.contains("(not folld)") ||
									tStr.contains("(overd)")) {
										references.add(tStr.trim());
									}
								}
							}
						}
					}

					//table legislations
					List<String> legislations = new ArrayList<String>();
					if (legExists > -1) {
						parsedText = parsedText.substring(legExists);
						tmpStr = parsedText.substring(parsedText.indexOf("\n")+1);
						arrTmpStr = tmpStr.split("\\n");
						for (int i = 0; i < arrTmpStr.length; i++) {
							if(!arrTmpStr[i].matches("(.*)\\p{Digit}(.*)")) break;
							if (arrTmpStr[i].trim().matches("\\p{Upper}\\p{Lower}\\p{Lower}(.*)") ||
							arrTmpStr[i].trim().matches("\\p{Upper}\\p{Lower}-(.*)")) {
								legislations.add(arrTmpStr[i].trim());
							} else {
								int index = legislations.size()-1;
								String tStr = legislations.get(index).concat(arrTmpStr[i]);
								legislations.set(index,tStr.trim());
							}
						}
					}

					//-- preparing statement
					Statement sqlStmt = con.createStatement();
					String stmt = "SET @d = now()";
					sqlStmt.addBatch(stmt);
					// table cases
					stmt = "INSERT cases (case_jurisdiction,description1,description2,court_abbreviation,year,number,judgment,status,last_mod_by,created_at,updated_at)" +
					"VALUES ('SG'," + party1 + "," + party2 + "," + courtAbbr + "," + year + "," + number + "," + judgement + ",'draft',17,@d,@d)";
					sqlStmt.addBatch(stmt);
					stmt = "SET @caseId = LAST_INSERT_ID()";
					sqlStmt.addBatch(stmt);
					strOut = "party1 : " + party1 + "\nparty2 : " + party2 + "\ncourtAbbr : " + courtAbbr + "\nyear : " + year + "\nnumber : " + number + "\njudgement : " + judgement;
					logFile.write((strOut + "\n").getBytes());
					//System.out.println(strOut);
					// table report
					stmt = "INSERT `case-citations_reported` (case_id,year,volume,abbreviation,number,created_at,updated_at)" +
					"VALUES (@caseId," + reportsYear + "," + reportsVol + "," + reportsAbbr + "," + reportsNum + ",@d,@d)";
					sqlStmt.addBatch(stmt);
					strOut = "report :>\n  year : " + reportsYear + "\n  volume : " + reportsVol + "\n  abbr : " + reportsAbbr + "\n  number : " + reportsNum;
					logFile.write((strOut + "\n").getBytes());
					//System.out.println(strOut);
					// table identifiers
					stmt = "INSERT `case-identifier` (case_id,matter_number,application_number,created_at,updated_at)" +
					"VALUES (@caseId," + idenCaseNum + "," + idenAppNum + ",@d,@d)";
					sqlStmt.addBatch(stmt);
					strOut = "identifiers :>\n  caseNum : " + idenCaseNum + "\n  appNum : " + idenAppNum;
					logFile.write((strOut + "\n").getBytes());
					//System.out.println(strOut);
					// table laws & issues
					strOut = "Laws & Issues :";
					logFile.write((strOut + "\n").getBytes());
					String prevLaw = "";
					for (String s : laws) {
						if (!s.substring(0,s.indexOf(" — ")).equals(prevLaw)) {
							stmt = "INSERT `case-AoL` (case_id,description,created_at,updated_at)" +
							"VALUES (@caseId,'" + s.substring(0,s.indexOf(" — ")) + "',@d,@d)";
							sqlStmt.addBatch(stmt);
							stmt = "SET @lawId = LAST_INSERT_ID()";
							sqlStmt.addBatch(stmt);
						}
						stmt = "INSERT `case-issue` (law_id,description,created_at,updated_at)" +
						"VALUES (@lawId,'" + s.substring(s.indexOf(" — ") + 3) + "',@d,@d)";
						sqlStmt.addBatch(stmt);
						prevLaw = s.substring(0,s.indexOf(" — "));
						strOut = "  law : " + s.substring(0,s.indexOf(" — ")) + "\n" +
						"  issue : " + s.substring(s.indexOf(" — ") + 3);
						logFile.write((strOut + "\n").getBytes());
					}
					// table references
					strOut = "references :> " + refExists ;
					logFile.write((strOut + "\n").getBytes());
					//System.out.println(strOut);
					for (String s : references) {
						strOut = s;
						logFile.write((strOut + "\n").getBytes());
						//System.out.println(strOut);
						int[] indexPTemp = {s.indexOf("(1"),s.indexOf("[1"),s.indexOf("(20"),s.indexOf("[20")};
						Arrays.sort(indexPTemp);
						int indexP = 0;
						for (int i : indexPTemp) {
							if (i > 0) {
								indexP = i;
								break;
							}
						}
						if (indexP == 0) indexP = s.indexOf("(");
						tmpStr = s.substring(0,indexP);
						arrTmpStr = tmpStr.split(" v ");
						String refParty1 = "\"" + arrTmpStr[0] + "\"";
						if (refParty1.length() < 3 || refParty1.length() > 257) throw new Exception("ref party1 : " + refParty1);
						String refParty2 = arrTmpStr.length > 1 ? "\"" + arrTmpStr[1] + "\"" : "null";
						if (refParty2.length() > 257) throw new Exception("ref party2 : " + refParty2);
						int indexT = s.length();
						if (s.indexOf("(folld)") > -1)
						indexT = s.indexOf("(folld)");
						else	if (s.indexOf("(refd)") > -1)
						indexT = s.indexOf("(refd)");
						else	if (s.indexOf("(distd)") > -1)
						indexT = s.indexOf("(distd)");
						else	if (s.indexOf("(not folld)") > -1)
						indexT = s.indexOf("(not folld)");
						else	if (s.indexOf("(overd)") > -1)
						indexT = s.indexOf("(overd)");
						String refTreatment = "'" + s.substring(indexT) + "'";
						if (!refTreatment.matches("'\\((.*)d\\)'") && !refTreatment.matches("''")) throw new Exception("ref treatment : " + refTreatment);
						refTreatment = refTreatment.replace("(","");
						refTreatment = refTreatment.replace(")","");
						if (refTreatment.equals("'folld'"))
						refTreatment = "'folld/appld'";
						stmt = "INSERT `case-ref_case` (case_id,description1,description2,treatment,non_sg_citation,created_at,updated_at)" +
						"VALUES (@caseId," + refParty1 + "," + refParty2 + "," + refTreatment + ",0,@d,@d)";
						sqlStmt.addBatch(stmt);
						stmt = "SET @refId = LAST_INSERT_ID()";
						sqlStmt.addBatch(stmt);
						strOut = "  party1 : " + refParty1 + "\n  party2 : " + refParty2 + "\n  treatment : " + refTreatment;
						logFile.write((strOut + "\n").getBytes());
						//System.out.println(strOut);

						tmpStr = s.substring(indexP,indexT);
						String[] citations = tmpStr.split(";");
						strOut = "  citations:>";
						logFile.write((strOut + "\n").getBytes());
						//System.out.println(strOut);
						for (String ss : citations) {
							if (ss.length() > 257) throw new Exception("ref citation : " + ss);
							// reference's citations
							stmt = "INSERT `case-ref_citation` (ref_case_id,year,created_at,updated_at)" +
							"VALUES (@refId,\"" + ss.trim() + "\",@d,@d)";
							sqlStmt.addBatch(stmt);
							strOut = "    year : " + ss.trim() ;
							logFile.write((strOut + "\n").getBytes());
							//System.out.println(strOut);
						}
					}//for (String s : references)
					// table legislations & sections
					strOut = "legislations :> " + legExists ;
					logFile.write((strOut + "\n").getBytes());
					//System.out.println(strOut);
					for (String s : legislations) {
						strOut = s;
						logFile.write((strOut + "\n").getBytes());
						//System.out.println(strOut);
						int indexA = s.indexOf("(") < 0 ? s.indexOf(",") : s.indexOf("(");
						if (s.indexOf(",") > -1 && s.indexOf(",") < indexA) indexA = s.indexOf(",");
						if (indexA < 0) indexA = 0;
						String legAct = "\"" + s.substring(0,indexA).trim() + "\"";
						if (legAct.length() > 257) throw new Exception("legAct : " + legAct);
						int indexS = s.indexOf("(") < 0 ? s.length()-1 : s.indexOf(")",indexA) + 1;
						String citation = "'" + s.substring(indexA,indexS).trim() + "'";
						if (citation.length() > 257) throw new Exception("leg citation : " + citation);
						String section = indexS > -1 ? "'" + s.substring(indexS).trim() + "'" : "null";

						stmt = "INSERT `case-ref_legis` (case_id,account_name,citation,created_at,updated_at)" +
						"VALUES (@caseId," + legAct + "," + citation + ",@d,@d)";
						sqlStmt.addBatch(stmt);
						stmt = "SET @legId = LAST_INSERT_ID()";
						sqlStmt.addBatch(stmt);
						strOut = "  legAct : " + legAct + "\n  citation : " + citation;
						logFile.write((strOut + "\n").getBytes());
						//System.out.println(strOut);
						// legislation's section
						String treatment = section.indexOf("(consd)") < 0 ? "''" : "'consd'";
						stmt = "INSERT `case-ref_section` (ref_legi_id,section,treatment,created_at,updated_at)" +
						"VALUES (@legId," + section + "," + treatment + ",@d,@d)";
						sqlStmt.addBatch(stmt);
						strOut = "    sections : " + section;
						logFile.write((strOut + "\n").getBytes());
						//System.out.println(strOut);
					}//for (String s : legislations)
					System.out.println("executing query...");
					sqlStmt.executeBatch();
					sqlStmt.close();
					//update case's content
					pstmt = con.prepareStatement("SELECT id FROM cases ORDER BY id DESC LIMIT 1");
					pstmt.execute();
					rs = pstmt.getResultSet();
					rs.next();
					int caseID = rs.getInt(1);
					rs.close();
					pstmt.close();
					pstmt = con.prepareStatement("UPDATE cases SET content = ? WHERE id = ?");
					pstmt.setString(1, content);
					pstmt.setInt(2,caseID);
					pstmt.executeUpdate();
					pstmt.close();
				} //not duplicated
			} catch (IOException e) {
				System.out.println("Error I1: " + e.getMessage());
				logFile.write("\n########## IO FAILED ##########\n".getBytes());
				logFile.write(("--- " + file.getName() + " ---\n\n").getBytes());
				File failedDir = new File("failed");
				failedDir.mkdir();
				failedDir = new File("failed/" + dir.getName());
				failedDir.mkdir();
				file.renameTo(new File("failed/" + dir.getName() + "/" + file.getName()));
				numFailed++;
			} catch (SQLException e) {
				System.out.println("Error S1: " + e.getMessage());
				logFile.write("\n########## QUERY FAILED ##########\n".getBytes());
				logFile.write(("--- " + file.getName() + " ---\n\n").getBytes());
				File failedDir = new File("failed");
				failedDir.mkdir();
				failedDir = new File("failed/" + dir.getName());
				failedDir.mkdir();
				file.renameTo(new File("failed/" + dir.getName() + "/" + file.getName()));
				numFailed++;
			} catch (Exception e) {
				System.out.println("Error E1: " + e.getMessage());
				logFile.write("\n########## VALUES VALIDATION FAILED ##########\n".getBytes());
				logFile.write(("--- " + file.getName() + " ---\n").getBytes());
				logFile.write(("########## " + e.getMessage() + " ##########\n\n").getBytes());
				File failedDir = new File("failed");
				failedDir.mkdir();
				failedDir = new File("failed/" + dir.getName());
				failedDir.mkdir();
				file.renameTo(new File("failed/" + dir.getName() + "/" + file.getName()));
				numFailed++;
			}
		}//for (File file : files)
		if (!con.isClosed()) con.close();
		//if (remote && session.isConnected()) session.disconnect();
		logFile.close();
	} catch (SQLException e) {
		System.out.println("Error S: " + e.getMessage());
		numFile--;
	} catch (FileNotFoundException e) {
		System.out.println("Error F: " + e.getMessage());
		numFile--;
	} catch (IOException e) {
		System.out.println("Error I: " + e.getMessage());
		numFile--;
	}/* catch (JSchException e) {
		System.out.println("Error J: " + e.getMessage());
		numFile--;
	} catch (ClassNotFoundException e) {
		System.out.println("Error J: " + e.getMessage());
		numFile--;
	}*/
	System.out.println("total file executed : " + numFile);
	System.out.println("total file failed : " + numFailed);
}
}
