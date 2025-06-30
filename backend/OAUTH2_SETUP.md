# OAuth2 Integration Setup Guide

This guide explains how to set up OAuth2 authentication with Google and GitHub for the LocalBite application.

## 🎯 Overview

The LocalBite application now supports OAuth2 authentication with:
- **Google OAuth2** - Most widely used
- **Facebook OAuth2** - Popular among general users

Users can:
1. Register/login with their existing Google or GitHub accounts
2. Skip email verification (OAuth providers handle this)
3. Automatically get assigned the "BUYER" role
4. Have their profile information populated from OAuth provider

## 🛠️ Setting Up OAuth2 Providers

### Google OAuth2 Setup

1. **Go to Google Cloud Console**
   - Visit: https://console.cloud.google.com/

2. **Create OAuth2 Credentials**
   - Go to "APIs & Services" → "Credentials"
   - Click "Create Credentials" → "OAuth client ID"
   - Application type: "Web application"

3. **Configure Redirect URIs**
   ```
   Authorized redirect URIs:
   - http://localhost:8080/login/oauth2/code/google
   ```

4. **Copy Credentials**
   - Copy the Client ID and Client Secret

### Facebook OAuth2 Setup

1. **Go to Facebook for Developers**
   - Visit: https://developers.facebook.com/

2. **Create New App**
   - Click "Create App"
   - Choose "Consumer" app type
   - App Name: "LocalBite Authentication"

3. **Add Facebook Login Product**
   - In the app dashboard, click "Add Product"
   - Find "Facebook Login" and click "Set Up"

4. **Configure OAuth Settings**
   - Go to Facebook Login → Settings
   - Valid OAuth Redirect URIs: `http://localhost:8080/login/oauth2/code/facebook`

5. **Copy Credentials**
   - Go to Settings → Basic
   - Copy the App ID and App Secret

## 🚀 Environment Variables

Set these before starting the backend:

```bash
export GOOGLE_CLIENT_ID="your-actual-google-client-id"
export GOOGLE_CLIENT_SECRET="your-actual-google-client-secret"
export FACEBOOK_CLIENT_ID="your-actual-facebook-app-id"
export FACEBOOK_CLIENT_SECRET="your-actual-facebook-app-secret"
```

## 🧪 Testing

1. Start backend with OAuth2 credentials
2. Visit http://localhost:3000/auth
3. Click "Continue with Google" or "Continue with Facebook"
4. Complete OAuth2 flow
5. Should redirect to dashboard with JWT tokens

## ✅ Success Indicators

- OAuth2 buttons appear in login form
- Clicking buttons redirects to provider login
- After login, redirected back to dashboard
- User info stored in database
- JWT tokens generated correctly 