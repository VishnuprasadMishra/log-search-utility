package com.util.log.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import com.util.log.search.LogParser;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.sql.Timestamp;

/**
 * 
 * @author Vishnu Mishra
 *
 */
public class LogSearchUserInteface {

	private JFrame frame;
	private JTextField searchStringTextField;
	private JTextField fieldsToExtractTextField;
	private JComboBox<String> logLevelComboBox;
	private JTextField startTimeTextField;
	private JTextField endTimeTextField;
	private JTextField fileTextField;
	private JTable outputTable;

	private Font textFieldFont = new Font("Arial", Font.PLAIN, 14);
	private Font labelFieldFont = new Font("Arial", Font.BOLD, 14);

	public LogSearchUserInteface() {
		initialize();
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(LogSearchUserInteface::new);
	}

	private void initialize() {
		frame = new JFrame("Log Search Utility");

		frame.setSize(1000, 1000);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(null);
		frame.setLocationRelativeTo(null);
		frame.setResizable(false);

		addSearchString();
		addFieldsToExtract();
		addLogLevel();
		addStatTime();
		addEndTime();
		addFileSelectButton();
		addOutputTable();
		addSubmitButton();

		frame.setVisible(true);
	}

	private void addSearchString() {
		JLabel searchStringLabel = new JLabel("Search Query:");
		searchStringLabel.setBounds(150, 50, 300, 30);
		searchStringLabel.setFont(labelFieldFont);
		frame.add(searchStringLabel);

		searchStringTextField = new JTextField();
		searchStringTextField.setBounds(350, 50, 500, 30);
		searchStringTextField.setFont(textFieldFont);
		searchStringTextField.setToolTipText("Enter search query for e.g. (\"Salesforce\" AND \"updated MQ\") OR 234532");

		frame.add(searchStringTextField);
	}

	private void addFieldsToExtract() {
		JLabel fieldsToExtractLabel = new JLabel("Fields to Extract:");
		fieldsToExtractLabel.setBounds(150, 90, 300, 30);
		fieldsToExtractLabel.setFont(labelFieldFont);
		frame.add(fieldsToExtractLabel);

		fieldsToExtractTextField = new JTextField();
		fieldsToExtractTextField.setBounds(350, 90, 500, 30);
		fieldsToExtractTextField.setFont(textFieldFont);
		fieldsToExtractTextField.setToolTipText("Enter comma seperated fields to extract from json in log event and if none provided then json from log event will be displayed. You can provide json path for e.g. correlationId,contextProperties.salesOrderNo");
		frame.add(fieldsToExtractTextField);
	}

	private void addLogLevel() {
		JLabel logLevelLabel = new JLabel("Log Level:");
		logLevelLabel.setBounds(150, 130, 300, 30);
		logLevelLabel.setFont(labelFieldFont);
		frame.add(logLevelLabel);

		String[] logLevels = { "ALL", "INFO", "DEBUG", "ERROR", "WARN", "TRACE" };
		logLevelComboBox = new JComboBox<>(logLevels);
		logLevelComboBox.setBounds(350, 130, 150, 30);
		frame.add(logLevelComboBox);
	}

	private void addStatTime() {
		JLabel startTimeLabel = new JLabel("Start Time:");
		startTimeLabel.setBounds(150, 170, 300, 30);
		startTimeLabel.setFont(labelFieldFont);
		frame.add(startTimeLabel);

		startTimeTextField = new JTextField();
		startTimeTextField.setBounds(350, 170, 200, 30);
		startTimeTextField.setFont(textFieldFont);
		startTimeTextField.setToolTipText("Enter the log start time to filter the logs in the format : " + new Timestamp(System.currentTimeMillis()).toString());

		frame.add(startTimeTextField);
	}

	private void addEndTime() {
		JLabel endTimeLabel = new JLabel("End Time:");
		endTimeLabel.setBounds(150, 210, 300, 30);
		endTimeLabel.setFont(labelFieldFont);
		frame.add(endTimeLabel);

		endTimeTextField = new JTextField();
		endTimeTextField.setBounds(350, 210, 200, 30);
		endTimeTextField.setFont(textFieldFont);
		endTimeTextField.setToolTipText("Enter the log end time to filter the logs in the format : " + new Timestamp(System.currentTimeMillis()).toString());

		frame.add(endTimeTextField);
	}

	private void addFileSelectButton() {
		JLabel fileLabel = new JLabel("Select Log File:");
		fileLabel.setBounds(150, 250, 300, 30);
		fileLabel.setFont(labelFieldFont);
		frame.add(fileLabel);

		JButton fileSelectButton = new JButton("Browse");
		fileSelectButton.setBounds(350, 250, 100, 30);
		fileTextField = new JTextField();
		fileTextField.setBounds(470, 250, 380, 30);
		fileTextField.setFont(textFieldFont);

		fileSelectButton.addActionListener(e -> {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileFilter(new LogFileFilter());
			int result = fileChooser.showOpenDialog(frame);

			if (result == JFileChooser.APPROVE_OPTION) {
				File selectedFile = fileChooser.getSelectedFile();
				fileTextField.setText(selectedFile.getAbsolutePath());
			}
		});

		frame.add(fileSelectButton);
		frame.add(fileTextField);
	}

	private void addSubmitButton() {
		JButton submitButton = new JButton("Search Logs");
		submitButton.setBounds(430, 330, 150, 30);
		frame.add(submitButton);

		submitButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				submitButtonClicked();
			}
		});
	}

	private void addOutputTable() {
		outputTable = new JTable();
		outputTable.setVisible(true);

		JScrollPane scrollPane = new JScrollPane(outputTable);
		scrollPane.setBounds(40, 380, 900, 550);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		frame.add(scrollPane);
	}

	private void submitButtonClicked() {
		String searchString = searchStringTextField.getText();
		String fieldsToExtract = fieldsToExtractTextField.getText();
		String logLevel = logLevelComboBox.getSelectedItem().toString();
		String filePath = fileTextField.getText();
		String startTime = startTimeTextField.getText();
		String endTime = endTimeTextField.getText();

		LogParser logParser = new LogParser();

		try {
			DefaultTableModel tableModel = logParser.execute(searchString, fieldsToExtract, logLevel, startTime, endTime, filePath);
			outputTable.setModel(tableModel);
			
			TableColumnModel columnModel = outputTable.getColumnModel();

		    // Set the preferred, minimum, and maximum widths for each column
		    columnModel.getColumn(0).setPreferredWidth(70); // Adjust the index and width as needed
		    columnModel.getColumn(0).setMinWidth(30);
		    columnModel.getColumn(0).setMaxWidth(100);

		    columnModel.getColumn(1).setPreferredWidth(160);
		    columnModel.getColumn(1).setMinWidth(100);
		    columnModel.getColumn(1).setMaxWidth(200);
		    
		    columnModel.getColumn(2).setPreferredWidth(80);
		    columnModel.getColumn(2).setMinWidth(30);
		    columnModel.getColumn(2).setMaxWidth(100);
		} catch (Exception exception) {
			DefaultTableModel tableModel = new DefaultTableModel();
			tableModel.addColumn("Exception Details");
			
			String row[] = {getStackTraceAsString(exception)};
			tableModel.addRow(row);
			
			outputTable.setModel(tableModel);
			outputTable.getColumnModel().getColumn(0).setCellRenderer(new WordWrapRenderer());
			outputTable.getColumnModel().getColumn(0).setCellEditor(new WordWrapEditor());
		}
	}

	private String getStackTraceAsString(Throwable throwable) {
		StringBuilder stackTrace = new StringBuilder();
		stackTrace.append(throwable.toString()).append("\n");

		for (StackTraceElement element : throwable.getStackTrace()) {
			stackTrace.append("\tat ").append(element).append("\n");
		}

		if (throwable.getCause() != null) {
			stackTrace.append("Caused by: ").append(getStackTraceAsString(throwable.getCause()));
		}

		return stackTrace.toString();
	}
}
