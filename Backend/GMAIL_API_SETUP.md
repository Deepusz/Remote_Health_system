# Gmail API Setup for Railway Deployment

## Why Gmail API?
Railway blocks SMTP connections, but Gmail API works over HTTPS, making it perfect for cloud deployments.

## Quick Setup (10 minutes)

### Step 1: Create Google Cloud Project
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Click "Select a project" → "New Project"
3. Name it "Remote Health Monitoring System"
4. Click "Create"

### Step 2: Enable Gmail API
1. In your project, go to "APIs & Services" → "Library"
2. Search for "Gmail API"
3. Click on it and press "Enable"

### Step 3: Create OAuth 2.0 Credentials
1. Go to "APIs & Services" → "Credentials"
2. Click "Create Credentials" → "OAuth client ID"
3. Choose "Web application"
4. Add these redirect URIs:
   - `http://localhost:8080`
   - `https://remotehealthsystem-production.up.railway.app`
5. Click "Create"
6. **Download the JSON file** - you'll need it

### Step 4: Get Refresh Token
You need to get a refresh token from your OAuth credentials. Here's a quick way:

#### Option A: Use Google's OAuth Playground (Easiest)
1. Go to [OAuth 2.0 Playground](https://developers.google.com/oauthplayground/)
2. Click the gear icon (⚙️) → "Use your own OAuth credentials"
3. Enter your Client ID and Client Secret from Step 3
4. In the left panel, find "Gmail API v1" → "https://www.googleapis.com/auth/gmail.send"
5. Click "Authorize APIs"
6. Sign in with your Gmail account
7. Click "Exchange authorization code for tokens"
8. **Copy the refresh_token** (not access_token)

#### Option B: Use a Simple Script
Create a file `get-token.js`:
```javascript
const { google } = require('googleapis');
const readline = require('readline');

const oauth2Client = new google.auth.OAuth2(
  'YOUR_CLIENT_ID',
  'YOUR_CLIENT_SECRET',
  'http://localhost:3000/oauth2callback'
);

const authUrl = oauth2Client.generateAuthUrl({
  access_type: 'offline',
  scope: ['https://www.googleapis.com/auth/gmail.send'],
});

console.log('Authorize this app by visiting this url:', authUrl);

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout,
});

rl.question('Enter the code from that page here: ', (code) => {
  oauth2Client.getToken(code, (err, token) => {
    if (err) return console.error('Error retrieving access token', err);
    console.log('Refresh Token:', token.refresh_token);
    rl.close();
  });
});
```

Run with: `node get-token.js`

### Step 5: Update Railway Environment Variables
In Railway dashboard → Variables, add:
```
GMAIL_CLIENT_ID=your-client-id-from-step-3
GMAIL_CLIENT_SECRET=your-client-secret-from-step-3
GMAIL_REFRESH_TOKEN=your-refresh-token-from-step-4
GMAIL_USER_EMAIL=kingdpk8@gmail.com
FALL_DETECTION_EMAIL=soorya1133@gmail.com
```

### Step 6: Test
Deploy and test with:
```bash
POST https://remotehealthsystem-production.up.railway.app/api/ts/test-fall-detection-email
Authorization: Bearer my-local-dev-token
```

## Troubleshooting

### "Invalid credentials" error:
- Check your Client ID and Client Secret are correct
- Ensure the refresh token is valid (not expired)

### "Insufficient permissions" error:
- Make sure you enabled Gmail API in Google Cloud Console
- Verify the OAuth scope includes `gmail.send`

### "User not found" error:
- Check GMAIL_USER_EMAIL matches your Gmail address
- Ensure the email is verified in Google Cloud Console

## Benefits of Gmail API
- ✅ Works with Railway (no SMTP blocking)
- ✅ Uses your existing Gmail account
- ✅ Better deliverability than SMTP
- ✅ Free (within Gmail limits)
- ✅ No third-party dependencies

## Gmail API Limits
- **Free**: 1 billion quota units per day
- **Rate limit**: 250 quota units per user per 100 seconds
- **Each email**: 100 quota units
- **Practical limit**: ~25,000 emails per day (more than enough!)

This setup will work reliably with Railway and use your existing Gmail account! 🚀
