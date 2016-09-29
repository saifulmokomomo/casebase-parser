import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;

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

public class CaseBaseFromText {
	public static void main(String[] args) {

		if (args.length == 0 || args.length > 2) {
			System.out.println("Usage: java CaseBaseFromText <Directory> [-remote]");
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
		boolean remote = (args.length > 1 && args[1].equals("-remote"));
		int numFile = 0;
		int numFailed = 0;
		try {
			String strLogFile = remote ? dir.getName() + "/remote.log" : dir.getName() + "/local.log";
			FileOutputStream logFile = new FileOutputStream(strLogFile);
			FileOutputStream duplicateFile = new FileOutputStream(dir.getName() + "/duplicate.log");
			FileOutputStream failedFile = new FileOutputStream(dir.getName() + "/failed.log");
			FileOutputStream listFile = new FileOutputStream(dir.getName() + "/listFile.log");
			Connection con;
			MysqlDataSource mysqlds = new MysqlDataSource();
			if (remote) {
				mysqlds.setUser("root");
				mysqlds.setPassword("Intelll3x");
				mysqlds.setURL("jdbc:mysql://52.77.234.223:3306/intellex_development");
			} else {
				mysqlds.setUser("intelllex");
				mysqlds.setPassword("Intelll3x");
				mysqlds.setURL("jdbc:mysql://127.0.0.1:3306/intellex_development");
			}
			con = mysqlds.getConnection();
			File[] files = dir.listFiles();
			for (File file : files) {
				if (!file.getName().matches("(.*).txt")) continue;
				String strAll = null;
				try {
					FileReader fileReader = new FileReader(file);
					BufferedReader br = new BufferedReader(fileReader);
					StringBuilder sb = new StringBuilder();
        	String line = br.readLine();
					while (line != null) {
        		sb.append(line + "\n");
            line = br.readLine();
          }
          strAll = sb.toString();
          br.close();
				}	catch(FileNotFoundException e) {

				}	catch(IOException e) {

				}
				String[] cases = strAll.split("\n========== SPLIT HERE ==========\n");
				for (String strCase : cases) {
					//System.out.println(strCase);
					numFile++;
					String fileName = strCase.substring(0,strCase.indexOf("\n"));
					String strOut = "\n" + numFile + "--" + fileName + "--";
					logFile.write((strOut + "\n").getBytes());
					System.out.println(strOut);
					strCase = strCase.substring(strCase.indexOf("\n")+1);//remove first line (pdf file name)
					String caseContent = strCase;
					try {
						//if (numFile < 3)
							//System.out.println(strCase);
						String tmpStr = strCase.substring(0,strCase.indexOf("\n"));
						// table case-citations_reported
						int idxRptYearS = tmpStr.indexOf("[");
						int idxRptYearE = tmpStr.indexOf("]") + 1;
						String reportsYear = "'" + tmpStr.substring(idxRptYearS,idxRptYearE).replaceAll("'", "\'").trim() + "'";

						int idxRptVolS = idxRptYearE + 1;
						int idxRptVolE = idxRptVolS + 1;//report's volume has only 1 digit
						String reportsVol = "'" + tmpStr.substring(idxRptVolS,idxRptVolE).replaceAll("'", "\'").trim() + "'";
						if (!Character.isDigit(reportsVol.charAt(1))) reportsVol = "null";

						int idxRptAbbrS = reportsVol.equals("null") ? idxRptVolS + 1 : idxRptVolE + 1;
						int idxRptAbbrE = tmpStr.charAt(0) == '[' ? tmpStr.indexOf(" ",idxRptAbbrS) : tmpStr.length();
						String reportsAbbr = "'" + tmpStr.substring(idxRptAbbrS,idxRptAbbrE).replaceAll("'", "\'").trim() + "'";

						int idxRptNumS = tmpStr.charAt(0) == '[' ?  tmpStr.lastIndexOf(" ") : 0;
						int idxRptNumE = tmpStr.charAt(0) == '[' ? tmpStr.length() : tmpStr.indexOf(" ");
						String reportsNum = "'" + tmpStr.substring(idxRptNumS,idxRptNumE).replaceAll("'", "\'").trim() + "'";
						strOut = "report :>\n  year : " + reportsYear + "\n  volume : " + reportsVol + "\n  abbr : " + reportsAbbr + "\n  number : " + reportsNum;
						logFile.write((strOut + "\n").getBytes());

						// table cases
						tmpStr = strCase.substring(strCase.indexOf("\n"),strCase.indexOf("[",strCase.indexOf("\n")));
						String[] arrTmpStr = tmpStr.split("v \n",2);

						String party1 = "'" + arrTmpStr[0].replaceAll("\n"," ").replaceAll("'", "\'").trim() + "'";
						//if (party1.length() < 3 || party1.length() > 257) throw new Exception("case party1 : " + party1);
						String party2 = arrTmpStr.length > 1 ? "'" + arrTmpStr[1].replaceAll("\n"," ").replaceAll("'", "\'").trim() + "'" : "null";
						//if (party2.length() > 257) throw new Exception("case party2 : " + party2);

						strCase = strCase.substring(strCase.indexOf("[",strCase.indexOf("\n")));
						tmpStr = strCase.substring(0,strCase.indexOf("\n"));
						arrTmpStr = tmpStr.split(" ");

						String year = "'" + arrTmpStr[0].substring(1,arrTmpStr[0].length()-1).replaceAll("'", "\'").trim() + "'";
						//if (!year.matches("'\\d+'")) throw new Exception("case year : " + year);
						String courtAbbr = arrTmpStr.length > 1 ? "'" + arrTmpStr[1].replaceAll("'", "\'").trim() + "'" : "null";
						String juris = "null";
						if (courtAbbr.matches("SG(.*)"))
							juris = "SG";
						else if (courtAbbr.matches("ML(.*)"))
							juris = "MY";
						//if (!courtAbbr.matches("SG(.*)") || courtAbbr.equals("null")) throw new Exception("case courtAbbr : " + courtAbbr);
						String number = arrTmpStr.length > 2 ? arrTmpStr[2].replaceAll("'", "\'").trim() : "null";
						//if (!number.matches("\\d+")) throw new Exception("case number : " + number);
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
							logFile.write(("--- " + fileName + " ---\n\n").getBytes());
							duplicateFile.write(("--- " + fileName + " ---\n\n").getBytes());
						}	else {
							// table identifiers
							strCase = strCase.substring(strCase.indexOf("\n")+1);
							arrTmpStr = strCase.split("\n");
							tmpStr = arrTmpStr[0];
							tmpStr = tmpStr.replaceFirst("Court of(.*)Appeal —","");
							tmpStr = tmpStr.replaceFirst("High Court —","");
							int next = 1;
							while (arrTmpStr[next].matches("(.*)\\p{Digit}(.*)")) {
								tmpStr = tmpStr.concat(" " + arrTmpStr[next]);
								next++;
							}
							String judgement = "'" + arrTmpStr[next].replaceAll("'", "\'").trim() + "'";
							strOut = "party1 : " + party1 + "\nparty2 : " + party2 + "\njuris : " + juris + "\ncourtAbbr : " + courtAbbr + "\nyear : " + year + "\nnumber : " + number + "\njudgement : " + judgement;
							logFile.write((strOut + "\n").getBytes());
							//if (judgement.length() < 3 || judgement.length() > 257) throw new Exception("case judgement : " + judgement);
							String[] arrTmpStr2 = tmpStr.matches("(.*);(.*)") ? tmpStr.split(";") : tmpStr.split("\\(");
							String idenCaseNum = "'" + arrTmpStr2[0].replaceAll("'", "\'").trim() + "'";
							idenCaseNum = idenCaseNum.replaceAll("[()]","");
							//if (idenCaseNum.length() < 3 || idenCaseNum.length() > 257) throw new Exception("case idenCaseNum : " + idenCaseNum);
							String idenAppNum = arrTmpStr2.length > 1 ? "'" + arrTmpStr2[1].replaceAll("'", "\'").trim() + "'" : "null";
							idenAppNum = idenAppNum.replaceAll("[()]","");
							//if (idenAppNum.length() > 257) throw new Exception("case idenAppNum : " + idenAppNum);
							strOut = "identifiers :>\n  caseNum : " + idenCaseNum + "\n  appNum : " + idenAppNum;
							logFile.write((strOut + "\n").getBytes());
							// table laws issues
							// remove date after judgment
							next++;
							Pattern pattern = Pattern.compile("\\d(.*) \\p{Upper}\\p{Lower}(.*) \\d{4}(.*)");// date after judgment
							Matcher matcher = pattern.matcher(arrTmpStr[next]);
							while (matcher.find()) {
								next++;
								matcher = pattern.matcher(arrTmpStr[next]);
							}
							arrTmpStr = Arrays.copyOfRange(arrTmpStr, next-1, Arrays.asList(arrTmpStr).indexOf("Facts"));
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
						strOut = "laws :>";
						logFile.write((strOut + "\n").getBytes());
						for (String law : laws) {
							strOut = "  - " + law;
							logFile.write((strOut + "\n").getBytes());
						}
						// table references
						List<String> references = new ArrayList<String>();
						int refExists = strCase.indexOf("Case(s) referred to\n");
						int legExists = strCase.indexOf("Legislation referred to\n");
						if (refExists > -1) {
							strCase = strCase.substring(refExists);
							legExists = strCase.indexOf("Legislation referred to\n");
							if (legExists > -1) {
								tmpStr = strCase.substring(strCase.indexOf("\n")+1, legExists);
								strOut = "raw references with legs :>\n" + tmpStr;
								logFile.write((strOut + "\n").getBytes());
								arrTmpStr = tmpStr.split("\n");
								for (int i = 0; i < arrTmpStr.length; i++) {
									String tStr = arrTmpStr[i];
									while(!arrTmpStr[i].contains("(folld)") &&
												!arrTmpStr[i].contains("(refd)") &&
												!arrTmpStr[i].contains("(distd)") &&
												!arrTmpStr[i].contains("(not folld)") &&
												!arrTmpStr[i].contains("(overd)")) {
										if (!tStr.equals(arrTmpStr[i]))
											tStr = tStr.concat(" " + arrTmpStr[i]);
										tStr = tStr.trim();
										i++;
										if (i >= arrTmpStr.length) {
											i--;
											break;
										}
									}
									if (!tStr.equals(arrTmpStr[i]))
										tStr = tStr.concat(" " + arrTmpStr[i]);
									references.add(tStr.trim());
								}
							} else {
								tmpStr = strCase.substring(strCase.indexOf("\n")+1);
								strOut = "raw references no legs :>";
								logFile.write((strOut + "\n").getBytes());
								arrTmpStr = tmpStr.split("\n");
								for (int i = 0; i < arrTmpStr.length; i++) {
									if (arrTmpStr[i].contains(" for ") || arrTmpStr[i].contains(" for")) break;
									String tStr = arrTmpStr[i];
									logFile.write((tStr + "\n").getBytes());
									while(!arrTmpStr[i].contains("(folld)") &&
												!arrTmpStr[i].contains("(refd)") &&
												!arrTmpStr[i].contains("(distd)") &&
												!arrTmpStr[i].contains("(not folld)") &&
												!arrTmpStr[i].contains("(overd)")) {
										if (!tStr.equals(arrTmpStr[i]))
											tStr = tStr.concat(" " + arrTmpStr[i]);
										tStr = tStr.trim();
										i++;
										if (i >= arrTmpStr.length) {
											i--;
											break;
										}
									}
									if (!tStr.equals(arrTmpStr[i]))
										tStr = tStr.concat(" " + arrTmpStr[i]);
									references.add(tStr.trim());
								}
							}
						}
						strOut = "references :>";
						logFile.write((strOut + "\n").getBytes());
						for (String ref : references) {
							strOut = "  - " + ref;
							logFile.write((strOut + "\n").getBytes());
						}
						//table legislations
						List<String> legislations = new ArrayList<String>();
						if (legExists > -1) {
							strCase = strCase.substring(legExists);
							tmpStr = strCase.substring(strCase.indexOf("\n")+1);
							strOut = "raw legislations :>";
							logFile.write((strOut + "\n").getBytes());
							arrTmpStr = tmpStr.split("\n");
							for (int i = 0; i < arrTmpStr.length; i++) {
								if (arrTmpStr[i].contains(" for ") || arrTmpStr[i].contains(" for")) break;
								logFile.write((arrTmpStr[i] + "\n").getBytes());
								if (arrTmpStr[i].trim().matches("\\p{Upper}\\p{Lower}\\p{Lower}(.*)") ||
										arrTmpStr[i].trim().matches("\\p{Upper}\\p{Upper}\\p{Upper}(.*)") ||
										arrTmpStr[i].trim().matches("\\p{Upper}\\p{Lower}-(.*)")) {
									legislations.add(arrTmpStr[i].trim());
								} else {
									int index = legislations.size()-1;
									String tStr = legislations.get(index).concat(arrTmpStr[i]);
									legislations.set(index,tStr.trim());
								}
							}
						}
						strOut = "legislations :>";
						logFile.write((strOut + "\n").getBytes());
						for (String leg : legislations) {
							strOut = "  - " + leg;
							logFile.write((strOut + "\n").getBytes());
						}
						//-- preparing statement
						Statement sqlStmt = con.createStatement();
						String stmt = "SET @d = now()";
						sqlStmt.addBatch(stmt);
						// table cases
						stmt = "INSERT cases (case_jurisdiction,description1,description2,court_abbreviation,year,number,judgment,status,last_mod_by,created_at,updated_at)" +
						"VALUES (" + juris + "," + party1 + "," + party2 + "," + courtAbbr + "," + year + "," + number + "," + judgement + ",'draft',17,@d,@d)";
						sqlStmt.addBatch(stmt);
						stmt = "SET @caseId = LAST_INSERT_ID()";
						sqlStmt.addBatch(stmt);
						//System.out.println(strOut);
						// table report
						stmt = "INSERT `case-citations_reported` (case_id,year,volume,abbreviation,number,created_at,updated_at)" +
						"VALUES (@caseId," + reportsYear + "," + reportsVol + "," + reportsAbbr + "," + reportsNum + ",@d,@d)";
						sqlStmt.addBatch(stmt);
						//System.out.println(strOut);
						// table identifiers
						stmt = "INSERT `case-identifier` (case_id,matter_number,application_number,created_at,updated_at)" +
						"VALUES (@caseId," + idenCaseNum + "," + idenAppNum + ",@d,@d)";
						sqlStmt.addBatch(stmt);
						//System.out.println(strOut);
						// table laws & issues
						strOut = "Laws & Issues :";
						logFile.write((strOut + "\n").getBytes());
						String prevLaw = "";
						for (String s : laws) {
							if (!s.substring(0,s.indexOf(" — ")).equals(prevLaw)) {
								stmt = "INSERT `case-AoL` (case_id,description,created_at,updated_at)" +
								"VALUES (@caseId,'" + s.substring(0,s.indexOf(" — ")).replaceAll("'","\\\\'").trim() + "',@d,@d)";
								sqlStmt.addBatch(stmt);
								stmt = "SET @lawId = LAST_INSERT_ID()";
								sqlStmt.addBatch(stmt);
							}
							stmt = "INSERT `case-issue` (law_id,description,created_at,updated_at)" +
							"VALUES (@lawId,'" + s.substring(s.indexOf(" — ") + 3).replaceAll("'","\\\\'").trim() + "',@d,@d)";
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
							String refParty1 = "'" + arrTmpStr[0].replaceAll("'","\\\\'").trim() + "'";
							//if (refParty1.length() < 3 || refParty1.length() > 257) throw new Exception("ref party1 : " + refParty1);
							String refParty2 = arrTmpStr.length > 1 ? "'" + arrTmpStr[1].replaceAll("'","\\\\'").trim() + "'" : "null";
							//if (refParty2.length() > 257) throw new Exception("ref party2 : " + refParty2);
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
							//if (!refTreatment.matches("'\\((.*)d\\)'") && !refTreatment.matches("''")) throw new Exception("ref treatment : " + refTreatment);
							refTreatment = refTreatment.replace("(","");
							refTreatment = refTreatment.replace(")","");
							if (refTreatment.equals("'folld'"))
								refTreatment = "'folld/appld'";
							else if (refTreatment.equals("'not folld'"))
								refTreatment = "'not folld/disappd'";
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
								//if (ss.length() > 257) throw new Exception("ref citation : " + ss);
								// reference's citations
								stmt = "INSERT `case-ref_citation` (ref_case_id,year,created_at,updated_at)" +
								"VALUES (@refId,'" + ss.replaceAll("'","\\\\'").trim() + "',@d,@d)";
								sqlStmt.addBatch(stmt);
								strOut = "    year : " + ss.replaceAll("'","\\\\'").trim() ;
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
							String legAct = "'" + s.substring(0,indexA).replaceAll("'","\\\\'").trim() + "'";
							//if (legAct.length() > 257) throw new Exception("legAct : " + legAct);
							int indexS = s.indexOf("(") < 0 ? s.length()-1 : s.indexOf(")",indexA) + 1;
							String citation = "'" + s.substring(indexA,indexS).replaceAll("'","\\\\'").trim() + "'";
							//if (citation.length() > 257) throw new Exception("leg citation : " + citation);
							String sections = "";
							if (indexS > -1)
								sections = s.substring(indexS).trim();

							stmt = "INSERT `case-ref_legis` (case_id,account_name,citation,created_at,updated_at)" +
							"VALUES (@caseId," + legAct + "," + citation + ",@d,@d)";
							sqlStmt.addBatch(stmt);
							stmt = "SET @legId = LAST_INSERT_ID()";
							sqlStmt.addBatch(stmt);
							strOut = "  legAct : " + legAct + "\n  citation : " + citation;
							logFile.write((strOut + "\n").getBytes());
							//System.out.println(strOut);
							// legislation's section
							if (!sections.equals("")) {
								String[] arrSections = sections.split(";");
								for (String ss : arrSections) {
									String treatment = ss.indexOf("(consd)") < 0 ? "''" : "'consd'";
									ss = ss.replaceAll("(consd)","");
									stmt = "INSERT `case-ref_section` (ref_legi_id,section,treatment,created_at,updated_at)" +
									"VALUES (@legId,'" + ss.replaceAll("'", "\'").trim() + "'," + treatment + ",@d,@d)";
									sqlStmt.addBatch(stmt);
									strOut = "    sections : " + sections;
									logFile.write((strOut + "\n").getBytes());
								}
							}
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
						pstmt.setString(1, caseContent);
						pstmt.setInt(2,caseID);
						pstmt.executeUpdate();
						pstmt.close();
						listFile.write((fileName + "	-- id: " + caseID + "\n").getBytes());
					} //not duplicated
				} catch (IOException e) {
					System.out.println("Error I1: " + e.getMessage());
					logFile.write("\n########## IO FAILED ##########\n".getBytes());
					logFile.write(("--- " + fileName + " ---\n\n").getBytes());
					logFile.write(("########## " + e.getMessage() + " ##########\n\n").getBytes());
					failedFile.write(("--- " + fileName + " ---\n\n").getBytes());
					numFailed++;
				} catch (SQLException e) {
					System.out.println("Error S1: " + e.getMessage());
					logFile.write("\n########## QUERY FAILED ##########\n".getBytes());
					logFile.write(("--- " + fileName + " ---\n\n").getBytes());
					logFile.write(("########## " + e.getMessage() + " ##########\n\n").getBytes());
					failedFile.write(("--- " + fileName + " ---\n\n").getBytes());
					numFailed++;
				}/* catch (Exception e) {
					System.out.println("Error E1: " + e.getMessage());
					logFile.write("\n########## VALUES VALIDATION FAILED ##########\n".getBytes());
					logFile.write(("--- " + fileName + " ---\n\n").getBytes());
					logFile.write(("########## " + e.getMessage() + " ##########\n\n").getBytes());
					failedFile.write(("--- " + fileName + " ---\n\n").getBytes());
					numFailed++;
				}*/
			}//for (String strCase : cases)
		}//for (File file : files)
		if (!con.isClosed()) con.close();
		//if (remote && session.isConnected()) session.disconnect();
		logFile.close();
		duplicateFile.close();
		failedFile.close();
		listFile.close();
		} catch (SQLException e) {
			System.out.println("Error S: " + e.getMessage());
			numFile--;
		} catch (FileNotFoundException e) {
			System.out.println("Error F: " + e.getMessage());
			numFile--;
		} catch (IOException e) {
			System.out.println("Error I: " + e.getMessage());
			numFile--;
		}
		System.out.println("total file executed : " + numFile);
		System.out.println("total file failed : " + numFailed);
	}
}
