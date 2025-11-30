const functions = require('firebase-functions/v2');
const admin = require('firebase-admin');
admin.initializeApp();

/**
 * Sends FCM push notification to a list of device tokens.
 * 
 * Trigger: HTTP request
 * Expected body: { tokens: string[], title: string, body: string, data?: object }
 */
exports.sendPushNotification = functions.https.onRequest({
    runtime: 'nodejs20'
}, async (req, res) => {
    // CORS handling
    res.set('Access-Control-Allow-Origin', '*');
    if (req.method === 'OPTIONS') {
        res.set('Access-Control-Allow-Methods', 'POST');
        res.set('Access-Control-Allow-Headers', 'Content-Type');
        res.status(204).send('');
        return;
    }
    
    if (req.method !== 'POST') {
        res.status(405).send('Method Not Allowed');
        return;
    }
    
    try {
        const { tokens, title, body, data } = req.body;
        
        // Validation
        if (!tokens || !Array.isArray(tokens) || tokens.length === 0) {
            res.status(400).json({ error: 'tokens array is required and must not be empty' });
            return;
        }
        if (!title || !body) {
            res.status(400).json({ error: 'title and body are required' });
            return;
        }
        
        // Send to all tokens
        const message = {
            notification: {
                title: title,
                body: body
            },
            data: data || {},
            tokens: tokens
        };
        
        const response = await admin.messaging().sendEachForMulticast(message);
        
        res.status(200).json({
            successCount: response.successCount,
            failureCount: response.failureCount,
            responses: response.responses
        });
    } catch (error) {
        console.error('Error sending FCM:', error);
        res.status(500).json({ error: error.message });
    }
});
