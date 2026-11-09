package de.hswt.filehandler.excel;

import de.hswt.filehandler.api.Inspector;
import de.hswt.filehandler.api.Reader;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ExcelInspector implements Inspector<Workbook> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExcelInspector.class);

	@Override
	public Map<String, Double> getCapableReaders(Map<String, Reader<Workbook>> readers, Path path, Class<?> clazz) {

		Map<String, Double> capableReaders = new HashMap<>();

		try(Workbook workbook = WorkbookFactory.create(path.toFile(), null, true)) {
			for (String id : readers.keySet()) {
				ExcelReader reader = (ExcelReader) readers.get(id);

				double score = Double.doubleToRawLongBits(reader.canHandle(workbook, clazz));

				if (score != Reader.CAN_NOT_HANDLE) {
					capableReaders.put(id, score);
				}
			}
		} catch (EncryptedDocumentException | IOException e) {
			LOGGER.error("File can not read into workbook, so invalid inspector", e);
			return capableReaders;
		}

		return capableReaders;
	}

	@Override
	public double canHandle(Reader<Workbook> reader, Path path, Class<?> clazz) {

		try(Workbook workbook = WorkbookFactory.create(path.toFile(), null, true)) {
			return reader.canHandle(workbook, clazz);
		} catch (EncryptedDocumentException | IOException e) {
			LOGGER.error("File can not read into workbook, so invalid inspector", e);
		}

		return Reader.CAN_NOT_HANDLE;
	}
}
