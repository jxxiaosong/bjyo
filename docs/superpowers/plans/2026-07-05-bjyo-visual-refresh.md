# BJYO Visual Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Re-imagine and implement the BJYO login/register screens and admin workbench with a polished enterprise operations visual direction.

**Architecture:** Keep the existing Thymeleaf templates and JavaScript form contracts intact. Add isolated CSS files for auth, shell, and workbench visuals so the redesign is easy to review and revert.

**Tech Stack:** Spring Boot, Thymeleaf, Bootstrap 3, Font Awesome, static CSS, generated PNG asset.

---

### Task 1: Project Visual Asset

**Files:**
- Create: `bjyo-admin/src/main/resources/static/img/bjyo-ops-bg.png`

- [x] **Step 1: Generate the background image**

Use built-in image generation with a restrained enterprise operations-center prompt. Save the selected image under `bjyo-admin/src/main/resources/static/img/bjyo-ops-bg.png`.

- [x] **Step 2: Verify the asset exists**

Run: `file bjyo-admin/src/main/resources/static/img/bjyo-ops-bg.png`

Expected: PNG image data.

### Task 2: Authentication Screens

**Files:**
- Create: `bjyo-admin/src/main/resources/static/bjyo/css/bjyo-auth.css`
- Modify: `bjyo-admin/src/main/resources/templates/login.html`
- Modify: `bjyo-admin/src/main/resources/templates/register.html`

- [x] **Step 1: Add shared auth CSS**

Create a full-screen responsive layout using the generated background image, a left product narrative panel, and a right glass-like form panel.

- [x] **Step 2: Update login template**

Preserve `form#signupForm`, `name="username"`, `name="password"`, `name="validateCode"`, `name="rememberme"`, and `#btnSubmit`.

- [x] **Step 3: Update register template**

Preserve `form#registerForm`, `name="username"`, `name="password"`, `name="confirmPassword"`, `name="validateCode"`, `name="acceptTerm"`, and `#btnSubmit`.

### Task 3: Admin Shell

**Files:**
- Create: `bjyo-admin/src/main/resources/static/bjyo/css/bjyo-shell.css`
- Modify: `bjyo-admin/src/main/resources/templates/index.html`
- Modify: `bjyo-admin/src/main/resources/templates/index-topnav.html`

- [x] **Step 1: Add shell CSS**

Style the sidebar, logo, user panel, topbar, tabs, and active states with dark surfaces and teal accents.

- [x] **Step 2: Include shell CSS**

Link `bjyo-shell.css` after existing BJYO UI CSS in both shell templates.

### Task 4: Workbench

**Files:**
- Create: `bjyo-admin/src/main/resources/static/bjyo/css/bjyo-workbench.css`
- Modify: `bjyo-admin/src/main/resources/templates/main.html`

- [x] **Step 1: Replace intro page with workbench**

Build a dashboard with hero summary, KPI cards, module cards, technology stack, and recent updates.

- [x] **Step 2: Preserve Thymeleaf version display**

Continue displaying `[[${version}]]`.

### Task 5: Verification

**Files:**
- No source edits expected.

- [x] **Step 1: Build**

Run: `mvn clean package -DskipTests`

Expected: `BUILD SUCCESS`.

- [x] **Step 2: Resource scan**

Run: `rg -n "bjyo-auth.css|bjyo-shell.css|bjyo-workbench.css|bjyo-ops-bg.png" bjyo-admin/src/main/resources`

Expected: templates and CSS reference the new assets.
