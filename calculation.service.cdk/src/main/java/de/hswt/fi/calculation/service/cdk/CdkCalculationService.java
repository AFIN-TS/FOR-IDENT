package de.hswt.fi.calculation.service.cdk;

import de.hswt.fi.calculation.service.api.CalculationService;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.formula.MolecularFormula;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.renderer.AtomContainerRenderer;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.generators.BasicAtomGenerator;
import org.openscience.cdk.renderer.generators.BasicBondGenerator;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.generators.IGenerator;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Marco Luthardt
 */
@Component
public class CdkCalculationService implements CalculationService {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(CdkCalculationService.class);

	private IChemObjectBuilder formulaBuilder;

	private List<IGenerator<IAtomContainer>> generators;
	
	private SmilesParser smilesParser;

	private InChIGeneratorFactory inChIGeneratorFactory;

	public CdkCalculationService() {

		formulaBuilder = new MolecularFormula().getBuilder();
		smilesParser = new SmilesParser(DefaultChemObjectBuilder.getInstance());

		try {
			inChIGeneratorFactory = InChIGeneratorFactory.getInstance();
		} catch (CDKException e) {
			LOGGER.error("An error occured during initalization", e);
		}

		generators = new ArrayList<>();
		generators.add(new BasicSceneGenerator());
		generators.add(new BasicBondGenerator());
		generators.add(new BasicAtomGenerator());
	}

	/**
	 * Checks if the tool box is available. It could not be available if no
	 * valid license file could be found by the tool box.
	 *
	 * @return true, if the tool box is available, otherwise false
	 */
	@Override
	public boolean isAvailable() {
		return true;
	}

	/**
	 * Creates a structure image from the given smiles code. If no image could
	 * be created, e.g. the SMILES is not valid, null is returned.
	 *
	 * @param smiles
	 *            the SMILES Code to create a image of
	 * @param width
	 *            the width of the image
	 * @param height
	 *            the height of the image
	 * @return the image as byte array, or null if image could not be created
	 */
	@Override
	public byte[] getSmilesAsImage(String smiles, int width, int height) {
		// the draw area and the image should be the same size
		Rectangle drawArea = new Rectangle(width, height);
		Image image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		IAtomContainer substance;
		try {
			substance = smilesParser.parseSmiles(smiles);
		} catch (InvalidSmilesException e) {
			LOGGER.error("An error occured inside getSmilesAsImage", e);
			return new byte[0];
		}
		StructureDiagramGenerator sdg = new StructureDiagramGenerator();
		try {
			AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(substance);
			sdg.setMolecule(substance);
			sdg.generateCoordinates();
		} catch (CDKException e) {
			LOGGER.warn("Warning: Could not draw molecule with SMILES {}", smiles);
			return new byte[0];
		}

		// the renderer needs to have a toolkit-specific font manager
		AtomContainerRenderer renderer = new AtomContainerRenderer(generators,
				new AWTFontManager());

		// the call to 'setup' only needs to be done on the first paint
		renderer.setup(sdg.getMolecule(), drawArea);

		// paint the background
		Graphics2D g2 = (Graphics2D) image.getGraphics();
		g2.setColor(Color.WHITE);
		g2.fillRect(0, 0, width, height);

		// the paint method also needs a toolkit-specific renderer
		renderer.paint(sdg.getMolecule(), new AWTDrawVisitor(g2), drawArea, false);

		byte[] imageData = null;

		try(ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ImageIO.write((RenderedImage) image, "png", baos);
			baos.flush();
			imageData = baos.toByteArray();
		} catch (IOException e) {
			LOGGER.error("An error occured", e);
		}

		return imageData;
	}

	/**
	 * Returns the mass from formula.
	 *
	 * @param formula
	 *            the formula to get the mass from
	 * @return the calculated mass
	 */
	@Override
	public Double getMassFromFormula(String formula) {
		if (formula == null || formula.isEmpty() || formula.toLowerCase().contains("formula") || formula.contains(" ")) {
			return null;
		}
		try {
			return MolecularFormulaManipulator.getTotalExactMass(
					MolecularFormulaManipulator.getMolecularFormula(formula, formulaBuilder));
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}
		return null;
	}

	@Override
	public List<String> getInvalidSmiles(List<String> smilesList) {

		List<String> invalid = new ArrayList<>();
		
		for(String smiles : smilesList) {
			try {
				smilesParser.parseSmiles(smiles);
			} catch (InvalidSmilesException e1) {
				invalid.add(smiles);
			}
		}
		
		return invalid;
	}

	public String getMolecularFormula(String smiles) {
		String formula = null;
		try {
			formula = MolecularFormulaManipulator.getString(MolecularFormulaManipulator
					.getMolecularFormula(smilesParser.parseSmiles(smiles)));
		} catch (InvalidSmilesException e) {
			LOGGER.error("An error occured during getMolecularFormula", e);
		}
		return formula;
	}

	@Override
	public String getInChi(String smiles) {
		IAtomContainer molecule;
		String inchi = "";
		try {
			molecule = smilesParser.parseSmiles(smiles);
			inchi = inChIGeneratorFactory.getInChIGenerator(molecule).getInchi();
		} catch (CDKException e) {
			LOGGER.error("An error occured during getInchi", e);
		}
		return inchi;
	}

	@Override
	public String getInChiKey(String smiles) {
		IAtomContainer molecule;
		String inchiKey = "";
		try {
			molecule = smilesParser.parseSmiles(smiles);
			inchiKey = inChIGeneratorFactory.getInChIGenerator(molecule).getInchiKey();
		} catch (CDKException e) {
			LOGGER.error("An error occured during getInchiKey", e);
		}

		return inchiKey;
	}


}
