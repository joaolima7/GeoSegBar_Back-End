package com.geosegbar.adapters.validations;

public interface ExistsByNameValidation {
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
}
