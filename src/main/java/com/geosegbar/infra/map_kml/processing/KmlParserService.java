package com.geosegbar.infra.map_kml.processing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class KmlParserService {

    // ── Inner models ──────────────────────────────────────────────────────────

    private static class KmlStyleData {
        String iconHref;
        String iconColor;
        String lineColor;
        String polyFillColor;
    }

    private static class GeometryResult {
        String type;
        String json;
        double minLat, maxLat, minLng, maxLng;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public List<KmlRawFeature> parse(byte[] bytes, boolean isKmz) {
        try {
            byte[] kmlBytes = isKmz ? extractKmlBytes(bytes, true) : bytes;
            Document doc = buildDocument(kmlBytes);

            Map<String, KmlStyleData> styles = extractStyles(doc);
            Map<String, String> styleMaps = extractStyleMaps(doc);

            List<KmlRawFeature> features = new ArrayList<>();
            int[] counter = {0};
            traverseForPlacemarks(doc.getDocumentElement(), new ArrayList<>(), styles, styleMaps, features, counter);
            return features;
        } catch (Exception e) {
            log.error("Erro ao parsear KML: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao processar KML: " + e.getMessage(), e);
        }
    }

    public byte[] extractKmlBytes(byte[] bytes, boolean isKmz) throws IOException {
        if (!isKmz) return bytes;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".kml")) {
                    return zis.readAllBytes();
                }
                zis.closeEntry();
            }
        }
        throw new RuntimeException("Nenhum arquivo .kml encontrado no .kmz");
    }

    public Document buildDocument(byte[] kmlBytes) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(null);
        return builder.parse(new ByteArrayInputStream(kmlBytes));
    }

    public List<Element> collectPlacemarks(Document doc) {
        List<Element> result = new ArrayList<>();
        collectPlacemarksRecursive(doc.getDocumentElement(), result);
        return result;
    }

    public void applyPlacemarkEdit(Element placemark, String customName, String customColor,
            String featureType, Document doc) {
        if (customName != null) {
            Element nameEl = firstChild(placemark, "name");
            if (nameEl != null) {
                nameEl.setTextContent(customName);
            } else {
                Element newName = doc.createElement("name");
                newName.setTextContent(customName);
                placemark.insertBefore(newName, placemark.getFirstChild());
            }
        }

        if (customColor != null) {
            Element styleEl = firstChild(placemark, "Style");
            if (styleEl == null) {
                styleEl = doc.createElement("Style");
                placemark.appendChild(styleEl);
            }
            String kmlColor = hexToKmlColor(customColor);
            if ("POINT".equals(featureType)) {
                Element iconStyle = getOrCreateChild(doc, styleEl, "IconStyle");
                getOrCreateChild(doc, iconStyle, "color").setTextContent(kmlColor);
            } else if ("LINE".equals(featureType)) {
                Element lineStyle = getOrCreateChild(doc, styleEl, "LineStyle");
                getOrCreateChild(doc, lineStyle, "color").setTextContent(kmlColor);
            } else {
                Element polyStyle = getOrCreateChild(doc, styleEl, "PolyStyle");
                getOrCreateChild(doc, polyStyle, "color").setTextContent(kmlColor);
            }
        }
    }

    public byte[] serializeDocument(Document doc) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(baos));
        return baos.toByteArray();
    }

    public byte[] repackKmz(byte[] originalKmz, byte[] newKmlBytes) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(originalKmz));
             ZipOutputStream zos = new ZipOutputStream(baos)) {
            boolean replaced = false;
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (!replaced && !entry.isDirectory() && name.toLowerCase().endsWith(".kml")) {
                    zos.putNextEntry(new ZipEntry(name));
                    zos.write(newKmlBytes);
                    zos.closeEntry();
                    replaced = true;
                } else {
                    zos.putNextEntry(new ZipEntry(name));
                    zis.transferTo(zos);
                    zos.closeEntry();
                }
                zis.closeEntry();
            }
        }
        return baos.toByteArray();
    }

    // ── Parsing internals ─────────────────────────────────────────────────────

    private void traverseForPlacemarks(Element el, List<String> folderPath,
            Map<String, KmlStyleData> styles, Map<String, String> styleMaps,
            List<KmlRawFeature> features, int[] counter) {
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (!(child instanceof Element childEl)) continue;
            switch (localName(childEl)) {
                case "Document" -> traverseForPlacemarks(childEl, folderPath, styles, styleMaps, features, counter);
                case "Folder" -> {
                    String folderName = getChildText(childEl, "name");
                    List<String> newPath = new ArrayList<>(folderPath);
                    if (folderName != null && !folderName.isBlank()) newPath.add(folderName);
                    traverseForPlacemarks(childEl, newPath, styles, styleMaps, features, counter);
                }
                case "Placemark" -> {
                    KmlRawFeature f = parsePlacemark(childEl, folderPath, styles, styleMaps, counter[0]++);
                    if (f != null) features.add(f);
                }
                default -> { /* ignore other elements */ }
            }
        }
    }

    private void collectPlacemarksRecursive(Element el, List<Element> result) {
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (!(child instanceof Element childEl)) continue;
            switch (localName(childEl)) {
                case "Document", "Folder" -> collectPlacemarksRecursive(childEl, result);
                case "Placemark" -> result.add(childEl);
                default -> { /* skip */ }
            }
        }
    }

    private KmlRawFeature parsePlacemark(Element el, List<String> folderPath,
            Map<String, KmlStyleData> styles, Map<String, String> styleMaps, int idx) {
        GeometryResult geo = parseGeometry(el);
        if (geo == null) return null;

        KmlRawFeature f = new KmlRawFeature();
        f.setFeatureIndex(idx);
        String extId = el.getAttribute("id");
        f.setExternalId(extId.isEmpty() ? null : extId);
        String name = getChildText(el, "name");
        f.setName(name != null && !name.isBlank() ? name : "Feature " + idx);
        f.setType(geo.type);
        f.setFolderPath(new ArrayList<>(folderPath));
        f.setGeometryJson(geo.json);
        f.setMinLat(geo.minLat);
        f.setMinLng(geo.minLng);
        f.setMaxLat(geo.maxLat);
        f.setMaxLng(geo.maxLng);

        KmlStyleData style = resolveStyle(el, styles, styleMaps);
        if (style != null) {
            if ("POINT".equals(geo.type)) {
                f.setStyleColor(style.iconColor);
                f.setIconHref(style.iconHref);
            } else if ("LINE".equals(geo.type)) {
                f.setStyleColor(style.lineColor);
                f.setStrokeColor(style.lineColor);
            } else {
                f.setStyleColor(style.lineColor);
                f.setStrokeColor(style.lineColor);
                f.setFillColor(style.polyFillColor);
            }
        }
        return f;
    }

    private KmlStyleData resolveStyle(Element placemark,
            Map<String, KmlStyleData> styles, Map<String, String> styleMaps) {
        Element inlineStyle = firstChild(placemark, "Style");
        if (inlineStyle != null) return parseStyleElement(inlineStyle);

        String styleUrl = getChildText(placemark, "styleUrl");
        if (styleUrl == null) return null;
        String id = styleUrl.startsWith("#") ? styleUrl.substring(1) : styleUrl;
        if (styleMaps.containsKey(id)) id = styleMaps.get(id);
        return styles.get(id);
    }

    private Map<String, KmlStyleData> extractStyles(Document doc) {
        Map<String, KmlStyleData> map = new HashMap<>();
        NodeList nodes = getByLocalName(doc, "Style");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            String id = el.getAttribute("id");
            if (!id.isEmpty()) map.put(id, parseStyleElement(el));
        }
        return map;
    }

    private KmlStyleData parseStyleElement(Element styleEl) {
        KmlStyleData s = new KmlStyleData();
        Element iconStyle = firstDescendant(styleEl, "IconStyle");
        if (iconStyle != null) {
            s.iconColor = colorFromChild(iconStyle, "color");
            Element icon = firstChild(iconStyle, "Icon");
            if (icon != null) s.iconHref = getChildText(icon, "href");
        }
        Element lineStyle = firstDescendant(styleEl, "LineStyle");
        if (lineStyle != null) s.lineColor = colorFromChild(lineStyle, "color");
        Element polyStyle = firstDescendant(styleEl, "PolyStyle");
        if (polyStyle != null) s.polyFillColor = colorFromChild(polyStyle, "color");
        return s;
    }

    private Map<String, String> extractStyleMaps(Document doc) {
        Map<String, String> map = new HashMap<>();
        NodeList nodes = getByLocalName(doc, "StyleMap");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            String id = el.getAttribute("id");
            if (id.isEmpty()) continue;
            NodeList pairs = getByLocalNameIn(el, "Pair");
            for (int j = 0; j < pairs.getLength(); j++) {
                Element pair = (Element) pairs.item(j);
                if ("normal".equals(getChildText(pair, "key"))) {
                    String url = getChildText(pair, "styleUrl");
                    if (url != null && url.startsWith("#")) map.put(id, url.substring(1));
                    break;
                }
            }
        }
        return map;
    }

    // ── Geometry parsing ──────────────────────────────────────────────────────

    private GeometryResult parseGeometry(Element placemark) {
        Element point = firstDescendant(placemark, "Point");
        if (point != null) {
            Element coords = firstDescendant(point, "coordinates");
            if (coords != null) return buildPoint(coords.getTextContent().trim());
        }
        Element line = firstDescendant(placemark, "LineString");
        if (line != null) {
            Element coords = firstDescendant(line, "coordinates");
            if (coords != null) return buildLine(coords.getTextContent().trim());
        }
        Element poly = firstDescendant(placemark, "Polygon");
        if (poly != null) {
            Element coords = firstDescendant(poly, "coordinates");
            if (coords != null) return buildPolygon(coords.getTextContent().trim());
        }
        Element multi = firstDescendant(placemark, "MultiGeometry");
        if (multi != null) return parseGeometry(multi);
        return null;
    }

    private GeometryResult buildPoint(String coordText) {
        List<double[]> coords = parseCoordinates(coordText);
        if (coords.isEmpty()) return null;
        double lng = coords.get(0)[0], lat = coords.get(0)[1];
        GeometryResult r = new GeometryResult();
        r.type = "POINT";
        r.json = "{\"type\":\"Point\",\"coordinates\":[" + d(lng) + "," + d(lat) + "]}";
        r.minLat = r.maxLat = lat;
        r.minLng = r.maxLng = lng;
        return r;
    }

    private GeometryResult buildLine(String coordText) {
        List<double[]> coords = parseCoordinates(coordText);
        if (coords.size() < 2) return null;
        GeometryResult r = new GeometryResult();
        r.type = "LINE";
        r.json = "{\"type\":\"LineString\",\"coordinates\":" + coordsJson(coords) + "}";
        setBounds(r, coords);
        return r;
    }

    private GeometryResult buildPolygon(String coordText) {
        List<double[]> coords = parseCoordinates(coordText);
        if (coords.size() < 3) return null;
        GeometryResult r = new GeometryResult();
        r.type = "POLYGON";
        r.json = "{\"type\":\"Polygon\",\"coordinates\":[" + coordsJson(coords) + "]}";
        setBounds(r, coords);
        return r;
    }

    private List<double[]> parseCoordinates(String text) {
        List<double[]> result = new ArrayList<>();
        for (String token : text.trim().split("\\s+")) {
            if (token.isEmpty()) continue;
            String[] parts = token.split(",");
            if (parts.length >= 2) {
                try {
                    double lng = Double.parseDouble(parts[0].trim());
                    double lat = Double.parseDouble(parts[1].trim());
                    result.add(new double[]{lng, lat});
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return result;
    }

    private String coordsJson(List<double[]> coords) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < coords.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("[").append(d(coords.get(i)[0])).append(",").append(d(coords.get(i)[1])).append("]");
        }
        return sb.append("]").toString();
    }

    private void setBounds(GeometryResult r, List<double[]> coords) {
        r.minLng = coords.stream().mapToDouble(c -> c[0]).min().orElse(0);
        r.maxLng = coords.stream().mapToDouble(c -> c[0]).max().orElse(0);
        r.minLat = coords.stream().mapToDouble(c -> c[1]).min().orElse(0);
        r.maxLat = coords.stream().mapToDouble(c -> c[1]).max().orElse(0);
    }

    private String d(double v) {
        return Double.toString(v);
    }

    // ── Color helpers ─────────────────────────────────────────────────────────

    private String colorFromChild(Element parent, String childName) {
        String text = getChildText(parent, childName);
        return kmlColorToHex(text);
    }

    private String kmlColorToHex(String kmlColor) {
        if (kmlColor == null) return null;
        String c = kmlColor.trim();
        if (c.length() == 8) {
            return "#" + c.substring(6, 8) + c.substring(4, 6) + c.substring(2, 4);
        }
        return null;
    }

    private String hexToKmlColor(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        if (h.length() == 6) {
            return "ff" + h.substring(4, 6) + h.substring(2, 4) + h.substring(0, 2);
        }
        return "ff000000";
    }

    // ── DOM helpers ───────────────────────────────────────────────────────────

    private String localName(Element el) {
        String ln = el.getLocalName();
        if (ln != null) return ln;
        String tag = el.getTagName();
        int colon = tag.indexOf(':');
        return colon >= 0 ? tag.substring(colon + 1) : tag;
    }

    private Element firstChild(Element parent, String lnTarget) {
        if (parent == null) return null;
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n instanceof Element el && lnTarget.equals(localName(el))) return el;
        }
        return null;
    }

    private Element firstDescendant(Element parent, String lnTarget) {
        if (parent == null) return null;
        NodeList nl = parent.getElementsByTagNameNS("*", lnTarget);
        if (nl.getLength() > 0) return (Element) nl.item(0);
        nl = parent.getElementsByTagName(lnTarget);
        return nl.getLength() > 0 ? (Element) nl.item(0) : null;
    }

    private String getChildText(Element parent, String childLn) {
        if (parent == null) return null;
        Element child = firstChild(parent, childLn);
        return child != null ? child.getTextContent().trim() : null;
    }

    private NodeList getByLocalName(Document doc, String ln) {
        NodeList nl = doc.getElementsByTagNameNS("*", ln);
        if (nl.getLength() == 0) nl = doc.getElementsByTagName(ln);
        return nl;
    }

    private NodeList getByLocalNameIn(Element parent, String ln) {
        NodeList nl = parent.getElementsByTagNameNS("*", ln);
        if (nl.getLength() == 0) nl = parent.getElementsByTagName(ln);
        return nl;
    }

    private Element getOrCreateChild(Document doc, Element parent, String lnTarget) {
        Element existing = firstChild(parent, lnTarget);
        if (existing != null) return existing;
        Element created = doc.createElement(lnTarget);
        parent.appendChild(created);
        return created;
    }
}
