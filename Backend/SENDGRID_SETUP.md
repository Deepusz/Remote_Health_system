# SendGrid Setup for Railway Deployment

## Why SendGrid?
Railway blocks outbound SMTP connections to Gmail for security reasons. SendGrid is a cloud-native email service that works perfectly with Railway.

## Quick Setup (5 minutes)

### Step 1: Create SendGrid Account
1. Go to [SendGrid.com](https://sendgrid.com/)
2. Click "Start for Free"
3. Sign up with your email
4. Verify your email address

### Step 2: Create API Key
1. In SendGrid dashboard → Settings → API Keys
2. Click "Create API Key"
3. Choose "Restricted Access"
4. Under "Mail Send", select "Full Access"
5. Click "Create & View"
6. **Copy the API key** (you won't see it again!)

### Step 3: Verify Sender Identity
1. In SendGrid dashboard → Settings → Sender Authentication
2. Click "Verify a Single Sender"
3. Fill in the form:
   - **From Name**: Remote Health Monitoring System
   - **From Email**: your-email@domain.com (use your own email)
   - **Reply To**: your-email@domain.com
   - **Company Address**: Your address
4. Click "Create"
5. Check your email and click the verification link

### Step 4: Update Railway Environment Variables
1. Go to Railway dashboard → Your project
2. Click on "Variables" tab
3. Add these variables:

```
SENDGRID_API_KEY=your-actual-api-key-here
SENDER_EMAIL=your-verified-email@domain.com
FALL_DETECTION_EMAIL=soorya1133@gmail.com
```

### Step 5: Deploy and Test
1. Push your changes to trigger Railway deployment
2. Test with:
```bash
POST https://remotehealthsystem-production.up.railway.app/api/ts/test-fall-detection-email
Authorization: Bearer my-local-dev-token
```

## SendGrid Free Tier Limits
- **100 emails/day** (perfect for fall detection alerts)
- **Unlimited contacts**
- **All core features included**

## Troubleshooting

### If you get "Unauthorized" error:
- Check your API key is correct
- Ensure the API key has "Mail Send" permissions

### If you get "Sender not verified" error:
- Complete the sender verification process
- Use the verified email address in SENDER_EMAIL

### If emails go to spam:
- SendGrid handles deliverability better than Gmail SMTP
- Add SendGrid's SPF record to your domain (optional)

## Alternative: Use Your Own Email as Sender
If you don't want to verify a sender, you can use your own email:

1. In SendGrid → Settings → Sender Authentication
2. Verify your own email address
3. Set `SENDER_EMAIL=your-email@gmail.com` in Railway variables

## Cost
- **Free**: 100 emails/day (sufficient for fall detection)
- **Paid**: $19.95/month for 50,000 emails (if you need more)

This setup will work reliably with Railway and provide better email deliverability than Gmail SMTP!
