/**
 * Play Integrity Verification Routes
 * Server-side verification of Play Integrity tokens
 */
const express = require('express');
const router = express.Router();
const crypto = require('crypto');
const { verifyToken } = require('../middleware/auth');

// Google Play Integrity API endpoint
const PLAY_INTEGRITY_API = 'https://playintegrity.googleapis.com/v1';

// Your app's package name
const PACKAGE_NAME = 'com.orignal.buddylynk';

/**
 * Generate a cryptographic nonce for Play Integrity request
 * The nonce should be unique per request and tied to the user action
 */
router.get('/nonce', verifyToken, (req, res) => {
    try {
        const userId = req.userId;

        // Generate random nonce
        const randomBytes = crypto.randomBytes(16);
        const timestamp = Date.now();

        // Create nonce: random + userId hash + timestamp
        const userHash = crypto.createHash('sha256').update(userId).digest('hex').substring(0, 8);
        const nonceData = `${randomBytes.toString('base64')}.${userHash}.${timestamp}`;

        res.json({
            nonce: Buffer.from(nonceData).toString('base64')
        });
    } catch (err) {
        console.error('Nonce generation error:', err);
        res.status(500).json({ error: 'Failed to generate nonce' });
    }
});

/**
 * Verify Play Integrity token from Android app
 * 
 * This is the SERVER-SIDE verification step:
 * 1. Android app gets integrity token from Play Integrity API
 * 2. App sends token to this endpoint
 * 3. Server decodes token with Google's API
 * 4. Server validates the verdict and allows/denies access
 * 
 * Request body: { "integrityToken": "..." }
 */
router.post('/verify', verifyToken, async (req, res) => {
    try {
        const { integrityToken } = req.body;

        if (!integrityToken) {
            return res.status(400).json({
                error: 'Integrity token required',
                valid: false
            });
        }

        // Use Firebase service account for authentication
        const { GoogleAuth } = require('google-auth-library');

        let accessToken;
        try {
            const auth = new GoogleAuth({
                keyFile: process.env.GOOGLE_APPLICATION_CREDENTIALS || './firebase-service-account.json',
                scopes: ['https://www.googleapis.com/auth/playintegrity']
            });
            const client = await auth.getClient();
            const tokenResponse = await client.getAccessToken();
            accessToken = tokenResponse.token;
        } catch (authError) {
            // In production, this should fail
            if (process.env.NODE_ENV === 'production') {
                console.error('[Integrity] CRITICAL: Failed to authenticate with Google:', authError.message);
                return res.status(500).json({
                    valid: false,
                    error: 'Integrity verification not properly configured'
                });
            }
            // Only in development: skip verification
            console.log('[Integrity] Auth failed, skipping verification (dev mode):', authError.message);
            return res.json({
                valid: true,
                warning: 'Integrity verification skipped (development)',
                deviceIntegrity: 'UNKNOWN',
                appIntegrity: 'UNKNOWN'
            });
        }

        // Call Google's Play Integrity API to decode the token
        const url = `${PLAY_INTEGRITY_API}/${PACKAGE_NAME}:decodeIntegrityToken`;

        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`
            },
            body: JSON.stringify({
                integrityToken: integrityToken
            })
        });

        if (!response.ok) {
            const errorText = await response.text();
            console.error('[Integrity] Google API error:', errorText);
            return res.status(400).json({
                valid: false,
                error: 'Failed to verify integrity token',
                details: errorText
            });
        }

        const verdict = await response.json();

        // Parse the verdict
        const tokenPayload = verdict.tokenPayloadExternal || {};

        const appIntegrity = tokenPayload.appIntegrity || {};
        const deviceIntegrity = tokenPayload.deviceIntegrity || {};
        const accountDetails = tokenPayload.accountDetails || {};

        // Check verdicts
        const appRecognition = appIntegrity.appRecognitionVerdict || 'UNKNOWN';
        const deviceRecognition = deviceIntegrity.deviceRecognitionVerdict || [];

        // Determine if the app/device passes our security requirements
        let isValid = true;
        const threats = [];

        // App must be recognized as genuine (from Play Store)
        if (!['PLAY_RECOGNIZED', 'UNRECOGNIZED_VERSION'].includes(appRecognition)) {
            isValid = false;
            threats.push('APP_NOT_GENUINE');
        }

        // Device must pass basic integrity
        if (!deviceRecognition.includes('MEETS_BASIC_INTEGRITY')) {
            isValid = false;
            threats.push('DEVICE_INTEGRITY_FAILED');
        }

        // For extra security, require device certification
        // Uncomment for stricter verification:
        // if (!deviceRecognition.includes('MEETS_DEVICE_INTEGRITY')) {
        //     isValid = false;
        //     threats.push('DEVICE_NOT_CERTIFIED');
        // }

        console.log(`[Integrity] Verification result: valid=${isValid}, threats=${threats.join(',')}`);

        res.json({
            valid: isValid,
            threats: threats,
            appIntegrity: appRecognition,
            deviceIntegrity: deviceRecognition,
            accountVerified: accountDetails.appLicensingVerdict || 'UNKNOWN'
        });

    } catch (err) {
        console.error('Integrity verification error:', err);
        res.status(500).json({
            valid: false,
            error: err.message
        });
    }
});

module.exports = router;
