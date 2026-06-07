package com.robotlive.smartcodeless.mcp;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.robotlive.smartcodeless.exception.BusinessException;
import com.robotlive.smartcodeless.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class McpSchemaValidator {

    @SuppressWarnings("unchecked")
    public void validate(Map<String, Object> schema, Map<String, Object> arguments) {
        if (schema == null) {
            return;
        }
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        List<String> required = (List<String>) schema.get("required");
        if (CollUtil.isNotEmpty(required)) {
            for (String field : required) {
                if (arguments == null || !arguments.containsKey(field) || arguments.get(field) == null) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "missing required field: " + field);
                }
            }
        }
        if (properties == null || arguments == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            Object property = properties.get(entry.getKey());
            if (!(property instanceof Map<?, ?> propertySchema)) {
                continue;
            }
            String type = String.valueOf(propertySchema.get("type"));
            Object value = entry.getValue();
            if (!matchesType(type, value)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "invalid field type: " + entry.getKey());
            }
            Object maxLength = propertySchema.get("maxLength");
            if (value instanceof String text && maxLength instanceof Number number && text.length() > number.intValue()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "field too long: " + entry.getKey());
            }
            Object minLength = propertySchema.get("minLength");
            if (value instanceof String text && minLength instanceof Number number && text.length() < number.intValue()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "field too short: " + entry.getKey());
            }
        }
    }

    private boolean matchesType(String type, Object value) {
        if (value == null || StrUtil.isBlank(type)) {
            return true;
        }
        return switch (type) {
            case "string" -> value instanceof String;
            case "number", "integer" -> value instanceof Number;
            case "boolean" -> value instanceof Boolean;
            case "object" -> value instanceof Map<?, ?>;
            case "array" -> value instanceof List<?>;
            default -> true;
        };
    }
}

