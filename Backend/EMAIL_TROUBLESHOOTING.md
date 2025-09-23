# Email SMTP Connection Troubleshooting Guide

## Current Issue: Connection Timeout to Gmail SMTP

You're experiencing connection timeouts when trying to connect to Gmail's SMTP server from Railway. This is a common issue with cloud platforms due to network restrictions.

## Immediate Solutions

### 1. Try Alternative SMTP Providers

Railway might block Gmail's SMTP servers. Try these alternatives:

#### Option A: Outlook/Hotmail SMTP
```properties
# Comment out Gmail settings and uncomment these:
spring.mail.host=smtp-mail.outlook.com
spring.mail.port=587
spring.mail.username=${EMAIL_USERNAME:your-outlook-email@outlook.com}
spring.mail.password=${EMAIL_PASSWORD:your-outlook-password}
```

#### Option B: Yahoo SMTP
```properties
# Comment out Gmail settings and uncomment these:
spring.mail.host=smtp.mail.yahoo.com
spring.mail.port=587
spring.mail.username=${EMAIL_USERNAME:your-yahoo-email@yahoo.com}
spring.mail.password=${EMAIL_PASSWORD:your-yahoo-app-password}
```

### 2. Use Third-Party Email Services

These are more reliable for cloud deployments:

#### SendGrid (Recommended)
1. Sign up at [SendGrid](https://sendgrid.com/)
2. Create an API key
3. Update your configuration:
```properties
spring.mail.host=smtp.sendgrid.net
spring.mail.port=587
spring.mail.username=apikey
spring.mail.password=${SENDGRID_API_KEY:your-sendgrid-api-key}
```

#### Mailgun
1. Sign up at [Mailgun](https://www.mailgun.com/)
2. Get SMTP credentials
3. Update your configuration:
```properties
spring.mail.host=smtp.mailgun.org
spring.mail.port=587
spring.mail.username=${MAILGUN_SMTP_USERNAME:your-mailgun-username}
spring.mail.password=${MAILGUN_SMTP_PASSWORD:your-mailgun-password}
```

### 3. Railway-Specific Solutions

#### Check Railway Network Policies
Railway might have specific network restrictions. Check:
1. Railway dashboard → Your project → Settings → Network
2. Look for any firewall rules or restrictions

#### Use Railway Environment Variables
Set these in your Railway project settings:
```
EMAIL_USERNAME=your-email@domain.com
EMAIL_PASSWORD=your-app-password
FALL_DETECTION_EMAIL=alert@domain.com
```

### 4. Test Different Ports

Try different SMTP ports:
```properties
# Port 465 (SSL)
spring.mail.port=465
spring.mail.properties.mail.smtp.ssl.enable=true
spring.mail.properties.mail.smtp.starttls.enable=false

# Port 25 (if not blocked)
spring.mail.port=25
```

## Debugging Steps

### 1. Enable Email Debugging
Add to your `application.properties`:
```properties
spring.mail.properties.mail.debug=true
logging.level.org.springframework.mail=DEBUG
```

### 2. Test Email Configuration
Use the test endpoint:
```bash
POST https://remotehealthsystem-production.up.railway.app/api/ts/test-fall-detection-email
Authorization: Bearer my-local-dev-token
```

### 3. Check Railway Logs
Look for these log messages:
- `Email configuration initialized:` - Shows your SMTP settings
- `Attempting to send fall detection email to:` - Shows email attempt
- `✅ Fall detection alert email sent successfully` - Success
- `❌ Failed to send fall detection email:` - Failure with details

## Alternative Solutions

### 1. Webhook-Based Notifications
Instead of email, use webhooks to notify external services:
- Slack webhooks
- Discord webhooks
- HTTP POST to notification services

### 2. Database Logging
Log fall detection events to database for later processing:
```java
// Add to ThingSpeakService
private void logFallDetection(String entryId, String heartRate, String spo2, String temperature) {
    // Log to database or file
    System.out.println("FALL_DETECTED: " + entryId + "," + heartRate + "," + spo2 + "," + temperature);
}
```

### 3. External Email Service
Use services like:
- [EmailJS](https://www.emailjs.com/) - Client-side email sending
- [Formspree](https://formspree.io/) - Form-based email sending
- [Netlify Forms](https://www.netlify.com/products/forms/) - If using Netlify

## Quick Fix for Immediate Testing

### Use Console Logging Instead
Temporarily disable email and use console logging:

```java
// In EmailService.java, replace the email sending with:
System.out.println("🚨 FALL DETECTION ALERT 🚨");
System.out.println("Entry ID: " + entryId);
System.out.println("Heart Rate: " + heartRate);
System.out.println("SpO2: " + spo2);
System.out.println("Temperature: " + temperature);
System.out.println("Time: " + LocalDateTime.now());
```

This will log fall detection events to Railway logs, which you can monitor.

## Recommended Action Plan

1. **Immediate**: Try Outlook/Hotmail SMTP (usually works better with cloud platforms)
2. **Short-term**: Set up SendGrid account for reliable email delivery
3. **Long-term**: Implement webhook notifications for better reliability

## Testing Commands

### Test SMTP Connection Locally
```bash
# Test Gmail SMTP
telnet smtp.gmail.com 587

# Test Outlook SMTP
telnet smtp-mail.outlook.com 587
```

### Test from Railway
Check Railway logs for connection attempts and error messages.

## Environment Variables for Railway

Set these in your Railway project:
```
EMAIL_USERNAME=your-email@domain.com
EMAIL_PASSWORD=your-app-password
FALL_DETECTION_EMAIL=alert@domain.com
SENDGRID_API_KEY=your-sendgrid-api-key (if using SendGrid)
```
