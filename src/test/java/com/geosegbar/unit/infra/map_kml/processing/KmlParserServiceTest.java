package com.geosegbar.unit.infra.map_kml.processing;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.geosegbar.infra.map_kml.processing.KmlParserService;
import com.geosegbar.infra.map_kml.processing.KmlRawFeature;

@Tag("unit")
@DisplayName("KmlParserService - Unit Tests")
class KmlParserServiceTest {

    private final KmlParserService parser = new KmlParserService();

    @Test
    @DisplayName("Should parse KML with xsi schemaLocation even when xsi namespace is missing")
    void shouldParseKmlWithMissingXsiNamespaceDeclaration() {
        String kml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <kml xmlns="http://www.opengis.net/kml/2.2">
                  <Document xsi:schemaLocation="http://www.opengis.net/kml/2.2">
                    <Folder>
                      <name>Inventario</name>
                      <Placemark id="sirene-01">
                        <name>Sirene 01</name>
                        <Point>
                          <coordinates>-63.9005,-8.8012,0</coordinates>
                        </Point>
                      </Placemark>
                    </Folder>
                  </Document>
                </kml>
                """;

        List<KmlRawFeature> features = parser.parse(kml.getBytes(StandardCharsets.UTF_8), false);

        assertThat(features).hasSize(1);
        KmlRawFeature feature = features.getFirst();
        assertThat(feature.getName()).isEqualTo("Sirene 01");
        assertThat(feature.getExternalId()).isEqualTo("sirene-01");
        assertThat(feature.getType()).isEqualTo("POINT");
        assertThat(feature.getFolderPath()).containsExactly("Inventario");
        assertThat(feature.getGeometryJson())
                .isEqualTo("{\"type\":\"Point\",\"coordinates\":[-63.9005,-8.8012]}");
    }
}
