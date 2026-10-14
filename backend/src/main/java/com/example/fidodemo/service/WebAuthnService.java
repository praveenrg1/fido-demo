package com.example.fidodemo.service;

import com.example.fidodemo.model.User;
import com.example.fidodemo.repository.CredentialRepositoryImpl;
import com.yubico.webauthn.*;
import com.yubico.webauthn.data.*;
import com.yubico.webauthn.exception.RegistrationFailedException;
import com.yubico.webauthn.exception.AssertionFailedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WebAuthnService {

        private final RelyingParty relyingParty;
        private final CredentialRepositoryImpl repository;
        private final SecureRandom random = new SecureRandom();

        public PublicKeyCredentialCreationOptions startRegistration(String username, String displayName) {
                User user = repository.getUser(username).orElseGet(() -> {
                        byte[] handle = new byte[32];
                        random.nextBytes(handle);
                        User newUser = User.builder()
                                        .username(username)
                                        .displayName(displayName)
                                        .handle(handle)
                                        .build();
                        repository.addUser(newUser);
                        return newUser;
                });

                return relyingParty.startRegistration(StartRegistrationOptions.builder()
                                .user(UserIdentity.builder()
                                                .name(user.getUsername())
                                                .displayName(user.getDisplayName())
                                                .id(new ByteArray(user.getHandle()))
                                                .build())
                                .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                                                .residentKey(ResidentKeyRequirement.REQUIRED)
                                                .userVerification(UserVerificationRequirement.PREFERRED)
                                                .build())
                                .timeout(60000L)
                                .build());
        }

        public RegistrationResult finishRegistration(PublicKeyCredentialCreationOptions options,
                        PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> response)
                        throws RegistrationFailedException {
                RegistrationResult result = relyingParty.finishRegistration(FinishRegistrationOptions.builder()
                                .request(options)
                                .response(response)
                                .build());

                String username = options.getUser().getName();
                repository.addCredential(username, RegisteredCredential.builder()
                                .credentialId(result.getKeyId().getId())
                                .userHandle(options.getUser().getId())
                                .publicKeyCose(result.getPublicKeyCose())
                                .signatureCount(result.getSignatureCount())
                                .build());

                return result;
        }

        public AssertionRequest startAuthentication(String username) {
                return relyingParty.startAssertion(StartAssertionOptions.builder()
                                .username(Optional.ofNullable(username))
                                .build());
        }

        public AssertionResult finishAuthentication(AssertionRequest request,
                        PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> response)
                        throws AssertionFailedException {
                return relyingParty.finishAssertion(FinishAssertionOptions.builder()
                                .request(request)
                                .response(response)
                                .build());
        }
}
