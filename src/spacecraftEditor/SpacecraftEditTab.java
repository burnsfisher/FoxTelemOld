package spacecraftEditor;

import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import common.Config;
import common.Log;
import common.Spacecraft;
import spacecraftEditor.listEditors.curves.CurvesTableModel;
import spacecraftEditor.listEditors.expressions.ExpressionsCsvFileEditPanel;
import spacecraftEditor.listEditors.expressions.ExpressionsTableModel;
import spacecraftEditor.listEditors.CsvFileEditPanel;
import spacecraftEditor.listEditors.curves.CurveCsvFileEditPanel;
import spacecraftEditor.listEditors.frames.FrameListEditPanel;
import spacecraftEditor.listEditors.lookupTables.LookupListTableModel;
import spacecraftEditor.listEditors.lookupTables.LookupTableListEditPanel;
import spacecraftEditor.listEditors.lookupTables.LookupTableModel;
import spacecraftEditor.listEditors.payload.PayloadListEditPanel;
import spacecraftEditor.listEditors.stringLookupTables.StringLookupTableListEditPanel;
import telemetry.SatPayloadStore;

/**
 * This holds an entire spacecraft that is being edited.  It is organized as follows:
 * 
 * LEFT: SpacecraftEditPanel
 * CENTER: TabbedPane containing SpacecraftListEditPanels
 * 
 * @author chris
 *
 */
public class SpacecraftEditTab extends JPanel {
	private static final long serialVersionUID = 1L;
	public static final String CURVES_TEMPLATE_FILENAME = "CURVES_template.csv";
	public static final String MATH_EXPRESSIONS_TEMPLATE_FILENAME = "MATH_EXPRESSIONS_template.csv";

	Spacecraft sat;
	JTabbedPane tabbedPane;
	SpacecraftEditPanel spacecraftEditPanel;
	
//	String[] satLists = {"Payloads","Frames","Lookup tables", "String Lookup tables", "Conversion Coeff", "Math Expressions" };
	
	public SpacecraftEditTab(Spacecraft s) {
		sat = s;
		setLayout(new BorderLayout(0, 0));
//		spacecraftEditPanel = new SpacecraftEditPanel(sat);
//		add(spacecraftEditPanel, BorderLayout.WEST);
		
		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		add(tabbedPane, BorderLayout.CENTER);
	
		// Params
		spacecraftEditPanel = new SpacecraftEditPanel(sat);
		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1><b>" 
				+ "Parameters" + "</b></body></html>", spacecraftEditPanel );
		
		// PAYLOADS 
		PayloadListEditPanel payloadListEditPanel = new PayloadListEditPanel(sat,spacecraftEditPanel);
		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1><b>" 
			+ "Payloads" + "</b></body></html>", payloadListEditPanel );
		
		// FRAMES
		FrameListEditPanel frameListEditPanel = new FrameListEditPanel(sat,spacecraftEditPanel);
		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1><b>" 
			+ "Frames" + "</b></body></html>", frameListEditPanel );
		
		// CURVES
		CurvesTableModel model = new CurvesTableModel();
		String f = sat.conversionCurvesFileName;
		CsvFileEditPanel csvFileEdit;
		if (f == null) {
			String targetFilename = "";
			File sourceFile = new File(System.getProperty("user.dir") + File.separator + Spacecraft.SPACECRAFT_DIR 
					+ File.separator + "templates" + File.separator + CURVES_TEMPLATE_FILENAME);
			File targetFile = new File(Config.spacecraftDir + File.separator + CURVES_TEMPLATE_FILENAME);
			try {
				SatPayloadStore.copyFile(sourceFile, targetFile);
				targetFilename = targetFile.getName();
			} catch (IOException e) {
				Log.errorDialog("ERROR", "Could not load CURVES template" + e);
				e.printStackTrace(Log.getWriter());
			}
			csvFileEdit = new CurveCsvFileEditPanel(sat, model, "Curves",targetFilename);
			csvFileEdit.setFilenameText("");
		} else {
			csvFileEdit = new CurveCsvFileEditPanel(sat, model, "Curves",f);
		}
		
		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1><b>" 
			+ "Conversion Curves" + "</b></body></html>", csvFileEdit );
		
		// EXPRESSIONS
		ExpressionsTableModel expressionsModel = new ExpressionsTableModel();
		String expFile = sat.conversionExpressionsFileName;
		ExpressionsCsvFileEditPanel expressionsCsvFileEdit;
		if (expFile == null) {
			String targetFilename = "";
			File sourceFile = new File(System.getProperty("user.dir") + File.separator + Spacecraft.SPACECRAFT_DIR 
					+ File.separator + "templates"+ File.separator + MATH_EXPRESSIONS_TEMPLATE_FILENAME);
			File targetFile = new File(Config.spacecraftDir + File.separator + MATH_EXPRESSIONS_TEMPLATE_FILENAME);
			try {
				SatPayloadStore.copyFile(sourceFile, targetFile);
				targetFilename = targetFile.getName();
			} catch (IOException e) {
				Log.errorDialog("ERROR", "Could not load CURVES template" + e);
				e.printStackTrace(Log.getWriter());
			}
			expressionsCsvFileEdit = new ExpressionsCsvFileEditPanel(sat, expressionsModel, "Expressions",targetFilename);
			expressionsCsvFileEdit.setFilenameText("");
		} else {
			expressionsCsvFileEdit = new ExpressionsCsvFileEditPanel(sat, expressionsModel, "Expressions",expFile);
		}
		
		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1><b>" 
				+ "Math Expressions" + "</b></body></html>", expressionsCsvFileEdit );
		
		// LOOKUP TABLES 
		LookupListTableModel lookupListTableModel = new LookupListTableModel();
		LookupTableModel lookupTableModel = new LookupTableModel();
		LookupTableListEditPanel lookupTableListEditPanel = new LookupTableListEditPanel(sat,"Lookup Tables", lookupListTableModel, lookupTableModel, spacecraftEditPanel);
		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1><b>" 
				+ "Lookup Tables" + "</b></body></html>", lookupTableListEditPanel );
		
		// STRING LOOKUP TABLES 
		LookupListTableModel stringLookupListTableModel = new LookupListTableModel();
		LookupTableModel stringLookupTableModel = new LookupTableModel();
		StringLookupTableListEditPanel stringLookupTableListEditPanel = new StringLookupTableListEditPanel(sat,"String Lookup Tables", stringLookupListTableModel, stringLookupTableModel, spacecraftEditPanel);
		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1><b>" 
				+ "String Lookup Tables" + "</b></body></html>", stringLookupTableListEditPanel );

	}
}
