package de.hswt.fi.application.regression;

import de.hswt.fi.common.Ionisation;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Input files of the regression tests: sample files of the repository for all supported import
 * formats, with the processing parameters a user would choose in the UI.
 */
final class RegressionInput {

	private static final Ionisation POS = Ionisation.POSITIVE_IONISATION;
	private static final Ionisation NEG = Ionisation.NEGATIVE_IONISATION;

	private static final String FORMATS = "fileimport.service.excel.formats/src/test/resources/";
	private static final String MZML = "resources/mzml/Unif mzML Files/";

	/** All inputs; "process" marks the ones that also run through the complete processing. */
	static final List<RegressionInput> ALL = Arrays.asList(
			// Sciex text exports
			new RegressionInput("txt-01-suspect", "resources/txt/01_FI-Test_Suspect-Screening_ESIpos.txt", null, POS, true),
			new RegressionInput("txt-02-nontarget-no-formula", "resources/txt/02_FI-Test_Non-Target-Screening_ESIpos_no Formula.txt", null, POS, true),
			new RegressionInput("txt-03-nontarget-formula", "resources/txt/03_FI-Test_Non-Target-Screening_ESIpos_inkl Formula.txt", null, POS, true),
			new RegressionInput("txt-04-wwtp-no-formula", "resources/txt/04_FI-Test_Non-Target-Screening_ESIpos_WWTP_no Formula.txt", null, POS, false),
			new RegressionInput("txt-05-wwtp-formula", "resources/txt/05_FI-Test_Non-Target-Screening_ESIpos_WWTP_inkl Formula.txt", null, POS, false),
			new RegressionInput("txt-sciex-short", "resources/txt/sciex-File_short.txt", null, POS, true),
			new RegressionInput("txt-sciex-example", "fileimport.service.txt/src/test/resources/exampleSciex.txt", null, POS, true),
			// Agilent CEF
			new RegressionInput("cef-example", "resources/cef/example.cef", null, POS, true),
			new RegressionInput("cef-pfna-neg", "resources/cef/UTue014_9171_Perfluorononanoic-acid_neg_4.cef", null, NEG, true),
			new RegressionInput("cef-xml-example", "fileimport.service.xml/src/test/resources/exampleCef.cef", null, POS, true),
			// Excel with separate RTI calibration file
			new RegressionInput("excel-data-pos-rti", "resources/excel/Data_pos.xlsx", "resources/excel/RTI_Calibration_File.xlsx", POS, true),
			// Vendor Excel templates (with RTI calibration sheets)
			new RegressionInput("excel-absciex", FORMATS + "template_absciex_rti.xlsx", null, POS, true),
			new RegressionInput("excel-agilent-profinder", FORMATS + "template_agilent_profinder_rti.xlsx", null, POS, true),
			new RegressionInput("excel-agilent", FORMATS + "template_agilent_rti.xls", null, POS, true),
			new RegressionInput("excel-sciex-msms", FORMATS + "template_sciex_msms_rti.xlsx", null, POS, true),
			new RegressionInput("excel-sciex", FORMATS + "template_sciex_rti.xlsx", null, POS, true),
			new RegressionInput("excel-thermo-cieve", FORMATS + "template_thermo_cieve_rti.xlsx", null, POS, true),
			new RegressionInput("excel-thermo-compound-discoverer", FORMATS + "template_thermo_compound_discoverer_rti.xlsx", null, POS, true),
			new RegressionInput("excel-waters-markerlynx-marker", FORMATS + "template_waters_marker_lynx_marker_rti.xlsx", null, POS, true),
			new RegressionInput("excel-waters-markerlynx-result", FORMATS + "template_waters_marker_lynx_result.xlsx", null, POS, true),
			new RegressionInput("excel-waters-unifi", FORMATS + "template_waters_unifi_rti.xlsx", null, POS, true),
			// JSON
			new RegressionInput("json-example", "fileimport.service.json/src/test/resources/example.json", null, POS, true),
			// mzML (proteomics examples from the HUPO-PSI specification, import only)
			new RegressionInput("mzml-accurate-mass-hdmse", MZML + "Accurate Mass Screening Analysis HDMSe Data/SynaptG2S_ WIVI_27MARCH2012_165 [27] [1].mzml", null, POS, false),
			new RegressionInput("mzml-intact-protein-07", MZML + "Intact Protein Analysis MS Data/070822_UAA014_WBC_RedmAb_07 [12,B] [1].mzml", null, POS, false),
			new RegressionInput("mzml-peptide-dda", MZML + "Peptide Mapping Analysis DDA Data/JPW_06 Jan11_002 [22] [1].mzml", null, POS, false),
			new RegressionInput("mzml-glycan-dda", MZML + "Released Glycan Analysis DDA Data/YQY_110410_04 [21] [1].mzml", null, POS, false),
			new RegressionInput("mzml-top-down-msms", MZML + "Top Down Analysis MSMS Data/BCA_MSMS_004 [1].mzml", null, POS, false)
	);

	final String id;
	final String file;
	final String calibrationFile;
	final Ionisation ionisation;
	final boolean process;

	private RegressionInput(String id, String file, String calibrationFile, Ionisation ionisation, boolean process) {
		this.id = id;
		this.file = file;
		this.calibrationFile = calibrationFile;
		this.ionisation = ionisation;
		this.process = process;
	}

	Path path(Path rootDir) {
		return rootDir.resolve(file);
	}

	Path calibrationPath(Path rootDir) {
		return calibrationFile == null ? null : rootDir.resolve(calibrationFile);
	}

	@Override
	public String toString() {
		return id;
	}
}
