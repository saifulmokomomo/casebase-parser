# casebase-parser

dependencies:
  - commons-logging-1.2
  - fontbox-1.8.9
  - iText-4.2.0
  - jempbox-1.8.9
  - jsch-0.1.53
  - mysql-connector-java-5.1.38-bin
  - pdfbox-1.8.9


convert pdfs to text first for easy manipulation
  java ToText <Directory>

process the converted pdfs and insert into database
  java CaseBaseFromText <Directory> [-remote]
