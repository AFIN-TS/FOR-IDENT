package de.hswt.fi.fileimport.service.excel;

import de.hswt.fi.fileimport.service.api.FileBasedFeatureContentImporter;
import de.hswt.fi.fileimport.service.api.RtiCalibrationImporter;
import de.hswt.fi.model.FeatureSet;
import de.hswt.fi.model.RTICalibrationData;
import de.hswt.filehandler.api.MainReader;
import de.hswt.filehandler.excel.ExcelInspector;
import de.hswt.filehandler.excel.ExcelReader;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
public class ExcelFeatureContentImporter
		implements FileBasedFeatureContentImporter, RtiCalibrationImporter {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExcelFeatureContentImporter.class);

	private List<ExcelReader> readers;

	private List<AbstractCalibrationExcelReader> calibrationReader;

	@Autowired
	public ExcelFeatureContentImporter(List<ExcelReader> readers, List<AbstractCalibrationExcelReader> calibrationReader) {
		this.readers = readers;
		this.calibrationReader = calibrationReader;
	}

	// TODO if AbstractProcesExcelReader is obsolete and removed,
	// change type of readers to AbstractExcelReader and replace
	// cleanedReaders with readers
	@Override
	public FeatureSet importFromFile(Path contentPath) {
		LOGGER.debug("enter getInputDataFromFile with file {}", contentPath);

		Objects.requireNonNull(contentPath, "The parameter contentPath must not be null.");

		FeatureSet featureSet;

		List<ExcelReader> cleanedReader =  readers.stream().filter(
				f -> !(f instanceof AbstractCalibrationExcelReader)).collect(Collectors.toList());
		
		if (cleanedReader.isEmpty()) {
			LOGGER.debug("no file readers available, return null");
			return null;
		}
		MainReader<Workbook> mainReader = new MainReader<>(new ExcelInspector(), cleanedReader);

		String readerID = mainReader.getFirstReaderID(contentPath, FeatureSet.class);
		if (readerID == null) {
			return null;
		}

		LOGGER.debug("choosing reader with id {}", readerID);

		Object object = mainReader.parseFile(contentPath, FeatureSet.class, readerID);
		if (FeatureSet.class.isInstance(object)) {
			featureSet = (FeatureSet) object;
		} else {
			LOGGER.debug("returned object {} from reader has wrong type, expecting {}", null,
					FeatureSet.class);
			return null;
		}

		LOGGER.debug("leave getInputDataFromFile, return {}", featureSet);
		return featureSet;
	}

	@Override
	public boolean canHandle(Path path) {
		try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(path.toFile()))) {
			FileMagic magic = FileMagic.valueOf(is);
			return magic == FileMagic.OOXML || magic == FileMagic.OLE2;
		} catch (IOException e) {
			LOGGER.error("An error occured",e);
		}
		return false;

	}

	@SuppressWarnings("unchecked")
	@Override
	public List<RTICalibrationData> importCalibrationData(Path calibrationPath) {
		LOGGER.debug("enter getInputDataFromFile with file {}", calibrationPath);

		List<RTICalibrationData> calibrationsData = new ArrayList<>();
		
		Objects.requireNonNull(calibrationPath, "The parameter calibrationPath must not be null.");

		if (calibrationReader.isEmpty()) {
			LOGGER.debug("no file readers available, return null");
			return calibrationsData;
		}
		
		MainReader<Workbook> mainReader = new MainReader<>(new ExcelInspector(), calibrationReader);

		String readerID = mainReader.getFirstReaderID(calibrationPath, List.class);
		if (readerID == null) {
			return Collections.emptyList();
		}

		LOGGER.debug("choosing reader with id {}", readerID);

		Object object = mainReader.parseFile(calibrationPath, List.class, readerID);
		if (List.class.isInstance(object)) {
			calibrationsData = (List<RTICalibrationData>) object;
		} else {
			LOGGER.debug("returned object {} from reader has wrong type, expecting {}", calibrationsData,
					List.class);
			return calibrationsData;
		}

		LOGGER.debug("leave importCalibrationData, return {}", calibrationsData);
		return calibrationsData;
	}
	
}