package com.example.fidodemo.controller;

import com.example.fidodemo.service.WebAuthnService;
import com.yubico.webauthn.*;
import com.yubico.webauthn.data.*;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WebAuthnController {

    private final WebAuthnService webAuthnService;

    @PostMapping("/register/begin")
    public ResponseEntity<PublicKeyCredentialCreationOptions> startRegistration(
            @RequestBody Map<String, String> request, HttpSession session) {
        String username = request.get("username");
        String displayName = request.getOrDefault("displayName", username);

        System.out.println(">>> [REG_BEGIN REQUEST] username: " + username);
        PublicKeyCredentialCreationOptions options = webAuthnService.startRegistration(username, displayName);
        System.out.println(">>> [REG_BEGIN RESPONSE] options: " + options);

        session.setAttribute("reg_options_" + username, options);
        return ResponseEntity.ok(options);
    }

    @PostMapping("/register/finish")
    public ResponseEntity<String> finishRegistration(
            @RequestBody PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> response,
            @RequestParam String username, HttpSession session) {

        System.out.println(">>> [REG_FINISH REQUEST] username: " + username + ", response: " + response);

        PublicKeyCredentialCreationOptions options = (PublicKeyCredentialCreationOptions) session
                .getAttribute("reg_options_" + username);
        if (options == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Session expired or invalid");
        }

        try {
            webAuthnService.finishRegistration(options, response);
            session.removeAttribute("reg_options_" + username);
            System.out.println(">>> [REG_FINISH RESPONSE] Registration successful");
            return ResponseEntity.ok("Registration successful");
        } catch (RegistrationFailedException e) {
            System.out.println(">>> [REG_FINISH RESPONSE] Registration failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Registration failed: " + e.getMessage());
        }
    }

    @PostMapping("/login/begin")
    public ResponseEntity<AssertionRequest> startAuthentication(
            @RequestBody Map<String, String> request, HttpSession session) {
        String username = request.get("username");

        System.out.println(">>> [AUTH_BEGIN REQUEST] username: " + username);
        AssertionRequest assertionRequest = webAuthnService.startAuthentication(username);
        System.out.println(">>> [AUTH_BEGIN RESPONSE] request: " + assertionRequest);

        session.setAttribute("auth_request_" + username, assertionRequest);
        return ResponseEntity.ok(assertionRequest);
    }

    @PostMapping("/login/finish")
    public ResponseEntity<String> finishAuthentication(
            @RequestBody PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> response,
            @RequestParam String username, HttpSession session) {

        System.out.println(">>> [AUTH_FINISH REQUEST] username: " + username + ", response: " + response);

        AssertionRequest request = (AssertionRequest) session.getAttribute("auth_request_" + username);
        if (request == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Session expired or invalid");
        }

        try {
            webAuthnService.finishAuthentication(request, response);
            session.removeAttribute("auth_request_" + username);
            System.out.println(">>> [AUTH_FINISH RESPONSE] Login successful");
            return ResponseEntity.ok("Login successful");
        } catch (AssertionFailedException e) {
            System.out.println(">>> [AUTH_FINISH RESPONSE] Authentication failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication failed: " + e.getMessage());
        }
    }
}
