package com.inspien.integration.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IdGeneratorTest {

    @Test
    void 기존_ID가_없으면_A001부터_시작한다() {
        assertThat(IdGenerator.next(null)).isEqualTo("A001");
    }

    @Test
    void 숫자를_하나씩_올린다() {
        assertThat(IdGenerator.next("A009")).isEqualTo("A010");
    }

    @Test
    void 숫자가_끝나면_다음_문자로_넘어간다() {
        assertThat(IdGenerator.next("A999")).isEqualTo("B001");
    }
}
