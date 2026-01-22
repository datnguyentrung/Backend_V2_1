package com.dat.backend_v2_1.util.converter;

public class NameConverter {
    public static String formatVietnameseName(String input) {
        if (input == null || input.isEmpty()) return "";

        // BƯỚC 1: Xóa ký tự đặc biệt (Chỉ giữ lại Chữ cái Unicode + Khoảng trắng)
        // [^\\p{L}\\s] nghĩa là: Cái gì KHÔNG PHẢI là (Chữ cái bất kỳ ngôn ngữ nào HOẶC Dấu cách) thì xóa đi
        String cleaned = input.replaceAll("[^\\p{L}\\s]", "");

        // BƯỚC 2: Chuẩn hóa viết hoa chữ cái đầu (Title Case)
        // Tách chuỗi thành mảng các từ dựa vào dấu cách
        String[] words = cleaned.trim().split("\\s+");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                // Chữ cái đầu viết hoa + Các chữ sau viết thường
                String capitalizedWord = Character.toUpperCase(word.charAt(0))
                        + word.substring(1).toLowerCase();
                result.append(capitalizedWord).append(" ");
            }
        }

        return result.toString().trim(); // Xóa dấu cách thừa ở cuối
    }
}
