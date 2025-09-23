# Fall Detection Email Notification Setup

This document explains how to set up and configure the fall detection email notification system for your Remote Health Monitoring System.

## Overview

The system now automatically detects when `field4` (Fall Detection Status) has a value of "1" in the ThingSpeak feeds and sends an emergency email notification with the current health metrics.

## Configuration

### 1. Email Provider Setup (Gmail Example)

1. **Enable 2-Factor Authentication** on your Gmail account
2. **Generate an App Password**:
   - Go to Google Account settings
   - Security → 2-Step Verification → App passwords
   - Generate a password for "Mail"
   - Use this password in your configuration

### 2. Environment Variables

Set the following environment variables or update `application.properties`:

```bash
# Email Configuration
EMAIL_USERNAME=your-email@gmail.com
EMAIL_PASSWORD=your-16-character-app-password
FALL_DETECTION_EMAIL=alert@example.com
```

### 3. Application Properties

Update `src/main/resources/application.properties`:

```properties
# Email Configuration for Fall Detection Alerts
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${EMAIL_USERNAME:your-email@gmail.com}
spring.mail.password=${EMAIL_PASSWORD:your-app-password}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true

# Fall Detection Email Settings
app.fall-detection.notification-email=${FALL_DETECTION_EMAIL:alert@example.com}
app.fall-detection.subject=🚨 FALL DETECTED - Emergency Alert
```

## How It Works

### Automatic Detection

1. When you call the `/api/ts/feeds` endpoint, the system automatically checks all returned feeds
2. If any feed has `field4` set to "1", it triggers an email notification
3. The email includes:
   - Current heart rate (field1)
   - SpO₂ level (field2) 
   - Temperature (field3)
   - Entry ID and timestamp
   - Emergency action instructions

### Email Content

The email contains:
- 🚨 Emergency alert header
- Current health metrics
- Device information
- Timestamp
- Action instructions

## Testing

### Test Email Functionality

Use the test endpoint to verify email configuration:

```bash
POST /api/ts/test-fall-detection-email
Authorization: Bearer my-local-dev-token
Content-Type: application/json

# Optional parameters (defaults provided):
?heartRate=85&spo2=98&temperature=36.5
```

### Test with Real ThingSpeak Data

1. Ensure your device sends `field4=1` to ThingSpeak
2. Call `/api/ts/feeds` endpoint
3. Check the console logs for fall detection messages
4. Verify email is received

## API Endpoints

### Existing Endpoints (Enhanced)

- `GET /api/ts/feeds` - Now automatically checks for fall detection
- `POST /api/ts/update` - Unchanged

### New Endpoints

- `POST /api/ts/test-fall-detection-email` - Test email functionality

## Logging

The system logs fall detection events:

```
🚨 FALL DETECTED! Entry ID: 237, Heart Rate: 0, SpO2: 0, Temperature: 36.24
Fall detection alert email sent successfully to: alert@example.com
```

## Troubleshooting

### Email Not Sending

1. **Check credentials**: Verify EMAIL_USERNAME and EMAIL_PASSWORD
2. **Check app password**: Ensure you're using an app-specific password, not your regular password
3. **Check SMTP settings**: Verify host, port, and TLS settings
4. **Check logs**: Look for error messages in console output

### Gmail Issues

1. **Less secure apps**: Enable 2FA and use app passwords
2. **Quota limits**: Check Gmail sending limits
3. **Spam folder**: Check recipient's spam folder

### ThingSpeak Issues

1. **API keys**: Verify read key has proper permissions
2. **Channel ID**: Ensure channel ID is correct
3. **Data format**: Verify field4 is being set to "1" for falls

## Security Considerations

1. **Environment variables**: Use environment variables for sensitive data
2. **App passwords**: Use app-specific passwords, not account passwords
3. **Authorization**: All endpoints require Bearer token authentication
4. **Rate limiting**: Consider implementing rate limiting for email notifications

## Customization

### Email Template

Edit `EmailService.java` to customize:
- Email subject
- Email body content
- Recipient logic
- Additional health metrics

### Notification Logic

Modify `ThingSpeakService.java` to customize:
- Detection criteria
- Frequency of checks
- Additional validation logic

## Production Deployment

1. **Environment variables**: Set all email configuration via environment variables
2. **Monitoring**: Set up monitoring for email delivery
3. **Logging**: Configure proper logging levels
4. **Rate limiting**: Implement rate limiting to prevent spam
5. **Fallback**: Consider SMS or push notification fallbacks
