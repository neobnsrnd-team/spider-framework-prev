package com.example.spideradmin.domain.xmlproperty.service;

import com.example.spideradmin.domain.xmlproperty.dto.XmlPropertyEntryResponse;
import com.example.spideradmin.domain.xmlproperty.dto.XmlPropertyFileCreateRequest;
import com.example.spideradmin.domain.xmlproperty.dto.XmlPropertyFileDetailResponse;
import com.example.spideradmin.domain.xmlproperty.dto.XmlPropertyFileResponse;
import com.example.spideradmin.domain.xmlproperty.dto.XmlPropertySaveRequest;
import com.example.spideradmin.global.exception.DuplicateException;
import com.example.spideradmin.global.exception.InternalException;
import com.example.spideradmin.global.exception.InvalidInputException;
import com.example.spideradmin.global.exception.NotFoundException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * XML Property 관리 Service 구현체
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class XmlPropertyService {

    @Value("${xml-property.directory}")
    private String xmlPropertyDirectory;

    private static final String FILE_SUFFIX = ".properties.xml";
    private static final Pattern VALID_FILE_NAME = Pattern.compile("^[a-zA-Z0-9_\\-\\.]+\\.properties\\.xml$");
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public List<XmlPropertyFileResponse> listFiles() {
        Path dir = ensureDirectoryExists();

        try (Stream<Path> paths = Files.walk(dir, 1)) {
            return paths.filter(p -> !p.equals(dir))
                    .filter(p -> p.getFileName().toString().endsWith(FILE_SUFFIX))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .map(this::toFileResponseDTO)
                    .toList();
        } catch (IOException e) {
            throw new InternalException("파일 목록 조회 실패", e);
        }
    }

    public XmlPropertyFileDetailResponse getFileDetail(String fileName) {
        Path filePath = resolveAndValidatePath(fileName);
        if (!Files.exists(filePath)) {
            throw new NotFoundException("fileName: " + fileName);
        }

        List<XmlPropertyEntryResponse> entries = parseEntries(filePath);
        String lastModified = formatLastModified(filePath);

        return XmlPropertyFileDetailResponse.builder()
                .fileName(fileName)
                .description(parseDescription(filePath))
                .lastModified(lastModified)
                .entries(entries)
                .build();
    }

    @Transactional
    public XmlPropertyFileResponse createFile(XmlPropertyFileCreateRequest request) {
        String rawName = request.getFileName().trim();
        String fileName = rawName.endsWith(FILE_SUFFIX) ? rawName : rawName + FILE_SUFFIX;

        if (!VALID_FILE_NAME.matcher(fileName).matches()) {
            throw new InvalidInputException(fileName);
        }

        ensureDirectoryExists();
        Path filePath = resolveAndValidatePath(fileName);

        if (Files.exists(filePath)) {
            throw new DuplicateException("fileName: " + fileName);
        }

        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            writeEntries(filePath, Collections.emptyList(), request.getDescription());
        } else {
            writeEmptyXml(filePath);
        }
        log.info("XML Property 파일 생성: {}", filePath);

        return toFileResponseDTO(filePath);
    }

    @Transactional
    public XmlPropertyFileDetailResponse saveEntries(String fileName, XmlPropertySaveRequest request) {
        Path filePath = resolveAndValidatePath(fileName);
        if (!Files.exists(filePath)) {
            throw new NotFoundException("fileName: " + fileName);
        }

        List<XmlPropertySaveRequest.EntryDTO> entries = request.getEntries();

        // key 중복 검증
        Set<String> keys = new HashSet<>();
        for (XmlPropertySaveRequest.EntryDTO entry : entries) {
            if (!keys.add(entry.getKey())) {
                throw new InvalidInputException("key: " + entry.getKey());
            }
        }

        writeEntries(filePath, entries, request.getDescription());
        log.info("XML Property 항목 저장: fileName={}, entryCount={}", fileName, entries.size());

        return getFileDetail(fileName);
    }

    @Transactional
    public void deleteFile(String fileName) {
        Path filePath = resolveAndValidatePath(fileName);
        if (!Files.exists(filePath)) {
            throw new NotFoundException("fileName: " + fileName);
        }
        try {
            Files.delete(filePath);
            log.info("XML Property 파일 삭제: {}", filePath);
        } catch (IOException e) {
            throw new InternalException("파일 삭제 실패: " + fileName, e);
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private Path resolveAndValidatePath(String fileName) {
        Path dir = Paths.get(xmlPropertyDirectory);
        Path resolved = dir.resolve(fileName).normalize();
        if (!resolved.startsWith(dir)) {
            throw new InvalidInputException(fileName);
        }
        return resolved;
    }

    private Path ensureDirectoryExists() {
        Path dir = Paths.get(xmlPropertyDirectory);
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                throw new InternalException("디렉토리 생성 실패", e);
            }
        }
        return dir;
    }

    private XmlPropertyFileResponse toFileResponseDTO(Path filePath) {
        List<XmlPropertyEntryResponse> entries = parseEntries(filePath);
        return XmlPropertyFileResponse.builder()
                .fileName(filePath.getFileName().toString())
                .description(parseDescription(filePath))
                .entryCount(entries.size())
                .lastModified(formatLastModified(filePath))
                .build();
    }

    private String formatLastModified(Path filePath) {
        try {
            Instant instant = Files.getLastModifiedTime(filePath).toInstant();
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(DISPLAY_FORMATTER);
        } catch (IOException e) {
            return "";
        }
    }

    private List<XmlPropertyEntryResponse> parseEntries(Path filePath) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(filePath.toFile());
            doc.getDocumentElement().normalize();

            NodeList nodeList = doc.getElementsByTagName("entry");
            List<XmlPropertyEntryResponse> entries = new ArrayList<>();
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element el = (Element) nodeList.item(i);
                entries.add(XmlPropertyEntryResponse.builder()
                        .key(el.getAttribute("key"))
                        .value(el.getTextContent())
                        .description(el.getAttribute("description"))
                        .build());
            }
            return entries;
        } catch (Exception e) {
            throw new InternalException("XML 파싱 실패: " + filePath.getFileName(), e);
        }
    }

    private void writeEmptyXml(Path filePath) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            Element root = doc.createElement("properties");
            doc.appendChild(root);
            writeDocument(doc, filePath.toFile());
        } catch (Exception e) {
            throw new InternalException("빈 XML 파일 생성 실패", e);
        }
    }

    private void writeEntries(Path filePath, List<XmlPropertySaveRequest.EntryDTO> entries, String description) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            Element root = doc.createElement("properties");
            if (description != null && !description.isBlank()) {
                root.setAttribute("description", description);
            }
            doc.appendChild(root);

            for (XmlPropertySaveRequest.EntryDTO entry : entries) {
                Element el = doc.createElement("entry");
                el.setAttribute("key", entry.getKey());
                if (entry.getDescription() != null && !entry.getDescription().isBlank()) {
                    el.setAttribute("description", entry.getDescription());
                }
                el.setTextContent(entry.getValue() != null ? entry.getValue() : "");
                root.appendChild(el);
            }

            writeDocument(doc, filePath.toFile());
        } catch (Exception e) {
            throw new InternalException("XML 항목 저장 실패", e);
        }
    }

    private String parseDescription(Path filePath) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(filePath.toFile());
            return doc.getDocumentElement().getAttribute("description");
        } catch (Exception e) {
            return "";
        }
    }

    private void writeDocument(Document doc, File file) throws TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        tf.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD, "");
        tf.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(new DOMSource(doc), new StreamResult(file));
    }
}
