/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titanium.markers.export;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.common.utils.IOUtils;
import org.eclipse.titan.designer.consoles.TITANDebugConsole;
import org.eclipse.titan.designer.core.ProjectBasedBuilder;
import org.eclipse.titanium.markers.handler.Marker;
import org.eclipse.titanium.markers.handler.MarkerHandler;
import org.eclipse.titanium.markers.types.CodeSmellType;
import org.eclipse.titanium.markers.types.TaskType;
import org.eclipse.titanium.markers.utils.AnalyzerCache;
import org.eclipse.titanium.markers.utils.RiskFactorCalculator;
import org.eclipse.titanium.metrics.MetricData;
import org.eclipse.titanium.metrics.ModuleMetric;
import org.eclipse.titanium.metrics.StatColumn;

/**
 * Export problem markers of a project to an xls.
 *
 * @author poroszd
 *
 */
public class XlsProblemExporter extends BaseProblemExporter {

	/**
	 * Creates a problem exporter on the given project
	 *
	 * @param project
	 *            candidate whose code smell markers are to save
	 */
	public XlsProblemExporter(final IProject project) {
		super(project);
	}

	/**
	 * Export the code smells of a project to an excel workbook.
	 * <p>
	 * The first sheet of the workbook is a summary page, showing the number of
	 * hits for each code smell, and an expressive bar chart of these data. The
	 * further sheets enumerate the specific code smells of each kind, including
	 * the message of the code smell, and the file name and line where it
	 * occurred.
	 * <p>
	 * Note: All code smell types are used in the analysis and are written in
	 * the output. Some code smells use external settings, which can be fine
	 * tuned on the preference page.
	 *
	 * @param filename
	 *            the file to save the xls
	 * @param date
	 *            the time stamp to write on the summary page
	 *
	 * @throws IOException
	 *             when writing the file fails
	 */
	@Override
	// Flow analysis thinks 'sheet' may be referenced as null, but it is
	// guaranteed to be initialized first.
	public void exportMarkers(final IProgressMonitor monitor, final String filename, final Date date) throws IOException {
		final SubMonitor progress = SubMonitor.convert(monitor, 100);
		final  File file = new File(filename);
		POIFSFileSystem fs = null;
		HSSFWorkbook workbook = null;

		try {
			fs = new POIFSFileSystem(XlsProblemExporter.class.getResourceAsStream("ProblemMarkers.xlt"));
			workbook = new HSSFWorkbook(fs, true);
		} catch (IOException e) {
			ErrorReporter.logExceptionStackTrace("Error while exporting to excel", e);
			// Error on opening the template xls. Create an empty
			// one (without the chart).
			if (reportDebugInformation) {
				TITANDebugConsole.println("Error on opening ProblemMarkers.xlt. Chartless xls will be generated");
			}
			workbook = new HSSFWorkbook(new FileInputStream(file));
			workbook.createSheet("Summary");
			workbook.setSheetOrder("Summary", 0);
		} catch (Exception e) {
			ErrorReporter.logExceptionStackTrace("Error while exporting to excel", e);
			return;
		}
		progress.worked(10);

		final List<IProject> projects = ProjectBasedBuilder.getProjectBasedBuilder(project).getAllReachableProjects();
		projects.remove(project);

		try {
			final HSSFSheet summarySheet = workbook.getSheetAt(0);
			createTimeSheet(workbook);

			final Map<String, Integer> smellCount = new HashMap<String, Integer>();
			int summaryRow = 5;

			Cell label = null;
			Cell numberCell = null;

			final Map<TaskType, List<IMarker>> markers = collectMarkers();
			// export the task markers:
			for (final TaskType t : TaskType.values()) {
				createTaskSheet(workbook, t, markers.get(t), !projects.isEmpty());

				final Row row1 = summarySheet.createRow(summaryRow++);
				label = row1.createCell(0);
				label.setCellValue(t.getHumanReadableName());

				final int nofMarkers = markers.get(t).size();
				numberCell = row1.createCell(1);
				numberCell.setCellValue(nofMarkers);

				// row-1 is the number of found markers
				smellCount.put(t.name(), nofMarkers);
			}

			progress.worked(20);

			final MarkerHandler mh = AnalyzerCache.withAll().analyzeProject(progress.newChild(30), project);
			progress.setWorkRemaining(CodeSmellType.values().length + 1);
			// export the semantic problem markers:
			for (final CodeSmellType t : CodeSmellType.values()) {
				createCodeSmellSheet(workbook, mh, t, !projects.isEmpty());

				final Row row1 = summarySheet.createRow(summaryRow++);
				label = row1.createCell(0);
				label.setCellValue(t.getHumanReadableName());

				smellCount.put(t.name(), mh.numberOfOccurrences(t));

				numberCell = row1.createCell(1);
				numberCell.setCellValue(mh.numberOfOccurrences(t));

				progress.worked(1);
			}

			final StringBuilder nameBuilder = new StringBuilder("Project: ");
			nameBuilder.append(project.getName());
			if (!projects.isEmpty()) {
				nameBuilder.append(" including ( ");
				for(int i = 0; i < projects.size(); i++) {
					if(i > 0) {
						nameBuilder.append(", ");
					}
					nameBuilder.append(projects.get(i).getName());
				}
				nameBuilder.append(" )");
			}
			final Row row0 = summarySheet.createRow(0);
			row0.createCell(0).setCellValue(nameBuilder.toString());


			final Row row1 = summarySheet.createRow(1);
			row1.createCell(0).setCellValue("Code smell \\ date");

			final CellStyle cellStyle = workbook.createCellStyle();
			cellStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("yyyy.mm.dd"));
			label = row1.createCell(1);
			label.setCellValue(date);
			label.setCellStyle(cellStyle);

			final Row row2 = summarySheet.createRow(2);
			row2.createCell(0).setCellValue("Commulative Project Risk Factor");
			final int riskFactor = new RiskFactorCalculator().measure(project, smellCount);
			row2.createCell(1).setCellValue(riskFactor);

			final MetricData data = MetricData.measure(project);
			final Number n = data.getStatistics(ModuleMetric.LINES_OF_CODE).get(StatColumn.TOTAL);
			final int loc = n.intValue();

			final Row row3 = summarySheet.createRow(3);
			row3.createCell(0).setCellValue("Lines of code");
			row3.createCell(1).setCellValue(loc);

			summarySheet.autoSizeColumn(0);
			summarySheet.autoSizeColumn(1);

			progress.worked(1);
		} catch (Exception e) {
			ErrorReporter.logExceptionStackTrace("Error while exporting to excel", e);
		} finally {
			FileOutputStream fileOutputStream = null;
			try {
				fileOutputStream = new FileOutputStream(file);
				workbook.write(fileOutputStream);
			} catch (Exception e) {
				ErrorReporter.logExceptionStackTrace("Error while closing the generated excel", e);
			} finally {
				IOUtils.closeQuietly(fileOutputStream);
			}
		}
	}

	/**
	 * Create the summary sheet in the exported document.
	 *
	 * @param workbook the workbook to work in.
	 * */
	private void createTimeSheet(final HSSFWorkbook workbook) {
		final HSSFSheet timeSheet = workbook.createSheet("Repair times");
		workbook.setSheetOrder("Repair times", 1);

		final Row headerRow = timeSheet.createRow(0);
		headerRow.createCell(1).setCellValue("Minimal repair time");
		headerRow.createCell(2).setCellValue("Average repair time");
		headerRow.createCell(3).setCellValue("Maximal repair time");

		int summaryRow = 4;
		Cell label;
		for (final TaskType t : TaskType.values()) {
			final Row row2 = timeSheet.createRow(summaryRow);
			label = row2.createCell(0);
			label.setCellValue(t.getHumanReadableName());
			final Cell minTimeCell = row2.createCell(1);
			minTimeCell.setCellType(HSSFCell.CELL_TYPE_FORMULA);
			minTimeCell.setCellFormula(t.getMinRepairTime() + "*Summary!$B" + (summaryRow + 1));
			final Cell avgTimeCell = row2.createCell(2);
			avgTimeCell.setCellType(HSSFCell.CELL_TYPE_FORMULA);
			avgTimeCell.setCellFormula(t.getAvgRepairTime() + "*Summary!$B" + (summaryRow + 1));
			final Cell maxTimeCell = row2.createCell(3);
			maxTimeCell.setCellType(HSSFCell.CELL_TYPE_FORMULA);
			maxTimeCell.setCellFormula(t.getMaxRepairTime() + "*Summary!$B" + (++summaryRow));
		}

		for (final CodeSmellType t : CodeSmellType.values()) {
			final Row row2 = timeSheet.createRow(summaryRow);
			label = row2.createCell(0);
			label.setCellValue(t.getHumanReadableName());
			final Cell minTimeCell = row2.createCell(1);
			minTimeCell.setCellType(HSSFCell.CELL_TYPE_FORMULA);
			minTimeCell.setCellFormula(t.getMinRepairTime() + "*Summary!$B" + (summaryRow + 1));
			final Cell avgTimeCell = row2.createCell(2);
			avgTimeCell.setCellType(HSSFCell.CELL_TYPE_FORMULA);
			avgTimeCell.setCellFormula(t.getAvgRepairTime() + "*Summary!$B" + (summaryRow + 1));
			final Cell maxTimeCell = row2.createCell(3);
			maxTimeCell.setCellType(HSSFCell.CELL_TYPE_FORMULA);
			maxTimeCell.setCellFormula(t.getMaxRepairTime() + "*Summary!$B" + (++summaryRow));
		}

		final Row totalRow = timeSheet.createRow(1);
		totalRow.createCell(0).setCellValue("Total");

		final Cell cell1 = totalRow.createCell(1);
		cell1.setCellType(HSSFCell.CELL_TYPE_FORMULA);
		cell1.setCellFormula("SUM($B4:$B" + summaryRow + ")");

		final Cell cell2 = totalRow.createCell(2);
		cell2.setCellType(HSSFCell.CELL_TYPE_FORMULA);
		cell2.setCellFormula("SUM($C4:$C" + summaryRow + ")");

		final Cell cell3 = totalRow.createCell(3);
		cell3.setCellType(HSSFCell.CELL_TYPE_FORMULA);
		cell3.setCellFormula("SUM($D4:$D" + summaryRow + ")");

		timeSheet.autoSizeColumn(0);
		timeSheet.autoSizeColumn(1);
		timeSheet.autoSizeColumn(2);
		timeSheet.autoSizeColumn(3);
	}

	/**
	 * Create a page for a task type.
	 *
	 * @param workbook the workbook to work in.
	 * @param t the task type to export.
	 * @param markers the list of markers.
	 * @param fullPath the resource names should use full path or project relative
	 * */
	private void createTaskSheet(final HSSFWorkbook workbook, final TaskType t, final List<IMarker> markers, final boolean fullPath) {
		if (markers.isEmpty()) {
			return;
		}

		int currentRow = 1;
		final String sheetName = t.getHumanReadableName();
		final HSSFSheet sheet = workbook.createSheet(sheetName);

		Row row = sheet.createRow(0);
		row.createCell(0).setCellValue("Description");
		row.createCell(1).setCellValue("Resource");
		row.createCell(2).setCellValue("Location");

		Cell label;
		Cell numberCell;
		for (final IMarker m : markers) {
			row = sheet.createRow(currentRow);
			label = row.createCell(0);
			try {
				label.setCellValue(m.getAttribute(IMarker.MESSAGE).toString());
			} catch (CoreException e) {
				label.setCellValue("<unknown>");
			}

			label = row.createCell(1);
			if( fullPath) {
				label.setCellValue(m.getResource().getFullPath().toString());
			} else {
				label.setCellValue(m.getResource().getName());
			}

			numberCell = row.createCell(2);
			try {
				numberCell.setCellValue(Double.parseDouble(m.getAttribute(IMarker.LINE_NUMBER).toString()));
			} catch (CoreException e) {
				// Do nothing
			}

			++currentRow;
		}

		sheet.autoSizeColumn(0);
		sheet.autoSizeColumn(1);
	}

	/**
	 * Create a page for a code smell.
	 *
	 * @param workbook the workbook to work in.
	 * @param mh the markerhandler object knowing the occurences of the given code smell
	 * @param t the codesmell type to export
	 * @param fullPath the resource names should use full path or project relative
	 * */
	private void createCodeSmellSheet(final HSSFWorkbook workbook, final MarkerHandler mh, final CodeSmellType t, final boolean fullPath) {
		if (mh.get(t).isEmpty()) {
			return;
		}

		final String sheetName = t.name();
		final HSSFSheet sheet = workbook.createSheet(sheetName);

		Row row = sheet.createRow(0);
		row.createCell(0).setCellValue("Description");
		row.createCell(1).setCellValue("Resource");
		row.createCell(2).setCellValue("Location");

		int currentRow = 1;
		Cell label;
		for (final Marker m : mh.get(t)) {
			if (m.getLine() == -1 || m.getResource() == null) {
				// TODO this might need a second thought
				continue;
			}
			try {
				row = sheet.createRow(currentRow);
				label = row.createCell(0);
				label.setCellValue(m.getMessage());

				label = row.createCell(1);
				if( fullPath) {
					label.setCellValue(m.getResource().getFullPath().toString());
				} else {
					label.setCellValue(m.getResource().getName());
				}

				label = row.createCell(2);
				label.setCellValue(m.getLine());

				++currentRow;
			} catch (Exception e) {
				ErrorReporter.logWarning("Only " + currentRow + " rows were written: the limit has been reached.");
				break;
			}
		}

		sheet.autoSizeColumn(0);
		sheet.autoSizeColumn(1);
		sheet.autoSizeColumn(2);
	}
}
