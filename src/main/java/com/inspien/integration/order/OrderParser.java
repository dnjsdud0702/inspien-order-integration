package com.inspien.integration.order;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

// XML -> Order 객체로 변환
@Component
public class OrderParser {

    public record Result(List<Order> orders, List<String> errors) {
    }

    public Result parse(String xml) throws Exception {
        Document document = read(xml);

        Map<String, Element> headers = new HashMap<>();
        NodeList headerNodes = document.getElementsByTagName("HEADER");
        for (int i = 0; i < headerNodes.getLength(); i++) {
            Element header = (Element) headerNodes.item(i);
            headers.put(text(header, "USER_ID"), header);
        }

        List<Order> orders = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        NodeList itemNodes = document.getElementsByTagName("ITEM");
        for (int i = 0; i < itemNodes.getLength(); i++) {
            Element item = (Element) itemNodes.item(i);
            String userId = text(item, "USER_ID");
            String itemId = text(item, "ITEM_ID");
            Element header = headers.get(userId);
            if (header == null) {
                errors.add(userId + "/" + itemId + ": HEADER 없음");
                continue;
            }

            Order order = new Order(null, userId, itemId, text(header, "NAME"), text(header, "ADDRESS"),
                    text(item, "ITEM_NAME"), text(item, "PRICE"));
            if (hasBlank(order)) {
                errors.add(userId + "/" + itemId + ": 필수값 누락");
                continue;
            }
            orders.add(order);
        }
        return new Result(orders, errors);
    }

    // XML 파싱 -> Document 객체 변환
    private Document read(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); // XXE 차단
        String body = xml.strip().replaceFirst("^<\\?xml[^>]*\\?>", "");
        try {
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader("<ROOT>" + body + "</ROOT>")));
        } catch (SAXException e) {
            throw new IllegalArgumentException("XML 형식이 올바르지 않습니다", e);
        }
    }

    // 필수값이 하나라도 비어있으면 true
    private boolean hasBlank(Order order) {
        return Stream.of(order.userId(), order.itemId(), order.name(), order.address(), order.itemName(),
                order.price()).anyMatch(String::isEmpty);
    }

    // 태그 텍스트 반환, 태그 없으면 빈 문자열
    private String text(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().strip();
    }
}
