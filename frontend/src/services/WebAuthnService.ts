import { create, get } from '@github/webauthn-json';

const API_BASE = '/api';

export const startRegistration = async (username: string, displayName: string) => {
    const response = await fetch(`${API_BASE}/register/begin`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, displayName }),
        credentials: 'include',
    });
    if (!response.ok) throw new Error('Failed to start registration');
    const options = await response.json();

    // Selectively handle extensions to avoid browser errors
    if (options.extensions) {
        // Remove problematic extensions and null values
        delete options.extensions.appidExclude;
        delete options.extensions.appid;
        delete options.extensions.largeBlob;
        delete options.extensions.uvm;
    } else {
        options.extensions = {
            credProps: true
        };
    }

    // Ensure authenticatorSelection is present and inclusive
    if (!options.authenticatorSelection) {
        options.authenticatorSelection = {
            residentKey: 'required',
            userVerification: 'preferred'
        };
    } else {
        const sel = options.authenticatorSelection as any;
        sel.userVerification = 'preferred';
        // Remove forced cross-platform to allow platform authenticators (like synced passkeys)
        if (sel.authenticatorAttachment) {
            delete sel.authenticatorAttachment;
        }
    }

    // Fix transports in excludeCredentials
    if (options.excludeCredentials) {
        options.excludeCredentials.forEach((cred: any) => {
            delete cred.transports;
        });
    }

    // Defensive cleanup of authenticatorSelection
    if (options.authenticatorSelection) {
        const sel = options.authenticatorSelection as any;
        if (sel.authnticatorAttachment !== undefined) {
            // Fix typo if it exists
            delete sel.authnticatorAttachment;
        }
    }

    if (options.timeout === null || options.timeout === 0) delete options.timeout;

    // Log options for debugging
    console.log("Final WebAuthn Options for navigator.credentials.create:", JSON.stringify(options, null, 2));

    // Use webauthn-json to handle the creation
    const credential = await create({ publicKey: options });

    const finishResponse = await fetch(`${API_BASE}/register/finish?username=${username}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(credential),
        credentials: 'include',
    });
    if (!finishResponse.ok) {
        const errorText = await finishResponse.text();
        throw new Error(errorText || 'Failed to finish registration');
    }
    return await finishResponse.text();
};

export const startAuthentication = async (username: string) => {
    const response = await fetch(`${API_BASE}/login/begin`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username }),
        credentials: 'include',
    });
    if (!response.ok) throw new Error('Failed to start authentication');
    const options = await response.json();

    // Fix transports by removing it entirely for the demo
    const requestOptions = options.publicKeyCredentialRequestOptions;
    if (requestOptions.allowCredentials) {
        requestOptions.allowCredentials.forEach((cred: any) => {
            delete cred.transports;
        });
    }

    // Remove extensions and null values to avoid browser issues
    if (requestOptions.extensions) delete requestOptions.extensions;
    if (requestOptions.timeout === null) delete requestOptions.timeout;
    if (requestOptions.userVerification === null) delete requestOptions.userVerification;

    // Log options for debugging
    console.log("Cleaned Auth Options:", requestOptions);

    // Use webauthn-json to handle the assertion
    const credential = await get({ publicKey: requestOptions });

    const finishResponse = await fetch(`${API_BASE}/login/finish?username=${username}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(credential),
        credentials: 'include',
    });
    if (!finishResponse.ok) {
        const errorText = await finishResponse.text();
        throw new Error(errorText || 'Failed to finish authentication');
    }
    return await finishResponse.text();
};
