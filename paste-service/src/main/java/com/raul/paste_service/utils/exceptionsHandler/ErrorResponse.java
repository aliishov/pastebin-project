package com.raul.paste_service.utils.exceptionsHandler;

import java.util.Map;

public record ErrorResponse(
        Map<String, String> errors
) { }
