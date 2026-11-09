package de.hswt.fi.export.service.excel;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.nio.file.Path;
import java.util.List;

public class ExcelFileDefinition {

	@Pattern(regexp = "([^\\s|\\[|\\]]+(\\.(?i)(xlsx))$)")
	@NotNull
	private String filename;

	private List<ExcelSheetDefinition> sheets;

	private Path sourceFilePath;

	private boolean addSourceFile = false;
	
	public ExcelFileDefinition() {
		filename = "results.xlsx";
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	List<ExcelSheetDefinition> getSheets() {
		return sheets;
	}

	public void setSheets(List<ExcelSheetDefinition> sheets) {
		this.sheets = sheets;
	}

	public void setSourceFile(Path sourceFilePath) {
		this.sourceFilePath = sourceFilePath;
	}
	
	Path getSourceFile() {
		return sourceFilePath;
	}
	
	boolean isAddSources() {
		return addSourceFile;
	}

	public void setAddSources(boolean addSources) {
		this.addSourceFile = addSources;
	}

	@Override
	public String toString() {
		return "ExcelFileDefinition [filename=" +
				filename +
				", sheets=" +
				sheets +
				"]";
	}

}
