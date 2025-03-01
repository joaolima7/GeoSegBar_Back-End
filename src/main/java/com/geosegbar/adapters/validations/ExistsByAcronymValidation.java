package com.geosegbar.adapters.validations;

public interface ExistsByAcronymValidation {
    boolean existsByAcronym(String acronym);
    boolean existsByAcronymAndIdNot(String acronym, Long id);
}
