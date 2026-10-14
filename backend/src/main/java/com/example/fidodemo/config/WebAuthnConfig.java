package com.example.fidodemo.config;

import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.yubico.webauthn.CredentialRepository;

@Configuration
public class WebAuthnConfig {

        @Value("${webauthn.rp.id}")
        private String rpId;

        @Value("${webauthn.rp.name}")
        private String rpName;

        @Value("${webauthn.rp.origins}")
        private String[] rpOrigins;

        @Bean
        public RelyingParty relyingParty(CredentialRepository repository) {
                RelyingPartyIdentity rpIdentity = RelyingPartyIdentity.builder()
                                .id(rpId)
                                .name(rpName)
                                .build();

                return RelyingParty.builder()
                                .identity(rpIdentity)
                                .credentialRepository(repository)
                                .origins(java.util.Set.of(rpOrigins))
                                .build();
        }
}
