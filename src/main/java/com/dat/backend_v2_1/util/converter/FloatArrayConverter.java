package com.dat.backend_v2_1.util.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class FloatArrayConverter implements AttributeConverter<float[], String> {

    @Override
    public String convertToDatabaseColumn(float[] attribute) {
        if (attribute == null) return null;
        // Lưu dưới dạng string "[0.1,0.2,...]" tương thích với pgvector
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < attribute.length; i++) {
            sb.append(attribute[i]);
            if (i < attribute.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public float[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;
        // Parse "[0.1,0.2,...]" hoặc "{0.1,0.2,...}" (PostgreSQL array format)
        // Xóa ký tự '[', '{' ở đầu và ']', '}' ở cuối (PostgreSQL vector/array formats)
        String trimmed = dbData.trim();
        if (!trimmed.isEmpty() && (trimmed.charAt(0) == '[' || trimmed.charAt(0) == '{')) {
            trimmed = trimmed.substring(1);
        }
        if (!trimmed.isEmpty()) {
            char last = trimmed.charAt(trimmed.length() - 1);
            if (last == ']' || last == '}') {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
        }
        String cleaned = trimmed;
        if (cleaned.isBlank()) return new float[0];
        String[] parts = cleaned.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }
}

