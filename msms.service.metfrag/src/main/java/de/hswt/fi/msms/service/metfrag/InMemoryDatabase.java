package de.hswt.fi.msms.service.metfrag;

import de.ipbhalle.metfraglib.additionals.MathTools;
import de.ipbhalle.metfraglib.additionals.MoleculeFunctions;
import de.ipbhalle.metfraglib.candidate.TopDownPrecursorCandidate;
import de.ipbhalle.metfraglib.database.AbstractDatabase;
import de.ipbhalle.metfraglib.exceptions.DatabaseIdentifierNotFoundException;
import de.ipbhalle.metfraglib.interfaces.ICandidate;
import de.ipbhalle.metfraglib.list.CandidateList;
import de.ipbhalle.metfraglib.settings.Settings;
import org.openscience.cdk.ChemObject;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.inchi.InChIGenerator;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

import java.util.List;
import java.util.ArrayList;

public class InMemoryDatabase extends AbstractDatabase {
	
	private ArrayList<TopDownPrecursorCandidate> candidates;

	public InMemoryDatabase(Settings settings) {
		super(settings);
	}

	public ArrayList<String> getCandidateIdentifiers(List<String> candidateIdentifiers) {
		if (candidates == null) {
			initialiseCandidatesFromMemory(candidateIdentifiers);
		}
		if (settings.get("PrecursorCompoundIDs") != null) {
			try {
				return getCandidateIdentifiers((String[]) settings.get("PrecursorCompoundIDs"));
			} catch (Exception e) {
				logger.error("An error occured", e);
				return null;
			}
		}
		if (settings.get("NeutralPrecursorMolecularFormula") != null) {
			try {
				return getCandidateIdentifiers(
						(String) settings.get("NeutralPrecursorMolecularFormula"));
			} catch (Exception e) {
				logger.error("An error occured", e);
			}
		}
		// No second mass filter: the candidates come from the mass search of FOR-IDENT and are all scored.
		// (The mass filter used before let all candidates pass because of a wrong comparison; with a correct
		// comparison, candidates at the edge of the ppm window would be dropped, see SP-07.)
		ArrayList<String> identifiers = new ArrayList<>();
		for (TopDownPrecursorCandidate candidate : candidates) {
			identifiers.add(candidate.getIdentifier());
		}
		return identifiers;
	}

	@Override
	public ArrayList<String> getCandidateIdentifiers(double monoisotopicMass, double relativeMassDeviation) {
		ArrayList<String> identifiers = new ArrayList<>();
		double mzabs = MathTools.calculateAbsoluteDeviation(monoisotopicMass,
				relativeMassDeviation);
		double lowerLimit = monoisotopicMass - mzabs;
		double upperLimit = monoisotopicMass + mzabs;
		for (TopDownPrecursorCandidate candidate : candidates) {
			double currentMonoisotopicMass = 0.0D;
			try {
				currentMonoisotopicMass = MoleculeFunctions
						.calculateMonoIsotopicMassImplicitHydrogens(
								MoleculeFunctions.convertExplicitToImplicitHydrogens(
										candidate.getAtomContainer()));
			} catch (Exception e) {
				logger.error("An error occured", e);
			}
			// Mass window around the precursor mass. Formerly "lowerLimit >= mass", which only worked because the
			// mass function of MetFragLib 2.3 ignored the implicit hydrogens (all candidates passed)
			if ((lowerLimit <= currentMonoisotopicMass)
					&& (currentMonoisotopicMass <= upperLimit)) {
				identifiers.add(candidate.getIdentifier());
			}
		}
		return identifiers;
	}

	@Override
	public ArrayList<String> getCandidateIdentifiers(String molecularFormula) {
		ArrayList<String> identifiers = new ArrayList<>();
		IMolecularFormula queryFormula = MolecularFormulaManipulator
				.getMolecularFormula(molecularFormula, new ChemObject().getBuilder());
		for (TopDownPrecursorCandidate candidate : candidates) {
			IMolecularFormula currentFormula = null;
			try {
				currentFormula = MolecularFormulaManipulator.getMolecularFormula(MoleculeFunctions
						.convertExplicitToImplicitHydrogens(candidate.getAtomContainer()));
			} catch (Exception e) {
				logger.error("An error occured", e);
			}
			if (MolecularFormulaManipulator.getString(queryFormula)
					.equals(MolecularFormulaManipulator.getString(currentFormula))) {
				identifiers.add(candidate.getIdentifier());
			}
		}
		return identifiers;
	}

	@Override
	public ArrayList<String> getCandidateIdentifiers(ArrayList<String> identifiers) {
		ArrayList<String> verifiedIdentifiers = new ArrayList<>();
		for (String identifier : identifiers) {
			try {
				getCandidateByIdentifier(identifier);
			} catch (DatabaseIdentifierNotFoundException e) {
				logger.warn("Warning: Candidate identifier " + identifier + " not found.");
				continue;
			}
			verifiedIdentifiers.add(identifier);
		}
		return verifiedIdentifiers;
	}

	@Override
	public ICandidate getCandidateByIdentifier(String identifier)
			throws DatabaseIdentifierNotFoundException {
		int index = indexOfIdentifier(identifier);
		if (index == -1) {
			throw new DatabaseIdentifierNotFoundException(identifier);
		}
		return candidates.get(index);
	}

	@Override
	public CandidateList getCandidateByIdentifier(ArrayList<String> identifiers) {
		CandidateList candidateList = new CandidateList();
		for (String identifier : identifiers) {
			ICandidate candidate = null;
			try {
				candidate = getCandidateByIdentifier(identifier);
			} catch (DatabaseIdentifierNotFoundException e) {
				logger.warn("Candidate identifier " + identifier + " not found.");
			}
			if (candidate != null) {
				candidateList.addElement(candidate);
			}
		}
		return candidateList;
	}

	@Override
	public void nullify() {
		// Not used but needed by interface
	}

	private void initialiseCandidatesFromMemory(List<String> identifier) {
		IAtomContainer[] molecules = (IAtomContainer[]) settings.get("MoleculeInMemory");
		candidates = new ArrayList<>();
		if (molecules == null) {
			return;
		}
		for (int i = 0; i < molecules.length; i++) {
			InChIGenerator generator = null;
			try {
				InChIGeneratorFactory igf = InChIGeneratorFactory.getInstance();
				
				// Smiles which cannot be assigned to kekule structure, create Arrays with null objects
				if(molecules[i] != null) {
					generator = igf.getInChIGenerator(molecules[i]);
				}
			} catch (CDKException e) {
				logger.error("An error occured", e);
			}
			if (generator == null) {
				continue;
			}
			TopDownPrecursorCandidate precursorCandidate = new TopDownPrecursorCandidate(
					generator.getInchi(), identifier.get(i));

			for (Object o : molecules[i].getProperties().keySet()) {
				String key = (String) o;
				precursorCandidate.setProperty(key, molecules[i].getProperty(key));
			}
			try {
				precursorCandidate.setProperty("InChIKey1", generator.getInchiKey().split("-")[0]);
				precursorCandidate.setProperty("InChIKey2", generator.getInchiKey().split("-")[1]);
			} catch (CDKException e) {
				logger.error("An error occured", e);
			}
			precursorCandidate.setProperty("MolecularFormula", generator.getInchi().split("/")[1]);

			candidates.add(precursorCandidate);
		}
	}

	private int indexOfIdentifier(String identifier) {
		for (int i = 0; i < candidates.size(); i++) {
			if (candidates.get(i).getIdentifier().equals(identifier)) {
				return i;
			}
		}
		return -1;
	}
}
