package com.inspien.integration.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class OrderParserTest {

    private final OrderParser parser = new OrderParser();

    @Test
    void ITEM은_순서가_아니라_USER_ID로_HEADER와_연결한다() throws Exception {
        OrderParser.Result result = parser.parse(header("U1", "김철수") + header("U2", "이영희") + item("U1", "I1", "1000"));

        assertThat(result.orders()).extracting(Order::name).containsExactly("김철수");
    }

    @Test
    void HEADER가_없는_ITEM은_제외하고_사유를_남긴다() throws Exception {
        OrderParser.Result result = parser.parse(header("U1", "김철수") + item("U9", "I1", "1000"));

        assertThat(result.orders()).isEmpty();
        assertThat(result.errors()).containsExactly("U9/I1: HEADER 없음");
    }

    @Test
    void 필수값이_없으면_제외한다() throws Exception {
        OrderParser.Result result = parser.parse(header("U1", "김철수") + item("U1", "I1", ""));

        assertThat(result.errors()).containsExactly("U1/I1: 필수값 누락");
    }

    @Test
    void 깨진_XML은_거부한다() {
        assertThatThrownBy(() -> parser.parse("<HEADER>")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 제공된_샘플은_63건을_저장하고_7건을_제외한다() throws Exception {
        String xml = Files.readString(Path.of("samples/orders.xml"), Charset.forName("EUC-KR"));

        OrderParser.Result result = parser.parse(xml);

        assertThat(result.orders()).hasSize(63);
        assertThat(result.errors()).hasSize(7);
    }

    private static String header(String userId, String name) {
        return "<HEADER><USER_ID>%s</USER_ID><NAME>%s</NAME><ADDRESS>강남구</ADDRESS></HEADER>".formatted(userId, name);
    }

    private static String item(String userId, String itemId, String price) {
        return "<ITEM><USER_ID>%s</USER_ID><ITEM_ID>%s</ITEM_ID><ITEM_NAME>모자</ITEM_NAME><PRICE>%s</PRICE></ITEM>"
                .formatted(userId, itemId, price);
    }
}
