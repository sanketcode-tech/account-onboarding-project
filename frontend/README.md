# Northbridge Bank Frontend

Static HTML/CSS/JavaScript frontend for the Northbridge Bank Onboarding System.

## Overview

The frontend is a single-page application (SPA) built with vanilla HTML, CSS, and JavaScript (no frameworks). It communicates with the backend microservices via REST APIs.

## Setup & Run

### Prerequisites

- Node.js 16+ (for `http-server` or `live-server`)
- OR Python 3.x (if you prefer `python -m http.server`)

### Option 1: Using Node.js http-server (Recommended)

```bash
# Install http-server globally
npm install -g http-server

# Navigate to frontend folder
cd frontend

# Start server on port 3000
http-server . -p 3000
```

Then open your browser to: `http://localhost:3000`

### Option 2: Using Python

```bash
# Navigate to frontend folder
cd frontend

# Start server on port 3000 (Python 3.x)
python -m http.server 3000
```

Then open your browser to: `http://localhost:3000`

### Option 3: Using Node.js live-server (with auto-reload)

```bash
# Install live-server globally
npm install -g live-server

# Navigate to frontend folder
cd frontend

# Start server on port 3000 with auto-reload
live-server --port=3000
```

## Project Structure

```
frontend/
├── index.html             (Main HTML file with templates)
├── css/
│   └── styles.css         (All CSS styles - branding + responsive)
├── js/
│   └── app.js             (Main JavaScript application logic)
└── README.md              (This file)
```

## Features

### Pages

1. **Login Page** (`login.html` in templates)
   - Username/password login form
   - Calls `/api/auth/login` on auth-service (port 8081)
   - Stores JWT token in sessionStorage
   - Redirects to application form on success

2. **Application Form** (`apply-page` template)
   - Three fieldsets: Personal Details, Employment Details, Account Preferences
   - Required fields: fullName, dob, panNumber, address, occupation, annualIncome, employerName, branch, initialDeposit
   - Optional: debitCardRequired checkbox
   - Calls `/api/applications` on application-service (port 8082)
   - Includes JWT bearer token in Authorization header

3. **Status Dashboard** (`status-page` template)
   - Shows current application status
   - Visual timeline with 8 steps (SUBMITTED → ACTIVE)
   - Displays application ID, current status, last updated timestamp
   - Auto-loads from `/api/status/{applicationId}` on onboarding-service (port 8083)

### Navigation

- Top navbar with Northbridge Bank branding (navy blue + gold)
- Menu items: Home, Apply, Status
- Login/Logout button (auto-toggles based on JWT token presence)

### Styling

- **Color Scheme:**
  - Primary: Navy Blue (#1a3a52)
  - Accent: Gold (#d4af37)
  - Neutral: Light gray backgrounds, dark gray text
- **Layout:** Flexbox-based, responsive on mobile (768px breakpoint)
- **Typography:** System fonts (-apple-system, BlinkMacSystemFont, etc.)
- **Components:** Buttons, forms, fieldsets, cards, timeline

## API Integration

### Endpoints Called

| Method | Endpoint | Service | Purpose |
|--------|----------|---------|---------|
| POST | `/api/auth/login` | auth-service:8081 | Login & get JWT token |
| POST | `/api/applications` | application-service:8082 | Submit application |
| GET | `/api/status/{applicationId}` | onboarding-service:8083 | Fetch application status |

### CORS

All backend services must enable CORS for `http://localhost:3000`. This is configured in each service's `app.cors.allowed-origins` property.

## Development

### Modifying Pages

Edit the `<template>` elements in `index.html` to change page layouts.

### Modifying Styles

Edit `css/styles.css` to change colors, spacing, or responsive behavior.

### Adding JavaScript Functions

Add functions to `js/app.js` and call them from event handlers in the HTML.

## Troubleshooting

### "CORS error: Access-Control-Allow-Origin"
- Ensure backend services are running and have CORS enabled for `http://localhost:3000`
- Check `app.cors.allowed-origins` in each service's `application-local.yml`
- Example:
  ```yaml
  app:
    cors:
      allowed-origins: http://localhost:3000
  ```

### "Login failed / Cannot reach auth-service"
- Verify auth-service is running: `mvn -pl auth-service spring-boot:run`
- Check auth-service is listening on port 8081: `curl http://localhost:8081/api/auth/login -X OPTIONS`

### "Application submission fails"
- Ensure JWT token is valid (not expired, stored in sessionStorage)
- Verify application-service is running on port 8082
- Check browser console for detailed error messages

### "Page not loading or styles missing"
- Ensure static server is running on port 3000
- Check browser console for 404 errors on CSS/JS files
- Verify folder structure: `frontend/css/styles.css` and `frontend/js/app.js` exist

## Browser Support

Tested on:
- Chrome/Chromium 90+
- Firefox 88+
- Safari 14+
- Edge 90+

ES6 features used: arrow functions, const/let, template literals, async/await.

## Future Enhancements

- Add WebSocket support for real-time status updates (instead of polling)
- Add more detailed offer display page
- Add document upload UI for signing ceremony
- Add notification banner component
- Migrate to a frontend framework (React, Vue) if needed for complexity

