/**
 * Zypo AI Backend Server - Ephemeral Token & Auth Middleware Reference
 *
 * Provides POST /api/live-token for generating ephemeral Gemini Live tokens
 * after verifying Firebase ID tokens from authenticated Android users.
 */

const express = require('express');
const admin = require('firebase-admin');
const axios = require('axios');
require('dotenv').config();

const app = express();
app.use(express.json());

// Initialize Firebase Admin SDK
if (process.env.FIREBASE_SERVICE_ACCOUNT) {
  const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
} else {
  admin.initializeApp();
}

/**
 * POST /api/live-token
 * Authenticates Firebase ID Token and returns a short-lived ephemeral Gemini token
 */
app.post('/api/live-token', async (req, res) => {
  try {
    const authHeader = req.headers.authorization;
    const token = authHeader && authHeader.startsWith('Bearer ') ? authHeader.split('Bearer ')[1] : req.body.firebaseIdToken;

    if (!token) {
      return res.status(401).json({ error: 'Missing Firebase ID token' });
    }

    // 1. Verify Firebase ID token
    const decodedToken = await admin.auth().verifyIdToken(token);
    const uid = decodedToken.uid;

    if (!uid) {
      return res.status(403).json({ error: 'Invalid or unverified user' });
    }

    // 2. Fetch short-lived ephemeral token from Gemini API
    const geminiApiKey = process.env.GEMINI_API_KEY;
    if (!geminiApiKey) {
      return res.status(500).json({ error: 'Server configuration error: missing GEMINI_API_KEY' });
    }

    const response = await axios.post(
      `https://generativelanguage.googleapis.com/v1beta/tokens?key=${geminiApiKey}`,
      {
        ttl: '1800s',
        clientAuth: {
          firebaseUid: uid
        }
      }
    );

    const ephemeralToken = response.data.name || response.data.token;

    return res.json({
      uid: uid,
      ephemeralToken: ephemeralToken,
      expiresInSeconds: 1800
    });
  } catch (error) {
    console.error('Error generating live token:', error.message);
    return res.status(500).json({ error: 'Unable to start Zypo AI voice session.' });
  }
});

const PORT = process.env.PORT || 8080;
app.listen(PORT, () => {
  console.log(`Zypo AI backend running on port ${PORT}`);
});
