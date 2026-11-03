package com.inspien.integration.common;

// ID 규칙: 대문자 1자 + 숫자 3자리 (A001 ... A999 -> B001)
// 시퀀스 생성 권한 없음 -> 현재 최댓값 + 1
public final class IdGenerator {

    private IdGenerator() {
    }

    public static String next(String current) {
        if (current == null) {
            return "A001";
        }
        char letter = current.charAt(0);
        int number = Integer.parseInt(current.substring(1));
        return number < 999 ? letter + String.format("%03d", number + 1) : (char) (letter + 1) + "001";
    }
}
