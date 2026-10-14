package com.example.fidodemo.repository;

import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.example.fidodemo.model.User;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class CredentialRepositoryImpl implements CredentialRepository {

    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, Set<RegisteredCredential>> credentials = new ConcurrentHashMap<>();

    public void addUser(User user) {
        users.put(user.getUsername(), user);
    }

    public Optional<User> getUser(String username) {
        return Optional.ofNullable(users.get(username));
    }

    public void addCredential(String username, RegisteredCredential credential) {
        credentials.computeIfAbsent(username, k -> new HashSet<>()).add(credential);
    }

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        return credentials.getOrDefault(username, Collections.emptySet()).stream()
                .map(reg -> PublicKeyCredentialDescriptor.builder()
                        .id(reg.getCredentialId())
                        .build())
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        return getUser(username).map(user -> new ByteArray(user.getHandle()));
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        return users.values().stream()
                .filter(user -> Arrays.equals(user.getHandle(), userHandle.getBytes()))
                .map(User::getUsername)
                .findFirst();
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        String username = getUsernameForUserHandle(userHandle).orElse(null);
        if (username == null)
            return Optional.empty();

        return credentials.getOrDefault(username, Collections.emptySet()).stream()
                .filter(reg -> reg.getCredentialId().equals(credentialId))
                .findFirst();
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        return credentials.values().stream()
                .flatMap(Collection::stream)
                .filter(reg -> reg.getCredentialId().equals(credentialId))
                .collect(Collectors.toSet());
    }
}
