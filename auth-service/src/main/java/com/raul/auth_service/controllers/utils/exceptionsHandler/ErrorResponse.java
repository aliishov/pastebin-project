package com.raul.auth_service.controllers.utils.exceptionsHandler;

import java.util.Map;

public record ErrorResponse(
        Map<String, String> errors
) { }
