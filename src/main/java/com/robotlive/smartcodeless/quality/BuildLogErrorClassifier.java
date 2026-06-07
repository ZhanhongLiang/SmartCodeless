package com.robotlive.smartcodeless.quality;

import com.robotlive.smartcodeless.model.enums.QualityFailureCategoryEnum;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class BuildLogErrorClassifier {

    public String classify(String logs) {
        String text = logs == null ? "" : logs.toLowerCase(Locale.ROOT);
        if (text.contains("cannot find module") || text.contains("failed to resolve import")) {
            return QualityFailureCategoryEnum.IMPORT_RESOLUTION_ERROR.name();
        }
        if (text.contains("typescript") || text.contains("vue-tsc") || text.contains("tsc")) {
            return QualityFailureCategoryEnum.TYPESCRIPT_ERROR.name();
        }
        if (text.contains("vite") || text.contains("build failed")) {
            return QualityFailureCategoryEnum.VITE_BUILD_ERROR.name();
        }
        if (text.contains("npm install") || text.contains("eresolve") || text.contains("enoent")) {
            return QualityFailureCategoryEnum.DEPENDENCY_INSTALL_ERROR.name();
        }
        if (text.contains("router") || text.contains("route")) {
            return QualityFailureCategoryEnum.ROUTE_ERROR.name();
        }
        return QualityFailureCategoryEnum.UNKNOWN_ERROR.name();
    }

    public String sanitize(String logs) {
        if (logs == null) {
            return "";
        }
        String sanitized = logs
                .replaceAll("(?i)(password|token|secret|key)=\\S+", "$1=***")
                .replaceAll("[A-Za-z]:\\\\[^\\s]+", "[本地路径]")
                .replaceAll("/[^\\s]+", "[路径]");
        return sanitized.length() > 6000 ? sanitized.substring(0, 6000) : sanitized;
    }
}
