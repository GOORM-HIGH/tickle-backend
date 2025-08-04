package com.profect.tickle.domain.performance.parser;

import com.profect.tickle.domain.performance.dto.KopisPerformanceDto;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class KopisXmlParser {
    public List<KopisPerformanceDto> parse(String xml) throws Exception {
        List<KopisPerformanceDto> list = new ArrayList<>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        NodeList nodeList = doc.getElementsByTagName("db");

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element el = (Element) nodeList.item(i);
            KopisPerformanceDto dto = new KopisPerformanceDto();
            dto.setMt20id(getTagValue("mt20id", el));
            dto.setPrfnm(getTagValue("prfnm", el));
            dto.setPoster(getTagValue("poster", el));
            dto.setFcltynm(getTagValue("fcltynm", el));
            dto.setGenrenm(getTagValue("genrenm", el));
            dto.setPrfpdfrom(getTagValue("prfpdfrom", el));
            dto.setPrfpdto(getTagValue("prfpdto", el));
            list.add(dto);
        }

        return list;
    }

    private String getTagValue(String tag, Element element) {
        NodeList nList = element.getElementsByTagName(tag);
        if (nList.getLength() == 0) return null;
        return nList.item(0).getTextContent();
    }
}