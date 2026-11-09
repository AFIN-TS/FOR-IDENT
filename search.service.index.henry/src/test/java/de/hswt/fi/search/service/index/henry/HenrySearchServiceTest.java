package de.hswt.fi.search.service.index.henry;

import de.hswt.fi.search.service.index.model.IndexSearchResult;
import de.hswt.fi.search.service.mass.search.model.Entry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The result for candidates without RTI result (GC): until SP-16 it was built with a constructor that rejects a
 * missing entry, so the GC RTI screening failed with a NullPointerException.
 */
public class HenrySearchServiceTest {

	@Test
	public void dummyResultForCandidatesWithoutRtiResult() {
		IndexSearchResult result = new HenrySearchService().getDummyResult();
		assertFalse(result.isAvailable());
		assertTrue(Double.isNaN(result.getTargetMass()));
		assertTrue(Double.isNaN(result.getRetentionTimeIndex()));

		// As the processing does it (DefaultProcessingService.parseRtiResult)
		result.setTargetIdentifier("mz 189.1022");
		result.setEntry(Entry.EntryBuilder.newBuilder().withPublicID("DI00000001").build());
		assertEquals("mz 189.1022DI00000001", result.getID());
	}
}
