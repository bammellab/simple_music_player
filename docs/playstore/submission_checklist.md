# Google Play Store Submission Checklist

Complete this checklist before submitting your app to the Play Store.

---

## Pre-Submission Requirements

### Developer Account

- [ ] Google Play Developer account created ($25 one-time fee)
- [ ] Developer profile completed
- [ ] Contact email verified

### App Configuration

- [ ] Unique application ID: `com.bammellab.musicplayer`
    - **Action Required:** Change to your own package name (e.g., `com.yourcompany.musicplayer`)
- [ ] Version code set in `build.gradle`
- [ ] Version name set (e.g., "1.0.0")
- [ ] Target SDK meets Play Store requirements (currently SDK 34+)
- [ ] Signed release APK or AAB generated

---

## Store Listing Checklist

### Basic Information

- [ ] App name: "Music Player" (30 characters max)
- [ ] Short description entered (80 characters max) - see `store_listing.md`
- [ ] Full description entered (4000 characters max) - see `store_listing.md`
- [ ] App category selected: Music & Audio
- [ ] Contact email provided
- [ ] Privacy policy URL provided (host `privacy_policy.md` content online)

### Graphics

- [ ] App icon (512x512 PNG) - auto-generated from project
- [ ] Feature graphic (1024x500) created and uploaded
- [ ] Phone screenshots uploaded (minimum 2, maximum 8)
- [ ] Tablet screenshots (optional but recommended)

### Categorization

- [ ] Primary category: Music & Audio
- [ ] Tags/keywords added

---

## Content Rating

- [ ] Content rating questionnaire completed - see `content_rating_questionnaire.md`
- [ ] Expected rating: Everyone (E)

---

## App Content Declaration

### Privacy

- [ ] Privacy policy created - see `privacy_policy.md`
- [ ] Privacy policy hosted at publicly accessible URL
- [ ] Data safety form completed in Play Console

### Data Safety Responses

For this app, answer as follows:

| Question                                                 | Answer                                         |
|----------------------------------------------------------|------------------------------------------------|
| Does your app collect or share user data?                | No                                             |
| Is all user data encrypted in transit?                   | N/A (no data transmitted)                      |
| Do you provide a way for users to request data deletion? | N/A (no data collected)                        |
| Does your app collect location data?                     | No                                             |
| Does your app collect personal info?                     | No                                             |
| Does your app collect financial info?                    | No                                             |
| Does your app collect health info?                       | No                                             |
| Does your app collect messages?                          | No                                             |
| Does your app collect photos/videos?                     | No                                             |
| Does your app collect audio files?                       | Accesses locally but does not collect/transmit |
| Does your app collect contacts?                          | No                                             |
| Does your app collect app activity?                      | No                                             |
| Does your app collect browsing history?                  | No                                             |
| Does your app collect device identifiers?                | No                                             |

---

## Pricing & Distribution

- [ ] App pricing: Free
- [ ] Countries selected for distribution
- [ ] Contains ads: No
- [ ] In-app purchases: No

---

## Technical Requirements

### Build Requirements

- [ ] Release build generated: `./gradlew assembleRelease` or `./gradlew bundleRelease`
- [ ] APK/AAB signed with release keystore
- [ ] ProGuard/R8 minification enabled (optional but recommended)
- [ ] App tested on multiple devices/API levels

### Testing Checklist

- [ ] App installs correctly
- [ ] Folder selection works
- [ ] Audio playback functions for all supported formats
- [ ] All playback controls work (play/pause/prev/next/volume)
- [ ] Shuffle mode works
- [ ] Seek bar functions correctly
- [ ] App handles permission denial gracefully
- [ ] App handles empty folders
- [ ] App handles rotation/configuration changes
- [ ] No crashes in normal usage

---

## Before You Submit

1. **Package Name Change Required**
    - Current: `com.bammellab.musicplayer`
    - Change to your unique identifier (cannot be changed after publishing)
    - Update in `app/build.gradle`

2. **Host Privacy Policy**
    - Upload `privacy_policy.md` content to a public URL
    - Options: GitHub Pages, your website, Google Sites

3. **Create Feature Graphic**
    - Use specifications in `asset_specifications.md`

4. **Capture Additional Screenshots**
    - Aim for 4-6 screenshots showing key features

5. **Generate Signed Release Build**
   ```bash
   ./gradlew bundleRelease
   ```
    - Use Android App Bundle (AAB) format preferred by Play Store

---

## Post-Submission

- [ ] Monitor review status in Play Console
- [ ] Respond promptly to any policy issues
- [ ] Review typically takes 1-7 days for new apps

---

## Common Rejection Reasons to Avoid

1. **Functionality issues** - Ensure app works as described
2. **Misleading metadata** - Description matches actual features
3. **Privacy policy missing** - Must be accessible via URL
4. **Broken links** - Verify all URLs work
5. **Intellectual property** - Don't use copyrighted assets
6. **Package name** - Must not use `com.bammellab.*`

---

## Files Reference

| Document                          | Purpose                          |
|-----------------------------------|----------------------------------|
| `store_listing.md`                | App name, descriptions, category |
| `privacy_policy.md`               | Privacy policy (host online)     |
| `content_rating_questionnaire.md` | Rating questionnaire answers     |
| `asset_specifications.md`         | Graphic asset requirements       |
| `release_notes.md`                | What's new text                  |
| `submission_checklist.md`         | This checklist                   |
