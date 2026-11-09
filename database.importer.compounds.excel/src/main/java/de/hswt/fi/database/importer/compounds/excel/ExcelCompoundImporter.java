package de.hswt.fi.database.importer.compounds.excel;

import de.hswt.fi.database.importer.compounds.api.CompoundImporter;
import de.hswt.fi.search.service.mass.search.model.Entry;
import de.hswt.fi.search.service.mass.search.model.SourceList;
import de.hswt.fi.search.service.mass.search.model.properties.*;
import de.hswt.fi.search.service.search.api.CompoundSearchService;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by August Gilg on 05.10.2016.
 */
@Component
@Scope("prototype")
public class ExcelCompoundImporter implements CompoundImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelCompoundImporter.class);
    private static final String TARGET_SHEET_NAME = "Gesamtliste";
    private static final String INDEX = "Index";
    private static final String NAME = "Name";
    private static final String INCHI = "InChI";
    private static final String INCHI_KEY = "InChIKey";
    private static final String CAS = "CAS";
    private static final String SMILES = "SMILES";
    private static final String EC_NUMBER = "EC number";
    private static final String IUPAC = "IUPAC";
    private static final String FORMULA = "Formula";
    private static final String MASS = "Mass";
    private static final String TONNAGE = "Tonnage";
    private static final String HENRY_BOND = "Henry constant (bond) [Pa-m³/mol]";
    private static final String HENRY_GROUP = "Henry constant (group) [Pa-m³/mol]";
    private static final String HENRY_EXPER = "Henry constant (exper database) [Pa-m³/mol]";
    private static final String LOG_P = "LogP";
    private static final String LOG_D = "LogD";
    private static final String ADDITIONAL_NAMES = "Additional Names";
    private static final String MASSBANK_ID = "Massbank IDs";
    private static final String CATEGORIES = "Categories";
    private static final String SID = "Public ID";
    private static final String SOURCE_TAGS = "Source-tag Name";
    private static final String EPA_CHEMISTRY_DASHBOARD_ID = "DTXSID";
    private final DataFormatter formatter;

    private Set<String> mandatoryColumns;
    private Set<String> optionalColumns;
    private Map<String, Integer> columnMap;
    private SimpleDateFormat dateFormat;
    private Date lastModified;
    private CompoundSearchService compoundSearchService;

    public ExcelCompoundImporter() {
        createMandatoryColumnList();
        createOptionalColumnList();
        formatter = new DataFormatter();
    }

    private void createMandatoryColumnList() {
        mandatoryColumns = new HashSet<>();
        mandatoryColumns.add(INDEX);
        mandatoryColumns.add(NAME);
        mandatoryColumns.add(INCHI);
        mandatoryColumns.add(INCHI_KEY);
        mandatoryColumns.add(CAS);
        mandatoryColumns.add(SMILES);
        mandatoryColumns.add(EC_NUMBER);
        mandatoryColumns.add(IUPAC);
        mandatoryColumns.add(FORMULA);
        mandatoryColumns.add(MASS);
        mandatoryColumns.add(TONNAGE);
        mandatoryColumns.add(HENRY_BOND);
        mandatoryColumns.add(HENRY_GROUP);
        mandatoryColumns.add(HENRY_EXPER);
        mandatoryColumns.add(LOG_P);
        mandatoryColumns.add(LOG_D);
        mandatoryColumns.add(ADDITIONAL_NAMES);
        mandatoryColumns.add(MASSBANK_ID);
        mandatoryColumns.add(CATEGORIES);
        mandatoryColumns.add(SID);
        mandatoryColumns.add(SOURCE_TAGS);
    }

    private void createOptionalColumnList() {
        optionalColumns = new HashSet<>();
        optionalColumns.add(EPA_CHEMISTRY_DASHBOARD_ID);
    }

    @Override
    public boolean importCompoundDataSet(Path path, LocalDate date, CompoundSearchService compoundSearchService) {

        if (!path.toFile().exists()) {
            LOGGER.debug("Invalid path to SI-Content file");
            return false;
        }

        if (date == null) {
            date = LocalDate.now();
            LOGGER.debug("No date given, using current date {}", date);
        }

        lastModified = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());

        this.compoundSearchService = compoundSearchService;

        Set<Entry> importedEntries = importEntries(path);

        Set<Entry> entries = importedEntries.stream()
                .filter(distinctByKey(entry -> entry.getInchiKey().getValue()))
                .collect(Collectors.toSet());

        LOGGER.debug("Entries left after removing duplicate InchiKeys: {}", entries.size());


        LOGGER.debug("Entries has Henry bond: {}", importedEntries.stream().filter(entry -> entry.getHenryBond() != null && entry.getHenryBond().getValue() != null).count());



        importedEntries.removeAll(entries);
        importedEntries.forEach(duplicate -> LOGGER.debug(duplicate.getPublicID() + "\t" + duplicate.getInchiKey() + "\t" + duplicate.getName()));

        entries.forEach(entry -> entry.setDatasourceName(compoundSearchService.getDatasourceName()));

        Set<CategoryStringProperty> categories = getUniqueCategories(entries);
        Set<SourceList> sourceLists = getUniqueLists(entries);

        entries.forEach(entry -> updateCategories(entry, categories));
        entries.forEach(entry -> updateSourceLists(entry, sourceLists));

        compoundSearchService.writeDatabase(entries, sourceLists, categories);

        return true;
    }

    private <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        final Set<Object> seen = new HashSet<>();
        return t -> seen.add(keyExtractor.apply(t));
    }

    private void updateSourceLists(Entry entry, Set<SourceList> sourceLists) {
        Set<SourceList> updateSourceLists = sourceLists.stream()
                .filter(sourceList -> entry.getSourceLists().contains(sourceList))
                .collect(Collectors.toSet());

        entry.getSourceLists().clear();
        entry.getSourceLists().addAll(updateSourceLists);
    }

    private void updateCategories(Entry entry, Set<CategoryStringProperty> categories) {

        Set<CategoryStringProperty> updateCategories = categories.stream()
                .filter(category -> entry.getCategories().contains(category))
                .collect(Collectors.toSet());

        entry.getCategories().clear();
        entry.getCategories().addAll(updateCategories);
    }

    private Set<CategoryStringProperty> getUniqueCategories(Set<Entry> entries) {
        return entries.stream()
                .map(Entry::getCategories)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    private Set<SourceList> getUniqueLists(Set<Entry> entries) {
        return entries.stream()
                .map(Entry::getSourceLists)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Entry> importEntries(Path path) {

        try (Workbook workbook = WorkbookFactory.create(path.toFile(), null, true)) {

            List<Entry> entries = new ArrayList<>();

            if (workbook == null || !createColumnIdMap(workbook)) {
                LOGGER.debug("No valid SI data file found");
                return Collections.emptySet();
            }

            LOGGER.debug("Valid SI data file found");
            dateFormat = new SimpleDateFormat("yyyy.mm.dd");

            Sheet sheet = workbook.getSheet(TARGET_SHEET_NAME);

            int firstRowIndex = sheet.getFirstRowNum() + 1;
            int lastRowIndex = sheet.getLastRowNum();

            Entry.EntryBuilder builder = null;

            // For all indices
            for (int i = firstRowIndex; i <= lastRowIndex; i++) {

                Row currentRow = sheet.getRow(i);

                if (currentRow.getFirstCellNum() == -1) {
                    continue;
                }

                int currentIndex = (int) currentRow.getCell(currentRow.getFirstCellNum()).getNumericCellValue();

                Row nextRow = sheet.getRow(i + 1);
                int nextIndex = -1;

                if (nextRow != null && nextRow.getCell(currentRow.getFirstCellNum()) != null) {
                    nextIndex = (int) nextRow.getCell(currentRow.getFirstCellNum()).getNumericCellValue();
                }

                if (builder == null) {
                    builder = Entry.EntryBuilder.newBuilder();
                    builder.withLastModified(lastModified);
                    importSingleEntries(currentRow, builder, currentIndex);
                    concatMultiRowValues(currentRow, builder);

                } else {
                    concatMultiRowValues(currentRow, builder);
                }

                if (nextIndex != currentIndex) {
                    entries.add(builder.build());
                    builder = null;
                }

            }

            LOGGER.debug("Imported entries: {}", entries.size());

            return new HashSet<>(entries);

        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }

        return Collections.emptySet();
    }

    private void concatMultiRowValues(Row currentRow, Entry.EntryBuilder builder) {

        if (isColumnValuePresent(ADDITIONAL_NAMES, currentRow)) {
            builder.withAdditionalName(parseEmbeddedStringValueProperty(currentRow,
                    columnMap.get(ADDITIONAL_NAMES), AdditionalNameStringProperty.class));
        }

        if (isColumnValuePresent(CATEGORIES, currentRow)) {
            builder.withCategory(parseEmbeddedStringValueProperty(currentRow,
                    columnMap.get(CATEGORIES), CategoryStringProperty.class));
        }

        if (isColumnValuePresent(MASSBANK_ID, currentRow)) {
            builder.withMassBankId(parseEmbeddedStringValueProperty(currentRow,
                    columnMap.get(MASSBANK_ID), MassBankIdStringProperty.class));
        }

        if (isColumnValuePresent(SOURCE_TAGS, currentRow)) {
            String sourceListName = currentRow.getCell(columnMap.get(SOURCE_TAGS)).getStringCellValue();
            String description = currentRow.getCell(columnMap.get(SOURCE_TAGS) + 1).getStringCellValue();
            builder.withSourceList(new SourceList(sourceListName, description));
        }

        if (currentRow.getCell(columnMap.get(LOG_P)) != null) {
            builder.withLogPValue(parseNumberValuePropertyWithPH(currentRow, columnMap.get(LOG_P), LogPNumberProperty.class));
        }

        if (currentRow.getCell(columnMap.get(LOG_D)) != null) {
            builder.withLogDValue(parseNumberValuePropertyWithPH(currentRow, columnMap.get(LOG_D), LogDNumberProperty.class));
        }
    }

    private boolean isColumnValuePresent(String columnIdentifier, Row row) {
        return columnIdentifier != null && columnMap.get(columnIdentifier) != null &&
                row.getCell(columnMap.get(columnIdentifier)) != null &&
                !getStringValue(row, columnMap.get(columnIdentifier)).isEmpty();
    }

    private void importSingleEntries(Row currentRow, Entry.EntryBuilder builder, int currentIndex) {

        String id = currentRow.getCell(columnMap.get(SID)).getStringCellValue();
        if (id.isEmpty()) {
            id = compoundSearchService.getIdPrefix() + String.format("%08d", currentIndex);
        }
        builder.withPublicID(id);

        builder.withName(parseEmbeddedStringValueProperty(currentRow, columnMap.get(NAME), NameStringProperty.class));
        builder.withInchi(parseEmbeddedStringValueProperty(currentRow, columnMap.get(INCHI), InchiStringProperty.class));
        builder.withInchiKey(parseEmbeddedStringValueProperty(currentRow, columnMap.get(INCHI_KEY), InchiKeyStringProperty.class));
        builder.withCas(parseEmbeddedStringValueProperty(currentRow, columnMap.get(CAS), CasStringProperty.class));
        builder.withSmiles(parseEmbeddedStringValueProperty(currentRow, columnMap.get(SMILES), SMILESStringProperty.class));
        builder.withEcNumber(parseEmbeddedStringValueProperty(currentRow, columnMap.get(EC_NUMBER), ECNumberStringProperty.class));
        builder.withIupac(parseEmbeddedStringValueProperty(currentRow, columnMap.get(IUPAC), IUPACStringProperty.class));
        builder.withElementalFormula(parseEmbeddedStringValueProperty(currentRow, columnMap.get(FORMULA), FormulaStringProperty.class));
        builder.withTonnage(parseEmbeddedStringValueProperty(currentRow, columnMap.get(TONNAGE), TonnageStringProperty.class));

        builder.withHenryBond(parseNumberValueProperty(currentRow, columnMap.get(HENRY_BOND), HenryConstantBondNumberProperty.class));
        builder.withHenryGroup(parseNumberValueProperty(currentRow, columnMap.get(HENRY_GROUP), HenryConstantGroupNumberProperty.class));
        builder.withHenryExper(parseNumberValueProperty(currentRow, columnMap.get(HENRY_EXPER), HenryConstantExperNumberProperty.class));

        if (isColumnValuePresent(EPA_CHEMISTRY_DASHBOARD_ID, currentRow)) {
            builder.withDtxsid(currentRow.getCell(columnMap.get(EPA_CHEMISTRY_DASHBOARD_ID)).getStringCellValue());
        }

        try {
            builder.withAccurateMass(parseNumberValuePropertyWithPH(currentRow, columnMap.get(MASS), MassNumberProperty.class));
        } catch (IllegalArgumentException e) {
            LOGGER.debug(e.getMessage());
        }
    }

    private <T extends NumberValueProperty> T parseNumberValueProperty(Row row, Integer columnId, Class<T> type) {

        Double value = null;

        try {
            String dotString = getStringValue(row, columnId).replace(",", ".");
            value = Double.parseDouble(dotString);
        } catch (NumberFormatException e1) {
            // Not showing parse errors
        } catch (IllegalStateException e) {
            LOGGER.error(e.getMessage());
        }

        String source = getStringValue(row, columnId + 1);
        Long date = parseDate(row, columnId + 2);

        String editor = getStringValue(row, columnId + 3);
        String additional = getStringValue(row, columnId + 4);

        try {
            T valueProperty = type.newInstance();
            valueProperty.setValue(value);
            valueProperty.setPh(null);
            valueProperty.setCharge(null);
            valueProperty.setSource(source);
            valueProperty.setEditor(editor);
            valueProperty.setAdditional(additional);
            valueProperty.setLastModified(new Date(date == null ? System.currentTimeMillis() : date));
            return valueProperty;
        } catch (InstantiationException | IllegalAccessException e) {
            LOGGER.error(e.getMessage());
        }

        return null;
    }

    private <T extends NumberValueProperty> T parseNumberValuePropertyWithPH(Row row, Integer columnId, Class<T> type) {

        Double value = null;
        Double ph = null;
        Integer charge = null;

        try {
            value = Double.parseDouble(getStringValue(row, columnId).replace(',', '.'));
            ph = Double.parseDouble(getStringValue(row, columnId + 1).replace(',', '.'));
            charge = Integer.parseInt(getStringValue(row, columnId + 2).replace(',', '.'));
        } catch (NumberFormatException e1) {
        } catch (IllegalStateException e) {
            // Do not log/throw if optional cell chas wrong cell type
        }

        String source = getStringValue(row, columnId + 3);
        Long date = parseDate(row, columnId + 4);

        String editor = getStringValue(row, columnId + 5);
        String additional = getStringValue(row, columnId + 6);

        try {
            T valueProperty = type.newInstance();
            valueProperty.setValue(value);
            valueProperty.setPh(ph);
            valueProperty.setCharge(charge);
            valueProperty.setSource(source);
            valueProperty.setEditor(editor);
            valueProperty.setAdditional(additional);
            valueProperty.setLastModified(new Date(date == null ? System.currentTimeMillis() : date));
            return valueProperty;
        } catch (InstantiationException | IllegalAccessException e) {
            LOGGER.error(e.getMessage());
        }

        return null;
    }

    private <T extends AbstractStringProperty> T parseEmbeddedStringValueProperty(Row row, int columnId, Class<T> type) {

        String value = getStringValue(row, columnId);
        String source = getStringValue(row, columnId + 1);
        Long date = parseDate(row, columnId + 2);
        String editor = getStringValue(row, columnId + 3);
        String additional = getStringValue(row, columnId + 4);

        return createSpecificStringValueProperty(type, value, source, date, editor, additional);
    }

    private String getStringValue(Row row, int columnId) {
        Cell cell = row.getCell(columnId);
        return cell != null ? formatter.formatCellValue(cell) : "";
    }

    private Long parseDate(Row row, int columnId) {

        String stringDateValue = getStringValue(row, columnId);

        if (!stringDateValue.isEmpty()) {
            try {
                return dateFormat.parse(stringDateValue).getTime();
            } catch (ParseException e) {
                LOGGER.error("Cannot parse date {}", stringDateValue);
            }
        }
        return null;
    }

    private <T extends AbstractStringProperty> T createSpecificStringValueProperty(
            Class<T> clazz, String value, String source, Long date, String editor, String additional) {

        try {
            T valueProperty = clazz.newInstance();
            valueProperty.setValue(value);
            valueProperty.setSource(source);
            valueProperty.setEditor(editor);
            valueProperty.setAdditional(additional);
            valueProperty.setLastModified(date == null ? lastModified : new Date(date));
            return valueProperty;
        } catch (InstantiationException | IllegalAccessException e) {
            LOGGER.error(e.getMessage());
        }

        return null;
    }

    private boolean createColumnIdMap(Workbook workbook) {

        columnMap = new HashMap<>();

        Sheet sheet = workbook.getSheet(TARGET_SHEET_NAME);

        if (sheet == null) {
            LOGGER.debug("Could not find sheet [{}]", TARGET_SHEET_NAME);
            return false;
        }

        Row headerRow = sheet.getRow(sheet.getFirstRowNum());

        determineColumnIndices(optionalColumns, headerRow);
        return determineColumnIndices(mandatoryColumns, headerRow);
    }

    private boolean determineColumnIndices(Set<String> columnSet, Row headerRow) {

        for (String column : columnSet) {
            for (int i = headerRow.getFirstCellNum(); i < headerRow.getLastCellNum(); i++) {
                String val = headerRow.getCell(i).getStringCellValue();
                if (column.equals(val)) {
                    LOGGER.debug("Add column {} with index {}", val, i);
                    columnMap.put(column, i);
                    break;
                }
            }

            if (!columnMap.containsKey(column)) {
                LOGGER.debug("Could not find column {}", column);
                return false;
            }
        }

        return true;
    }
}
