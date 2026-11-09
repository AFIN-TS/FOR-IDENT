package de.hswt.fi.database.importer.compounds.excel;

import de.hswt.fi.database.importer.compounds.api.CompoundImporter;
import de.hswt.fi.search.service.mass.search.model.Entry;
import de.hswt.fi.search.service.mass.search.model.SourceList;
import de.hswt.fi.search.service.mass.search.model.properties.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by August Gilg on 12.10.2016.
 */
public class SIContentImporterTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(SIContentImporterTest.class);

	private CompoundImporter importer;

	private SimpleDateFormat sdf;

	private List<Entry> referenceEntries;

	@BeforeEach
	public void setUp() {
		importer = new ExcelCompoundImporter();
		referenceEntries = createReferenceData();
	}

	private List<Entry> createReferenceData() {

		sdf = new SimpleDateFormat("yyyy.mm.dd");

		List<Entry> referenceEntries = new ArrayList<>();

		// First Reference Entry
		Entry.EntryBuilder builder = Entry.EntryBuilder.newBuilder();

		builder.withName(createStringValueProperty(NameStringProperty.class, "dibutyl phthalate", "REACH", "Anne Bayer", "2014.03.08"));
		builder.withInchi(createStringValueProperty(InchiStringProperty.class, "InChI=1S/C16H22O4/c1-3-5-11-19-15(17)13-9-7-8-10-14(13)16(18)20-12-6-4-2/h7-10H,3-6,11-12H2,1-2H3", "ChemAxon's JChem", "", "2016.07.29"));
		builder.withInchiKey(createStringValueProperty(InchiKeyStringProperty.class, "DOIRQSBPFJWKBE-UHFFFAOYSA-N", "ChemAxon's JChem", "", "2016.07.29"));
		builder.withCas(createStringValueProperty(CasStringProperty.class, "84-74-2", "Drewes et al. 2009, WERF", "", "2015.09.29"));
		builder.withSmiles(createStringValueProperty(SMILESStringProperty.class, "CCCCOC(=O)c1ccccc1C(=O)OCCCC", "NORMAN 2011", "Anne Bayer", "2014.03.06"));
		builder.withEcNumber(createStringValueProperty(ECNumberStringProperty.class, "201-557-4", "COMMISSION REGULATION (EC) No 1451/2007 (ANNEX I)", "Anne Bayer", "2014.02.25"));
		builder.withIupac(createStringValueProperty(IUPACStringProperty.class, "1,2-Benzenedicarboxylic acid, dibutyl ester", "NORMAN 2011", "Anne Bayer", "2014.03.06"));
		builder.withElementalFormula(createStringValueProperty(FormulaStringProperty.class, "C16H22O4", "ChemAxon's JChem", "Anne Bayer", "2016.07.29"));

		builder.withAccurateMass(createNumberValueProperty(MassNumberProperty.class, 278.1518, "CDK", "Anne Bayer", "2016.07.29"));
		builder.withTonnage(createStringValueProperty(TonnageStringProperty.class, "", "", "", "2014.02.02"));

		builder.withLogPValue(createNumberValueProperty(LogPNumberProperty.class, 4.63, "www.chemicalize.org", "Anne Bayer", "2014.02.02"));
		builder.withLogPValue(createNumberValueProperty(LogPNumberProperty.class, 4.70, "PubChem", "", "2015.10.19"));

		builder.withLogDValue(createLogDValueProperty(4.63, 3.0, 0, "ChemAxon's Marvin", "", "2016.10.04"));
		builder.withLogDValue(createLogDValueProperty(4.63, 5.0, 0, "ChemAxon's Marvin", "", "2016.10.04"));
		builder.withLogDValue(createLogDValueProperty(4.63, 7.0, 0, "ChemAxon's Marvin", "", "2016.10.04"));
		builder.withLogDValue(createLogDValueProperty(4.63, 9.0, 0, "ChemAxon's Marvin", "", "2016.10.04"));


		builder.withAdditionalName(createStringValueProperty(AdditionalNameStringProperty.class, "DBP", "UBA", "Anne Bayer", "2014.02.03"));
		builder.withAdditionalName(createStringValueProperty(AdditionalNameStringProperty.class, "Phthalic acid, dibutyl ester", "UBA", "Anne Bayer", "2014.02.03"));
		builder.withAdditionalName(createStringValueProperty(AdditionalNameStringProperty.class, "1,2-Benzenedicarboxylic acid, dibutyl ester", "UBA", "Anne Bayer", "2014.02.03"));
		builder.withAdditionalName(createStringValueProperty(AdditionalNameStringProperty.class, "Dibutyl-phthalate", "UBA", "Anne Bayer", "2014.02.03"));

		builder.withMassBankId(createStringValueProperty(MassBankIdStringProperty.class, "JP001700", "MassBank", "", "2014.08.27"));
		builder.withMassBankId(createStringValueProperty(MassBankIdStringProperty.class, "JP003306", "MassBank", "", "2014.08.27"));
		builder.withMassBankId(createStringValueProperty(MassBankIdStringProperty.class, "JP005814", "MassBank", "", "2014.08.27"));
		builder.withMassBankId(createStringValueProperty(MassBankIdStringProperty.class, "JP006128", "MassBank", "", "2014.08.27"));

		builder.withCategory(createStringValueProperty(CategoryStringProperty.class, "REACH-chemical", "", "", "2016.07.29"));
		builder.withCategory(createStringValueProperty(CategoryStringProperty.class, "Pharmaceuticals", "", "", "2016.07.29"));
		builder.withCategory(createStringValueProperty(CategoryStringProperty.class, "Transformation Products", "", "", "2016.07.29"));
		builder.withCategory(createStringValueProperty(CategoryStringProperty.class, "Biocides", "", "", "2016.07.29"));

		builder.withPublicID("SI00000001");
		builder.withSourceList(new SourceList("STOFF-IDENT", "Database for the identification of previously unknown trace contaminants"));

		builder.withHenryExper(new HenryConstantExperNumberProperty());

		referenceEntries.add(builder.build());

		// Second Reference Entry
		builder = Entry.EntryBuilder.newBuilder();

		builder.withName(createStringValueProperty(NameStringProperty.class, "Wonuk", "UBA", "Anne Bayer", "2014.02.03"));
		builder.withInchi(createStringValueProperty(InchiStringProperty.class, "InChI=1S/C8H14ClN5/c1-4-10-7-12-6(9)13-8(14-7)11-5(2)3/h5H,4H2,1-3H3,(H2,10,11,12,13,14)", "ChemAxon's JChem", "", "2016.07.29"));
		builder.withInchiKey(createStringValueProperty(InchiKeyStringProperty.class, "MXWJVTOOROXGIU-UHFFFAOYSA-N", "ChemAxon's JChem", "", "2016.07.29"));
		builder.withCas(createStringValueProperty(CasStringProperty.class, "1912-24-9", "Drewes et al. 2009, WERF", "", "2015.09.29"));
		builder.withSmiles(createStringValueProperty(SMILESStringProperty.class, "CCNc1nc(Cl)nc(NC(C)C)n1", "www.chemicalize.org", "Anne Bayer", "2014.03.07"));
		builder.withEcNumber(createStringValueProperty(ECNumberStringProperty.class, "217-617-8", "UBA", "Anne Bayer", "2014.02.03"));
		builder.withIupac(createStringValueProperty(IUPACStringProperty.class, "6-chloro-2-N-ethyl-4-N-(propan-2-yl)-1,3,5-triazine-2,4-diamine", "www.chemicalize.org", "Anne Bayer", "2014.03.07"));
		builder.withElementalFormula(createStringValueProperty(FormulaStringProperty.class, "C8H14ClN5", "ChemAxon's JChem", "Anne Bayer", "2016.07.29"));

		builder.withAccurateMass(createNumberValueProperty(MassNumberProperty.class, 215.0938, "CDK", "Anne Bayer", "2016.07.29"));
		builder.withTonnage(createStringValueProperty(TonnageStringProperty.class, "", "", "", "2014.02.02"));

		builder.withLogPValue(createNumberValueProperty(LogPNumberProperty.class, 2.20, "www.chemicalize.org", "Anne Bayer", "2014.02.02"));
		builder.withLogPValue(createNumberValueProperty(LogPNumberProperty.class, 2.60, "PubChem", "", "2015.10.19"));

		builder.withLogDValue(createLogDValueProperty(1.79, 3.0, 1, "ChemAxon's Marvin", "", "2016.10.04"));
		builder.withLogDValue(createLogDValueProperty(2.19, 5.0, 0, "ChemAxon's Marvin", "", "2016.10.04"));


		builder.withAdditionalName(createStringValueProperty(AdditionalNameStringProperty.class, "1,3,5-Triazine-2,4-diamine, 6-chloro-N-ethyl-N'-(1-methylethyl)-", "UBA", "Anne Bayer", "2014.02.03"));
		builder.withAdditionalName(createStringValueProperty(AdditionalNameStringProperty.class, "Atrazin", "UBA", "Anne Bayer", "2014.02.03"));

		builder.withMassBankId(createStringValueProperty(MassBankIdStringProperty.class, "EA028801", "MassBank", "", "2014.08.27"));
		builder.withMassBankId(createStringValueProperty(MassBankIdStringProperty.class, "EA028802", "MassBank", "", "2014.08.27"));

		builder.withCategory(createStringValueProperty(CategoryStringProperty.class, "Pharmaceuticals", "", "", "2016.07.29"));
		builder.withCategory(createStringValueProperty(CategoryStringProperty.class, "Transformation Products", "", "", "2016.07.29"));

		builder.withPublicID("SI00000002");
		builder.withSourceList(new SourceList("STOFF-IDENT", "Database for the identification of previously unknown trace contaminants"));

		referenceEntries.add(builder.build());

		// Third Reference Entry
		builder = Entry.EntryBuilder.newBuilder();

		builder.withName(createStringValueProperty(NameStringProperty.class, "Bisphenol A", "BfG,Schüsener 2015", "", "2015.09.29"));
		builder.withInchi(createStringValueProperty(InchiStringProperty.class, "InChI=1S/C15H16O2/c1-15(2,11-3-7-13(16)8-4-11)12-5-9-14(17)10-6-12/h3-10,16-17H,1-2H3", "ChemAxon's JChem", "", "2016.07.29"));
		builder.withInchiKey(createStringValueProperty(InchiKeyStringProperty.class, "IISBACLAFKSPIT-UHFFFAOYSA-N", "ChemAxon's JChem", "", "2016.07.29"));
		builder.withCas(createStringValueProperty(CasStringProperty.class, "80-05-7", "Drewes et al. 2009, WERF", "", "2015.09.29"));
		builder.withSmiles(createStringValueProperty(SMILESStringProperty.class, "CC(C)(c1ccc(O)cc1)c1ccc(O)cc1", "PubChem", "", "2015.10.19"));
		builder.withEcNumber(createStringValueProperty(ECNumberStringProperty.class, "201-245-8", "PubChem", "", "2015.10.19"));
		builder.withIupac(createStringValueProperty(IUPACStringProperty.class, "4-[2-(4-hydroxyphenyl)propan-2-yl]phenol", "PubChem", "", "2015.10.19"));
		builder.withElementalFormula(createStringValueProperty(FormulaStringProperty.class, "C15H16O2", "ChemAxon's JChem", "", "2016.07.29"));

		builder.withAccurateMass(createNumberValueProperty(MassNumberProperty.class, 228.1150, "CDK", "", "2016.07.29"));
		builder.withTonnage(createStringValueProperty(TonnageStringProperty.class, "", "", "", "2014.02.02"));

		builder.withLogPValue(createNumberValueProperty(LogPNumberProperty.class, 3.30, "PubChem", "", "2015.10.19"));

		builder.withLogDValue(createLogDValueProperty(4.04, 3.0, 0, "ChemAxon's Marvin", "", "2016.10.04"));
		builder.withLogDValue(createLogDValueProperty(4.04, 5.0, 0, "ChemAxon's Marvin", "", "2016.10.04"));
		builder.withLogDValue(createLogDValueProperty(4.04, 7.0, 0, "ChemAxon's Marvin", "", "2016.10.04"));


		builder.withAdditionalName(createStringValueProperty(AdditionalNameStringProperty.class, "Bisphenol A", "Drewes et al. 2009, WERF", "", "2015.10.19"));
		builder.withAdditionalName(createStringValueProperty(AdditionalNameStringProperty.class, "4,4-isopropylidenediphenol", "High Production Chemical Database, Colorado School of Mines/Southern Nevada Water Auhority", "", "2015.09.29"));
		builder.withAdditionalName(createStringValueProperty(AdditionalNameStringProperty.class, "2,2-Bis(4-hydroxyphenyl)propane", "PubChem", "", "2015.10.19"));

		builder.withCategory(createStringValueProperty(CategoryStringProperty.class, "Uncategorized", "", "", "2016.07.29"));

		builder.withPublicID("SI00000003");
		builder.withSourceList(new SourceList("STOFF-IDENT", "Database for the identification of previously unknown trace contaminants"));

		referenceEntries.add(builder.build());

		// Fourth Reference Entry
		builder = Entry.EntryBuilder.newBuilder();

		builder.withName(createStringValueProperty(NameStringProperty.class, "benzophenone", "REACH", "Anne Bayer", "2014.03.08"));
		builder.withInchi(createStringValueProperty(InchiStringProperty.class, "InChI=1S/C13H10O/c14-13(11-7-3-1-4-8-11)12-9-5-2-6-10-12/h1-10H", "ChemAxon's JChem", "", "2016.07.29"));
		builder.withInchiKey(createStringValueProperty(InchiKeyStringProperty.class, "RWCCWEUUXYIKHB-UHFFFAOYSA-N", "ChemAxon's JChem", "", "2016.07.29"));
		builder.withCas(createStringValueProperty(CasStringProperty.class, "119-61-9", "Drewes et al. 2009, WERF", "", "2015.09.29"));
		builder.withSmiles(createStringValueProperty(SMILESStringProperty.class, "O=C(c1ccccc1)c1ccccc1", "NORMAN 2011", "Anne Bayer", "2014.03.06"));
		builder.withEcNumber(createStringValueProperty(ECNumberStringProperty.class, "204-337-6", "UBA", "Anne Bayer", "2014.02.03"));
		builder.withIupac(createStringValueProperty(IUPACStringProperty.class, "diphenylmethanone", "NORMAN 2011", "Anne Bayer", "2014.03.18"));
		builder.withElementalFormula(createStringValueProperty(FormulaStringProperty.class, "C13H10O", "ChemAxon's JChem", "Anne Bayer", "2016.07.29"));

		builder.withAccurateMass(createNumberValueProperty(MassNumberProperty.class, 182.0732, "CDK", "Anne Bayer", "2016.07.29"));
		builder.withTonnage(createStringValueProperty(TonnageStringProperty.class, "", "", "", "2014.02.02"));

		builder.withLogPValue(createNumberValueProperty(LogPNumberProperty.class, 3.43, "www.chemicalize.org", "Anne Bayer", "2014.02.02"));
		builder.withLogDValue(createLogDValueProperty(3.43, 3.0, 0, "ChemAxon's Marvin", "", "2016.10.04"));


		builder.withAdditionalName(createStringValueProperty(AdditionalNameStringProperty.class, "Methanone, diphenyl-", "UBA", "Anne Bayer", "2014.02.03"));

		builder.withMassBankId(createStringValueProperty(MassBankIdStringProperty.class, "JP001010", "MassBank", "", "2014.08.27"));

		builder.withCategory(createStringValueProperty(CategoryStringProperty.class, "REACH-chemical", "", "", "2016.07.29"));

		builder.withPublicID("SI00000004");
		builder.withSourceList(new SourceList("STOFF-IDENT", "Database for the identification of previously unknown trace contaminants"));

		referenceEntries.add(builder.build());

		// Fifth Reference Entry
		builder = Entry.EntryBuilder.newBuilder();

		builder.withName(createStringValueProperty(NameStringProperty.class, "oxybenzone", "REACH", "Anne Bayer", "2014.03.08"));
		builder.withInchi(createStringValueProperty(InchiStringProperty.class, "InChI=1S/C14H12O3/c1-17-11-7-8-12(13(15)9-11)14(16)10-5-3-2-4-6-10/h2-9,15H,1H3", "ChemAxon's JChem", "", "2016.07.29"));
		builder.withInchiKey(createStringValueProperty(InchiKeyStringProperty.class, "DXGLGDHPHMLXJC-UHFFFAOYSA-N", "ChemAxon's JChem", "", "2016.07.29"));
		builder.withCas(createStringValueProperty(CasStringProperty.class, "131-57-7", "Drewes et al. 2009, WERF", "", "2015.09.29"));
		builder.withSmiles(createStringValueProperty(SMILESStringProperty.class, "COc1ccc(C(=O)c2ccccc2)c(O)c1", "NORMAN 2011", "Anne Bayer", "2014.03.06"));
		builder.withEcNumber(createStringValueProperty(ECNumberStringProperty.class, "205-031-5", "REACH", "Anne Bayer", "2014.01.28"));
		builder.withIupac(createStringValueProperty(IUPACStringProperty.class, "2-Hydroxy-4-methoxybenzophenone", "NORMAN 2011", "Anne Bayer", "2014.03.06"));
		builder.withElementalFormula(createStringValueProperty(FormulaStringProperty.class, "C14H12O3", "ChemAxon's JChem", "Anne Bayer", "2016.07.29"));

		builder.withAccurateMass(createNumberValueProperty(MassNumberProperty.class, 228.0786, "CDK", "Anne Bayer", "2016.07.29"));
		builder.withTonnage(createStringValueProperty(TonnageStringProperty.class, "10 - 100 tonnes per annum", "REACH", "Anne Bayer", "2014.01.28"));

		builder.withLogPValue(createNumberValueProperty(LogPNumberProperty.class, 3.62, "www.chemicalize.org", "Anne Bayer", "2014.01.28"));
		builder.withLogDValue(createLogDValueProperty(3.62, 3.0, 0, "ChemAxon's Marvin", "", "2016.10.04"));


		builder.withAdditionalName(createStringValueProperty(AdditionalNameStringProperty.class, "Oxybenzone", "Zweckverband Landeswasserversorgung Langenau", "", "2015.09.29"));

		builder.withMassBankId(createStringValueProperty(MassBankIdStringProperty.class, "EA023001", "MassBank", "", "2014.08.27"));

		builder.withCategory(createStringValueProperty(CategoryStringProperty.class, "REACH-chemical", "", "", "2016.07.29"));

		builder.withPublicID("SI00000005");
		builder.withSourceList(new SourceList("STOFF-IDENT", "Database for the identification of previously unknown trace contaminants"));

		referenceEntries.add(builder.build());

		LOGGER.debug("Reference set built: " + referenceEntries);

		return referenceEntries;
	}

	@Test
	public void testSIContentImport() throws URISyntaxException {
		LOGGER.debug("Executing Test");
		List<Entry> importedEntries = new ArrayList<>(importer.importEntries(getResourcePath()));

		// Due to mapping / id problems with hash() and equals() in persistence class Entry
		// the comparison of Object's content is managed via JSON

		referenceEntries.sort(Comparator.comparing(Entry::getPublicID, String.CASE_INSENSITIVE_ORDER));
		importedEntries.sort(Comparator.comparing(Entry::getPublicID, String.CASE_INSENSITIVE_ORDER));


		Assertions.assertEquals(referenceEntries.size(), importedEntries.size());

		for (int i = 0; i < referenceEntries.size(); i++) {
			Assertions.assertTrue(propertiesEqual(referenceEntries.get(i), importedEntries.get(i)));
		}
	}

	private boolean propertiesEqual(Entry entry, Entry entry1) {

		return entry.getName().equals(entry1.getName()) && entry.getInchi().equals(entry1.getInchi())
				&& entry.getInchiKey().equals(entry1.getInchiKey()) && entry.getCas().equals(entry1.getCas())
				&& entry.getSmiles().equals(entry1.getSmiles()) && entry.getEcNumber().equals(entry1.getEcNumber())
				&& entry.getIupac().equals(entry1.getIupac()) && entry.getElementalFormula().equals(entry1.getElementalFormula())
				&& entry.getAccurateMass().equals(entry1.getAccurateMass()) && entry.getTonnage().equals(entry1.getTonnage())
				&& entry.getLogdValues().equals(entry.getLogdValues()) && entry.getLogpValues().equals(entry1.getLogpValues())
				&& entry.getAdditionalNames().equals(entry1.getAdditionalNames()) && entry.getMassBankIds().equals(entry1.getMassBankIds())
				&& entry.getPublicID().equals(entry1.getPublicID()) && entry.getSourceLists().equals(entry1.getSourceLists());
	}

	private Path getResourcePath() throws URISyntaxException {
		return Paths.get(Objects.requireNonNull(this.getClass().getClassLoader().getResource("SI_Content_Test_Short.xls")).toURI());
	}

	private Date getTimeFromDateString(String dateString) {
		try {
			return sdf.parse(dateString);
		} catch (ParseException e) {
			LOGGER.error(e.getMessage());
		}
		return null;
	}

	private <T extends StringValueProperty> T createStringValueProperty(Class<T> clazz, String value, String source, String editor, String dateString) {

		try {
			T stringValueProperty = clazz.newInstance();
			stringValueProperty.setValue(value);
			stringValueProperty.setSource(source);
			stringValueProperty.setEditor(editor);
			stringValueProperty.setAdditional("");
			stringValueProperty.setLastModified(getTimeFromDateString(dateString));
			return stringValueProperty;
		} catch (InstantiationException | IllegalAccessException e) {
			LOGGER.error(e.getMessage());
		}

		return null;
	}

	private <T extends AbstractNumberProperty> T createNumberValueProperty(Class<T> clazz, Double value, String source, String editor, String dateString) {

		try {
			T numberValueProperty = clazz.newInstance();
			numberValueProperty.setValue(value);
			numberValueProperty.setSource(source);
			numberValueProperty.setEditor(editor);
			numberValueProperty.setAdditional("");
			numberValueProperty.setLastModified(getTimeFromDateString(dateString));
			return numberValueProperty;
		} catch (InstantiationException | IllegalAccessException e) {
			LOGGER.error(e.getMessage());
		}

		return null;
	}

	private LogDNumberProperty createLogDValueProperty(Double value, Double ph, Integer charge, String source, String editor, String dateString) {

		LogDNumberProperty logDNumberProperty = new LogDNumberProperty();
		logDNumberProperty.setValue(value);
		logDNumberProperty.setPh(ph);
		logDNumberProperty.setCharge(charge);
		logDNumberProperty.setSource(source);
		logDNumberProperty.setEditor(editor);
		logDNumberProperty.setAdditional("");
		logDNumberProperty.setLastModified(getTimeFromDateString(dateString));

		return logDNumberProperty;
	}

}
