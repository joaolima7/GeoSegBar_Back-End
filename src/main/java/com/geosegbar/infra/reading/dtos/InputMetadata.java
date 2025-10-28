package com.geosegbar.infra.reading.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InputMetadata {

    private String acronym;
    private String name;
    private String unit;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InputMetadata that = (InputMetadata) o;
        return acronym.equals(that.acronym);
    }

    @Override
    public int hashCode() {
        return acronym.hashCode();
    }
}
