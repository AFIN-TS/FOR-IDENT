package de.hswt.fi.search.service.search.api.repositories;

import de.hswt.fi.search.service.mass.search.model.Entry;
import de.hswt.fi.search.service.mass.search.model.WebserviceEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EntryRepository extends PagingAndSortingRepository<Entry, Long>, CrudRepository<Entry, Long> {
	
	@Override
	Page<Entry> findAll();

	@Query("select e from Entry e where e.elementalFormula.value like %?1%")
	Page<WebserviceEntry> findByFormula(String elementalFormula, Pageable pageable);
	
	@Query("select e from Entry e where e.accurateMass.value between ?1 and ?2")
	Page<WebserviceEntry> findByAccurateMass(Double accurateMassMin, Double accurateMassMax, Pageable pageable);
	
	@Query("select e.inchiKey.value from Entry e")
	Page<String> findAllInchiKeys(Pageable pageable);
	
	@Query("select e.inchiKey.value, e.publicID from Entry e")
	Page<Object> findAllInchiKeysAndStoffidentIds(Pageable pageable);
	
	@Query("select e.publicID from Entry e")
	Page<String> findAllPublicIDs(Pageable pageable);
	
	@Query("select e from Entry e where e.publicID = ?1")
	Optional<WebserviceEntry> findByPublicID(String publicID);
	
	Optional<WebserviceEntry> findByInchiKeyValue(String inchiKey);
}
