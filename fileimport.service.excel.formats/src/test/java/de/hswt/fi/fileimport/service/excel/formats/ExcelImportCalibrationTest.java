package de.hswt.fi.fileimport.service.excel.formats;

import de.hswt.fi.fileimport.service.excel.AbstractCalibrationExcelReader;
import de.hswt.fi.model.RTICalibrationData;
import de.hswt.filehandler.api.MainReader;
import de.hswt.filehandler.excel.ExcelInspector;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ExcelImportCalibrationTest {

	private static final String TEST_FILE_NAME = "RTI_Calibration_File.xlsx";

	private static final Class<List> CONTENT_RETURN_TYPE = List.class;

	private AbstractCalibrationExcelReader calibrationReader;
	
	private ArrayList<RTICalibrationData> data;
	
	@BeforeEach
	public void setupReferenceData() {

		calibrationReader = new CalibrationExcelReaderLC();

		data = new ArrayList<>();
		
		RTICalibrationData rti = new RTICalibrationData("Metformin", -0.92);
		rti.setMeanRt(16.91);
		data.add(rti);
		
		rti = new RTICalibrationData("Chloridazon", 1.11);
		rti.setMeanRt(23.56);
		data.add(rti);
		
		rti = new RTICalibrationData("Carbetamide", 1.65);
		rti.setMeanRt(25.01);
		data.add(rti);
		
		rti = new RTICalibrationData("Monuron", 1.93);
		rti.setMeanRt(25.49);
		data.add(rti);
		
		rti = new RTICalibrationData("Metobromuron", 2.24);
		rti.setMeanRt(27.53);
		data.add(rti);
		
		rti = new RTICalibrationData("Chlorbromuron", 2.85);
		rti.setMeanRt(29.03);
		data.add(rti);
		
		rti = new RTICalibrationData("Metconazole", 3.59);
		rti.setMeanRt(29.92);
		data.add(rti);
		
		rti = new RTICalibrationData("Diazinon", 4.19);
		rti.setMeanRt(31.87);
		data.add(rti);
		
		rti = new RTICalibrationData("Quinoxyfen", 4.98);
		rti.setMeanRt(34.22);
		data.add(rti);
		
		rti = new RTICalibrationData("Fenofibrate", 5.28);
		rti.setMeanRt(35.14);
		data.add(rti);
	}

	@Test
	public void testReadersAutowired() {
		assertNotNull(calibrationReader);
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testImport() throws URISyntaxException {
		
		Path path = Paths.get(this.getClass().getClassLoader().getResource(TEST_FILE_NAME).toURI());
		assertTrue(path.toFile().exists());
		
		MainReader<Workbook> mainReader = new MainReader(new ExcelInspector(), Collections.singleton(calibrationReader));
		String readerID = mainReader.getFirstReaderID(path, CONTENT_RETURN_TYPE);
		Object object = mainReader.parseFile(path, CONTENT_RETURN_TYPE, readerID);
		
		if(CONTENT_RETURN_TYPE.isInstance(object)) {
			List<RTICalibrationData> list = (List<RTICalibrationData>) object;
			assertEquals(10, data.size());
			assertEquals(10, list.size());
			data.removeAll(list);
 			assertTrue(data.isEmpty());
		} else {
			fail();
		}
	}
}