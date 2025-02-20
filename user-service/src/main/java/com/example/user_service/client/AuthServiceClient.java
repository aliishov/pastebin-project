
package com.example.user_service.client;

import com.example.user_service.dto.MessageResponse;
import com.example.user_service.dto.ResendConfirmationRequest;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "auth-service", url = "${auth.service.url}")
public interface AuthServiceClient {

    @PostMapping("resend-confirmation")
    ResponseEntity<MessageResponse> resendConfirmation(@RequestBody @Valid ResendConfirmationRequest request);

}
