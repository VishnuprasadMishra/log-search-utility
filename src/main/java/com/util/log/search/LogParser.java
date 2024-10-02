package com.util.log.search;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.table.DefaultTableModel;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * @author Vishnu Mishra
 *
 */
public class LogParser {

	private Directory memoryIndex = new ByteBuffersDirectory();
	private StandardAnalyzer analyzer = new StandardAnalyzer();

	public DefaultTableModel execute(String searchQuery, String fieldsToExtract, String logLevel, String startTime, String endTime, String logFilePath) throws Exception {

		try {
			
			if (searchQuery == null || searchQuery.isEmpty()) {
				throw new Exception("Please provide search query.");
			}
			
			if (fieldsToExtract == null || fieldsToExtract.isEmpty()) {
				fieldsToExtract = "Log Event";
			}
			
			DefaultTableModel tableModel = new DefaultTableModel();
			
			tableModel.addColumn("S.N.");
			tableModel.addColumn("Date");
			tableModel.addColumn("Level");

			String[] fieldsToExtractArr = fieldsToExtract.split(",");
			for (String columnHeader : fieldsToExtractArr) {
				tableModel.addColumn(columnHeader.trim());
			}

			parseLogs(logFilePath, logLevel, startTime, endTime);

			SearchLog searchLog = new SearchLog(memoryIndex, analyzer);
			List<Document> outputDocuments = searchLog.search(searchQuery, logLevel);
			
			ObjectMapper objectMapper = new ObjectMapper();

			int eventCount = 0;
			
			for (Document outputDocument : outputDocuments) {
				eventCount = eventCount + 1;
				 
				String[] row = new String[tableModel.getColumnCount()];
				if (fieldsToExtractArr[0] == "Log Event") {
					row[0] = String.valueOf(eventCount);
            		row[1] = outputDocument.get("Log_Date");
            		row[2] = outputDocument.get("Level");
            		row[3] = outputDocument.get("Content");
				} else {
					String jsonString = getJsonFromString(outputDocument.get("Content"));
					if (isValidJson(jsonString, objectMapper)) {
						
					    JsonNode jsonNode = objectMapper.readTree(jsonString);
						 
						row[0] = String.valueOf(eventCount);
						row[1] = outputDocument.get("Log_Date");
						row[2] = outputDocument.get("Level");
						
						for (int i = 0; i < fieldsToExtractArr.length; i++) {
							if (fieldsToExtractArr[i].equalsIgnoreCase("payload")) {
								row[i+3] = jsonString;
							} else {
								row[i+3] = extractJsonPathValue(jsonNode, fieldsToExtractArr[i]);
							}
						}
					}
				}
				
				tableModel.addRow(row);
			}
			
			return tableModel;

		} catch (Exception e) {
			throw e;
		} finally {
			try {
				memoryIndex.close();
				analyzer.close();
				memoryIndex = null;
				analyzer = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void parseLogs(String logFilePath, String logLevel, String startTime, String endTime) throws Exception {
		IndexLog indexLog = new IndexLog(memoryIndex, analyzer);

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
		 
		LocalDateTime startTimestamp = null;
		LocalDateTime endTimestamp = null;
		if (startTime != null && !startTime.isEmpty()) {
			startTimestamp = LocalDateTime.parse(startTime, formatter);
        } 
		
		if (endTime != null && !endTime.isEmpty()) {
			endTimestamp = LocalDateTime.parse(endTime, formatter);
        } 

		try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath))) {

			String currentLogLevel = null;
			StringBuilder currentLogEntry = new StringBuilder();
			String line;

			while ((line = reader.readLine()) != null) {
				String level = getLogLevel(line);
				if (level != null) {
					if (currentLogEntry.length() > 0 && currentLogLevel != null) {
						if(level.equalsIgnoreCase(logLevel)|| logLevel.equalsIgnoreCase("ALL")) {
							String logContent = currentLogEntry.toString();
							
							String logTimestamp = extractTimestamp(logContent);
							LocalDateTime dateToCheckTimestamp = LocalDateTime.parse(logTimestamp, formatter);
							if(startTimestamp != null && endTimestamp!= null) {
								if(isWithinRange(dateToCheckTimestamp, startTimestamp, endTimestamp)) {
									Document document = new Document();
									document.add(new TextField("Level", level, Field.Store.YES));
									document.add(new TextField("Log_Date", logTimestamp, Field.Store.YES));
									document.add(new TextField("Content", logContent, Field.Store.YES));
									indexLog.index(document);
								}
							} else {
								Document document = new Document();
								document.add(new TextField("Level", level, Field.Store.YES));
								document.add(new TextField("Log_Date", logTimestamp, Field.Store.YES));
								document.add(new TextField("Content", logContent, Field.Store.YES));
								indexLog.index(document);
							}
						}
						currentLogEntry.setLength(0);
					}
					currentLogLevel = level;
				}
				currentLogEntry.append(line).append("\n");
			}

			if (currentLogEntry.length() > 0 && currentLogLevel != null) {
				String logContent = currentLogEntry.toString();
				
				String logTimestamp = extractTimestamp(logContent);
				LocalDateTime dateToCheckTimestamp = LocalDateTime.parse(logTimestamp, formatter);
				if(currentLogLevel.equalsIgnoreCase(logLevel)|| logLevel.equalsIgnoreCase("ALL")) {
					if(startTimestamp != null && endTimestamp!= null) {
						if(isWithinRange(dateToCheckTimestamp, startTimestamp, endTimestamp)) {
							Document document = new Document();
							document.add(new TextField("Level", currentLogLevel, Field.Store.YES));
							document.add(new TextField("Log_Date", logTimestamp, Field.Store.YES));
							document.add(new TextField("Content", logContent, Field.Store.YES));
							indexLog.index(document);
						}
					} else {
						Document document = new Document();
						document.add(new TextField("Level", currentLogLevel, Field.Store.YES));
						document.add(new TextField("Log_Date", logTimestamp, Field.Store.YES));
						document.add(new TextField("Content", logContent, Field.Store.YES));
						indexLog.index(document);
					}
				}				
			}
		} finally {
			indexLog.close();
			indexLog = null;
		}
	}
	
	private String getJsonFromString(String input) {
        StringBuilder jsonBuilder = new StringBuilder();
        int braceCount = 0;

        for (char ch : input.toCharArray()) {
            if (ch == '{') {
                braceCount++;
            }
            if (braceCount > 0) {
                jsonBuilder.append(ch);
            }
            if (ch == '}') {
                braceCount--;
                if (braceCount == 0) {
                    break;
                }
            }
        }

        return jsonBuilder.toString();
    }

    public static String extractJsonPathValue(JsonNode rootNode, String jsonPath) {
        String[] pathParts = jsonPath.split("\\.");
        JsonNode currentNode = rootNode;

        for (String part : pathParts) {
            if (part.contains("[") && part.contains("]")) {
                String arrayName = part.substring(0, part.indexOf("["));
                int arrayIndex = Integer.parseInt(part.substring(part.indexOf("[") + 1, part.indexOf("]")));
                currentNode = currentNode.path(arrayName);

                if (currentNode.isArray() && currentNode.size() > arrayIndex) {
                    currentNode = currentNode.get(arrayIndex);
                } else {
                    return null;
                }
            } else {
                currentNode = currentNode.path(part);
            }

            if (currentNode.isMissingNode()) {
                return null;
            }
        }

        return currentNode.isValueNode() ? currentNode.asText() : null;
    }

    private static boolean isValidJson(String jsonString, ObjectMapper objectMapper) {
        try {
            objectMapper.readTree(jsonString);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    private String extractTimestamp(String logEntry) {
        // Define a regular expression to match the timestamp pattern
        String timestampRegex = "(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})";

        // Create a Pattern object
        Pattern pattern = Pattern.compile(timestampRegex);

        // Create a Matcher object
        Matcher matcher = pattern.matcher(logEntry);

        // Find the timestamp
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return "Timestamp not found";
        }
    }

    private boolean isWithinRange(LocalDateTime dateToCheck, LocalDateTime startDate, LocalDateTime endDate) {
        return dateToCheck.isEqual(startDate) || dateToCheck.isEqual(endDate) || (dateToCheck.isAfter(startDate) && dateToCheck.isBefore(endDate));
    }

	private static String getLogLevel(String line) {
		Pattern pattern = Pattern.compile("\\s+(INFO|ERROR|WARN|DEBUG|TRACE|FATAL)\\s+");
		Matcher matcher = pattern.matcher(line);

		if (matcher.find()) {
			return matcher.group(1).trim();
		}

		return null;
	}
}
