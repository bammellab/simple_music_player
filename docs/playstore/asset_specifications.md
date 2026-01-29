# Google Play Store Asset Specifications

This document outlines all required and optional graphic assets for Play Store submission.

---

## Required Assets

### 1. App Icon (High-Resolution)
- **Dimensions:** 512 x 512 pixels
- **Format:** 32-bit PNG (with alpha)
- **File size:** Up to 1 MB
- **Location:** Already configured in `app/src/main/res/mipmap-*/`

**Design Recommendations:**
- Use a music-themed icon (musical note, headphones, or play button)
- Follow Material Design icon guidelines
- Ensure icon is recognizable at small sizes
- Avoid text in the icon

### 2. Feature Graphic
- **Dimensions:** 1024 x 500 pixels
- **Format:** JPEG or 24-bit PNG (no alpha)
- **File size:** Up to 1 MB

**Design Recommendations:**
- Create a banner showcasing the app's clean interface
- Include app name "Music Player" prominently
- Use colors consistent with app theme (Material 3)
- Show a stylized representation of the app's functionality
- Avoid excessive text

**Suggested Content:**
- Background: Gradient using app's primary colors
- Center: App icon or music visualization graphic
- Text: "Music Player" with tagline "Simple. Clean. Local."

### 3. Screenshots (Phone)
- **Minimum:** 2 screenshots
- **Maximum:** 8 screenshots
- **Dimensions:** 16:9 or 9:16 aspect ratio
- **Minimum dimension:** 320 pixels
- **Maximum dimension:** 3840 pixels
- **Format:** JPEG or 24-bit PNG (no alpha)

**Recommended Screenshots:**

1. **Folder Selection Screen**
   - Shows "Select Folder" button
   - Caption: "Choose your music folder"

2. **Track List View**
   - Shows list of audio files
   - Current track highlighted
   - Caption: "Browse your music collection"

3. **Now Playing View**
   - Shows current track info
   - Seek bar visible
   - Caption: "Clean Now Playing interface"

4. **Playback Controls**
   - Shows all control buttons
   - Caption: "Intuitive playback controls"

5. **Shuffle Mode Active**
   - Shuffle button highlighted
   - Caption: "Shuffle for variety"

**Note:** Existing screenshot available at `docs/screenshots/screenshot.png`

---

## Optional Assets

### 4. Screenshots (Tablet - 7 inch)
- **Dimensions:** Same requirements as phone
- **Recommended:** At least 2 if tablet layout differs

### 5. Screenshots (Tablet - 10 inch)
- **Dimensions:** Same requirements as phone
- **Recommended:** At least 2 if tablet layout differs

### 6. Promotional Video
- **Platform:** YouTube
- **Duration:** 30 seconds to 2 minutes recommended
- **Content suggestions:**
  - App walkthrough
  - Feature demonstration
  - No audio required (many view without sound)

### 7. TV Banner (if applicable)
- **Dimensions:** 1280 x 720 pixels
- **Note:** Only needed if targeting Android TV

---

## Asset Checklist

| Asset                      | Required | Status                 |
|----------------------------|----------|------------------------|
| App Icon (512x512)         | Yes      | Configured in project  |
| Feature Graphic (1024x500) | Yes      | To be created          |
| Phone Screenshots (min 2)  | Yes      | 1 available, need more |
| Tablet 7" Screenshots      | No       | Optional               |
| Tablet 10" Screenshots     | No       | Optional               |
| Promotional Video          | No       | Optional               |

---

## Color Palette Reference

For consistent branding, use these colors from the app theme:

```
Primary: Material 3 dynamic colors
Background: System default (light/dark)
Accent: Follow Material You guidelines
```

---

## Tools for Creating Assets

**Recommended Tools:**
- Figma (free) - For feature graphic and screenshot frames
- Canva (free) - Easy banner/graphic creation
- Android Studio - For capturing clean screenshots
- Screener - For adding device frames to screenshots

**Screenshot Capture:**
1. Run app on emulator or device
2. Use `adb shell screencap` or Android Studio screenshot tool
3. Frame with device mockup if desired
