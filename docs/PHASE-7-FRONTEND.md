# Phase 7 — Frontend: serve static pages on port 3000

Checklist
- [ ] Finalize frontend pages (login, apply, thank-you, offer, upload-document, status)
- [ ] Wire frontend JS to backend endpoints and test CORS
- [ ] Ensure JWT is stored in `sessionStorage` and applied to API calls
- [ ] Add client-side validation for required fields
- [ ] Serve frontend with `http-server` or `live-server` on port 3000
- [ ] Add `.http` test files for common flows

Goal
Provide a clean, vanilla-JS frontend that allows customers to log in, submit applications, accept offers, upload signed documents, and view status. Serve the static site separately on port 3000.

Deliverables
- `frontend/index.html` (templates for pages)
- `frontend/css/styles.css` (branding)
- `frontend/js/app.js` (API calls, JWT handling, UI rendering)
- `frontend/README.md` with serve instructions

Implementation notes
- All backend services must enable CORS for `http://localhost:3000` (property `app.cors.allowed-origins`)
- Keep UI minimal and accessible; no frontend frameworks

Local run
```powershell
cd frontend
# Option A: http-server
npx http-server . -p 3000
# Option B: live-server (auto reload)
npx live-server --port=3000
# Option C: python
python -m http.server 3000
```

Verification
1. Open `http://localhost:3000`
2. Register/login using `auth-service` endpoints
3. Submit an application and ensure API calls return success
4. Accept an offer (simulate via backend/Camunda) and upload a document
5. Confirm status page shows lifecycle progression

Notes
- The frontend currently includes basic sample logic in `frontend/js/app.js`; extend as services become available.

