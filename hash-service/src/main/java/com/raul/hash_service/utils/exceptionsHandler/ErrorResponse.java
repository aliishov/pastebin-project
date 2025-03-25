package com.raul.hash_service.utils.exceptionsHandler;

import java.util.Map;

public record ErrorResponse(
        Map<String, String> errors
) { }
