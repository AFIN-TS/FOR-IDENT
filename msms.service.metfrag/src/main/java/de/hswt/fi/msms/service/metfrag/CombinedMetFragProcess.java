package de.hswt.fi.msms.service.metfrag;

import de.ipbhalle.metfraglib.additionals.BondEnergies;
import de.ipbhalle.metfraglib.collection.PostProcessingCandidateFilterCollection;
import de.ipbhalle.metfraglib.collection.PreProcessingCandidateFilterCollection;
import de.ipbhalle.metfraglib.database.LocalPropertyFileDatabase;
import de.ipbhalle.metfraglib.exceptions.ScorePropertyNotDefinedException;
import de.ipbhalle.metfraglib.interfaces.ICandidate;
import de.ipbhalle.metfraglib.interfaces.IPostProcessingCandidateFilter;
import de.ipbhalle.metfraglib.interfaces.IPreProcessingCandidateFilter;
import de.ipbhalle.metfraglib.interfaces.IScoreInitialiser;
import de.ipbhalle.metfraglib.list.AbstractPeakList;
import de.ipbhalle.metfraglib.list.CandidateList;
import de.ipbhalle.metfraglib.list.ScoredCandidateList;
import de.ipbhalle.metfraglib.parameter.ClassNames;
import de.ipbhalle.metfraglib.parameter.VariableNames;
import de.ipbhalle.metfraglib.process.CombinedSingleCandidateMetFragProcess;
import de.ipbhalle.metfraglib.process.ProcessingStatus;
import de.ipbhalle.metfraglib.settings.MetFragGlobalSettings;
import de.ipbhalle.metfraglib.settings.MetFragSingleProcessSettings;
import de.ipbhalle.metfraglib.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CombinedMetFragProcess implements Runnable {

	private Logger LOGGER = LoggerFactory.getLogger(CombinedMetFragProcess.class);

	//settings object containing all parameters
	private MetFragGlobalSettings globalSettings;
	//database object for candidate retrieval
	private InMemoryDatabase database;
	//candidate filters
	private PreProcessingCandidateFilterCollection preProcessingCandidateFilterCollection;
	private PostProcessingCandidateFilterCollection postProcessingCandidateFilterCollection;
	//candidate list -> later also containing the scored candidates
	private CandidateList sortedScoredCandidateList;
	private int numberCandidatesBeforeFilter;
	//threads to process single candidates
	private CombinedSingleCandidateMetFragProcess[] processes;
	//process status object -> stores values about metfrag's processing status
	private ProcessingStatus processingStatus;

	/**
	 * constructore needs settings object
	 * 
	 * @param globalSettings
	 */
	CombinedMetFragProcess(MetFragGlobalSettings globalSettings) {
		this.processes = null;
		this.globalSettings = globalSettings;
		//init processing status object
		//inits database, peaklist reader
		this.initialise();
		//init pre- and post-processing filters
		this.initialiseCandidateFilters();
	}
	
	/*
	 * retrieve the candidates from the database 
	 */
	boolean retrieveCompounds(List<String> identifiers) throws Exception {
		this.processes = null;
		ArrayList<String> databaseCandidateIdentifiers = this.database.getCandidateIdentifiers(identifiers);
		if(this.globalSettings.containsKey(VariableNames.MAXIMUM_CANDIDATE_LIMIT_TO_STOP_NAME) && this.globalSettings.get(VariableNames.MAXIMUM_CANDIDATE_LIMIT_TO_STOP_NAME) != null) {
			int limit = (Integer)this.globalSettings.get(VariableNames.MAXIMUM_CANDIDATE_LIMIT_TO_STOP_NAME);
			if(limit < databaseCandidateIdentifiers.size()) {
				LOGGER.info("{} candidate(s) exceeds the defined limit (MaxCandidateLimitToStop = {})",
						databaseCandidateIdentifiers.size(), limit);
				return false;
			}
		}
		this.sortedScoredCandidateList = this.database.getCandidateByIdentifier(databaseCandidateIdentifiers);
		this.database.nullify();
		numberCandidatesBeforeFilter = this.sortedScoredCandidateList.getNumberElements();
		LOGGER.info("Got {} candidate(s)", numberCandidatesBeforeFilter);
		//in case external property file is defined, initialise external property values
		//TODO: ExternalPropertyPath needs to be added to VariableNames
		if(this.globalSettings.containsKey("ExternalPropertyPath") && this.globalSettings.get("ExternalPropertyPath") != null) {
			this.initExternalProperties(
					(String)this.globalSettings.get("ExternalPropertyPath"),
					this.sortedScoredCandidateList,
					(String[])this.globalSettings.get(VariableNames.METFRAG_SCORE_TYPES_NAME)
			);
		}
		return true;
	}
	
	/*
	 * starts global metfrag process that starts a single thread for each candidate
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		this.processes = null;
		boolean threadStoppedExternally = false;
		this.processes = new CombinedSingleCandidateMetFragProcess[this.sortedScoredCandidateList.getNumberElements()];

		//reset processing status
		this.processingStatus.setProcessStatusString("Processing Candidates");
		this.processingStatus.setNumberCandidates(this.sortedScoredCandidateList.getNumberElements());
		this.processingStatus.setNumberFinishedCandidates(0);
		this.processingStatus.setNextPercentageValue(1);
		//initialise all necessary score parameters
		//these parameters are shared over all single candidate thread instances
		this.initialiseScoresGlobal(this.globalSettings);
		/*
		 * prepare single MetFrag threads
		 */
		for(int i = 0; i < this.sortedScoredCandidateList.getNumberElements(); i++) 
		{
			/*
			 * local settings for each thread stores a reference to the global settings
			 */
			MetFragSingleProcessSettings singleProcessSettings = new MetFragSingleProcessSettings(this.globalSettings);
			/*
			 * necessary to define number of hydrogens and make the implicit
			 */
			CombinedSingleCandidateMetFragProcess scmfp = new CombinedSingleCandidateMetFragProcess(singleProcessSettings, this.sortedScoredCandidateList.getElement(i));
			scmfp.setPreProcessingCandidateFilterCollection(this.preProcessingCandidateFilterCollection);
			
			this.processes[i] = scmfp;
		}
		
		/*
		 * define executer thread to run MetFrag process
		 */
		ExecutorService executer = Executors.newFixedThreadPool((Byte) this.globalSettings.get(VariableNames.NUMBER_THREADS_NAME));
		/* 
		 * ###############
		 * 	run processes
		 * ###############
		 */
		for(CombinedSingleCandidateMetFragProcess scmfp : this.processes) {
			executer.execute(scmfp);
		}
		executer.shutdown();
	    while(!executer.isTerminated())
		{
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				LOGGER.error(e.getMessage());
				Thread.currentThread().interrupt();
			}
		}
		
	    if(threadStoppedExternally) {
	    	return;
	    }
	    /*
	     * retrieve the result
	     */
	    ScoredCandidateList scoredCandidateList = new ScoredCandidateList();
	    if(this.processes == null) return;
	    int numberCandidatesProcessed = 0;
		for(CombinedSingleCandidateMetFragProcess scmfp : this.processes) {
			/*
			 * check whether the single run was successful
			 */
			if(scmfp.wasSuccessful()) {
				// MetFrag 2.6: the final scores are calculated and assigned here, after all candidates were processed
				try {
					scmfp.singlePostCalculateScores();
					scmfp.assignScores();
				} catch (Exception e) {
					// MetFrag itself stops the whole evaluation here; only this candidate is left out
					LOGGER.error("Error when scoring candidate {}", scmfp.getScoredPrecursorCandidate().getIdentifier(), e);
					scmfp.getFragmenterAssignerScorer().nullifyScoresCollection();
					continue;
				}
				numberCandidatesProcessed++;
				ICandidate[] candidates = scmfp.getScoredPrecursorCandidates();
				for (ICandidate candidate : candidates) scoredCandidateList.addElement(candidate);
				//important to eliminate static variables
				scmfp.getFragmenterAssignerScorer().nullifyScoresCollection();
			}
		}
		/*
		 * normalise scores of the candidate list 
		 */
		try {
			this.sortedScoredCandidateList = scoredCandidateList.normaliseScores(
				(Double[])this.globalSettings.get(VariableNames.METFRAG_SCORE_WEIGHTS_NAME), 
				(String[])this.globalSettings.get(VariableNames.METFRAG_SCORE_TYPES_NAME),
				(String[])this.globalSettings.get(VariableNames.SCORE_NAMES_NOT_TO_SCALE)
			);
		} catch (ScorePropertyNotDefinedException e) {
			LOGGER.error(e.getMessage());
		}
		
		/*
		 * filter candidates by post processing filter
		 */
		if(this.sortedScoredCandidateList.getNumberElements() != this.numberCandidatesBeforeFilter)
			LOGGER.info("Processed {} candidate(s)", numberCandidatesProcessed);
		numberCandidatesBeforeFilter = numberCandidatesProcessed;
		this.sortedScoredCandidateList = this.postProcessingCandidateFilterCollection.filter(this.sortedScoredCandidateList);
		/*
		 * set number of peaks used for processing
		 */
		((ScoredCandidateList)this.sortedScoredCandidateList).setNumberPeaksUsed(((AbstractPeakList)this.globalSettings.get(VariableNames.PEAK_LIST_NAME)).getNumberPeaksUsed());
		
		LOGGER.info("{} candidate(s) were discarded before processing due to pre-filtering", processingStatus.getNumberPreFilteredCandidates().get());
		LOGGER.info("{} candidate(s) discarded during processing due to errors", processingStatus.getNumberErrorCandidates().get());
		LOGGER.info("{} candidate(s) discarded after processing due to post-filtering", postProcessingCandidateFilterCollection.getNumberPostFilteredCandidates());
		LOGGER.info("Stored {} candidate(s)", this.sortedScoredCandidateList.getNumberElements());
		
		this.processingStatus.setProcessStatusString("Processing Candidates");
		
		this.processes = null;
	}
	
	CandidateList getCandidateList() {
		return this.sortedScoredCandidateList;
	}

	/**
	 * init database, peaklist reader, bond energies 
	 */
	private void initialise() {
		/*
		 * set processing status object
		 * stores and returns status of metfrag processing
		 */
		this.processingStatus = new ProcessingStatus(this.globalSettings);
		this.globalSettings.set(VariableNames.PROCESS_STATUS_OBJECT_NAME, this.processingStatus);
		if(LOGGER.isTraceEnabled())
			LOGGER.trace(this.getClass().getName());
		try {
			if(LOGGER.isTraceEnabled())
				LOGGER.trace("\tinitialising database {}", VariableNames.METFRAG_DATABASE_TYPE_NAME);
			//initialise database
			this.database = new InMemoryDatabase(globalSettings);
				LOGGER.trace("\tinitialising peakListReader {}", VariableNames.METFRAG_PEAK_LIST_READER_NAME);
			//init bond energies
			BondEnergies bondEnergies;
			//from external file if given
			if(this.globalSettings.get(VariableNames.BOND_ENERGY_FILE_PATH_NAME) != null) 
				bondEnergies = new BondEnergies((String)this.globalSettings.get(VariableNames.BOND_ENERGY_FILE_PATH_NAME));
			else //or use defaults
				bondEnergies = new BondEnergies();
			this.globalSettings.set(VariableNames.BOND_ENERGY_OBJECT_NAME, bondEnergies);
		} catch (IllegalArgumentException | SecurityException e) {
			LOGGER.error(e.getMessage());
		}
	}
	
	/**
	 * init pre- and post-processing candidate filters
	 */
	private void initialiseCandidateFilters() {
		if(LOGGER.isTraceEnabled())
			LOGGER.trace(this.getClass().getName());
		/*
		 * retrieve candidate filter class names
		 */
		String[] preProcessingCandidateFilterNames = (String[])globalSettings.get(VariableNames.METFRAG_PRE_PROCESSING_CANDIDATE_FILTER_NAME);
		String[] postProcessingCandidateFilterNames = (String[])globalSettings.get(VariableNames.METFRAG_POST_PROCESSING_CANDIDATE_FILTER_NAME);
		/*
		 * initialise candidate filter arrays
		 */
		IPreProcessingCandidateFilter[] preProcessingCandidateFilter = new IPreProcessingCandidateFilter[preProcessingCandidateFilterNames.length];
		IPostProcessingCandidateFilter[] postProcessingCandidateFilter = new IPostProcessingCandidateFilter[postProcessingCandidateFilterNames.length];
		/*
		 * fill arrays with candidate filter objects
		 */
		try {
			if(LOGGER.isTraceEnabled())
				LOGGER.trace("\tinitialising preProcessingCandidateFilters");
			//init pre-processing filters
			for(int i = 0; i < preProcessingCandidateFilterNames.length; i++) {
				if(LOGGER.isTraceEnabled())
					LOGGER.trace("\t\tinitialising {}", preProcessingCandidateFilterNames[i]);
				preProcessingCandidateFilter[i] = (IPreProcessingCandidateFilter) Class.forName(ClassNames.getClassNameOfPreProcessingCandidateFilter(preProcessingCandidateFilterNames[i])).getConstructor(Settings.class).newInstance(this.globalSettings);
			}
			if(LOGGER.isTraceEnabled())
				LOGGER.trace("\tinitialising postProcessingCandidateFilters");
			//init post-processing filters
			for(int i = 0; i < postProcessingCandidateFilterNames.length; i++) {
				if(LOGGER.isTraceEnabled())
					LOGGER.trace("\t\tinitialising {}", postProcessingCandidateFilterNames[i]);
				postProcessingCandidateFilter[i] = (IPostProcessingCandidateFilter) Class.forName(ClassNames.getClassNameOfPostProcessingCandidateFilter(postProcessingCandidateFilterNames[i])).getConstructor(Settings.class).newInstance(this.globalSettings);
			}
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException |
				InvocationTargetException | SecurityException | ClassNotFoundException e) {
			LOGGER.error(e.getMessage());
		}
		
		/*
		 * define the filter collections
		 * stores all filters in one collection
		 */
		this.preProcessingCandidateFilterCollection = new PreProcessingCandidateFilterCollection(preProcessingCandidateFilter);
		this.postProcessingCandidateFilterCollection = new PostProcessingCandidateFilterCollection(postProcessingCandidateFilter);
	}
	
	private void initialiseScoresGlobal(MetFragGlobalSettings globalSettings) {
		String[] scoreTypes = (String[])globalSettings.get(VariableNames.METFRAG_SCORE_TYPES_NAME);
		for (String score_type : scoreTypes) {
			try {
				IScoreInitialiser scoreInitialiser = (IScoreInitialiser) Class.forName(
						ClassNames.getClassNameOfScoreInitialiser(score_type)).getConstructor().newInstance();
				scoreInitialiser.initScoreParameters(globalSettings);
			} catch (Exception e) {
				LOGGER.error(e.getMessage());
			}
		}
	}
	
	private void initExternalProperties(String path, CandidateList candidateList, String[] propertyValuesToScore) {
		Settings settings = new Settings();
		settings.set(VariableNames.LOCAL_DATABASE_PATH_NAME, path);
		LocalPropertyFileDatabase propertyDatabase = new LocalPropertyFileDatabase(settings);
		ArrayList<String> externalInChIKeys = new ArrayList<>();
		ArrayList<String> externalPropertiesDefined = new ArrayList<>();
		ArrayList<String> ids;
		try {
			ids = propertyDatabase.getCandidateIdentifiers();
		} catch(Exception e) {
			LOGGER.error("Error: Problems reading ExternalPropertyFile: {}", path);
			return;
		}
		CandidateList externalCandidates = propertyDatabase.getCandidateByIdentifier(ids);
		for(int i = 0; i < externalCandidates.getNumberElements(); i++) {
			ICandidate externalCandidate = externalCandidates.getElement(i);
			if(!externalCandidate.getProperties().containsKey(VariableNames.INCHI_KEY_1_NAME) || externalCandidate.getProperty(VariableNames.INCHI_KEY_1_NAME) == null) {
				LOGGER.error("Error: InChIKey1 field not defined for all candidate in ExternalPropertyFile: {}", path);
				return;
			}
			String externalInChIKey1 = (String)externalCandidate.getProperty(VariableNames.INCHI_KEY_1_NAME);
			if(externalInChIKeys.contains(externalInChIKey1)) {
				LOGGER.error("Error: InChIKey1 {} defined more than once in ExternalPropertyFile: {}", externalInChIKey1, path);
				return;
			}
			else externalInChIKeys.add(externalInChIKey1);
			//try to set external values
			for(int j = 0; j < candidateList.getNumberElements(); j++) {
				ICandidate candidate = candidateList.getElement(j);
				String candidateInChIKey1 =  (String)candidate.getProperty(VariableNames.INCHI_KEY_1_NAME);
				if(externalInChIKey1.equals(candidateInChIKey1)) {
					java.util.Enumeration<?> externalKeys = externalCandidate.getProperties().keys();
					while(externalKeys.hasMoreElements()) {
						String currentKey = (String)externalKeys.nextElement();
						for (String aPropertyValuesToScore : propertyValuesToScore) {
							if (aPropertyValuesToScore.equals(currentKey)) {
								if (!externalPropertiesDefined.contains(currentKey))
									externalPropertiesDefined.add(currentKey);
								Double value;
								try {
									value = (Double) externalCandidate.getProperty(currentKey);
								} catch (Exception e1) {
									try {
										value = Double.parseDouble((String) externalCandidate.getProperty(currentKey));
									} catch (Exception e2) {
										LOGGER.error("Error: Invalid value for candidate {} for column {} in ExternalPropertyFile: {}",
												externalInChIKey1, currentKey, path);
										return;
									}
								}
								candidate.setProperty(currentKey, value);
							}
						}
					}
				}
			}
		}
		//set property to zero (neutral element) in case not defined
		for(int i = 0; i < candidateList.getNumberElements(); i++) {
			ICandidate candidate = candidateList.getElement(i);
			for (String anExternalPropertiesDefined : externalPropertiesDefined) {
				if (!candidate.getProperties().containsKey(anExternalPropertiesDefined) || candidate.getProperty(anExternalPropertiesDefined) == null) {
					candidate.setProperty(anExternalPropertiesDefined, 0.0);
				}
			}
		}
	}
}
