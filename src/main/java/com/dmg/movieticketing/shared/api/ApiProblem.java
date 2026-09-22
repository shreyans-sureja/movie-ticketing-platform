package com.dmg.movieticketing.shared.api;

import java.util.List;

public record ApiProblem(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        String code,
        List<FieldViolation> violations
) {
    public static ApiProblem of(
            String type,
            String title,
            int status,
            String detail,
            String instance,
            String code
    ) {
        return new ApiProblem(type, title, status, detail, instance, code, List.of());
    }
}

