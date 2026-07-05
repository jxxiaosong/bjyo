# BJYO Pages Style Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply the BJYO visual system to system, monitor, tool, generator, demo, modal, and standalone utility pages.

**Architecture:** Add one global page-level stylesheet and load it through the shared Thymeleaf `include :: header` fragment so existing pages inherit the new style without controller or template rewrites. Standalone templates that do not use the shared header receive the same stylesheet directly.

**Tech Stack:** Java Spring Boot, Thymeleaf, Bootstrap 3, existing BJYO/RuoYi static assets.

---

### Task 1: Global Page Stylesheet

**Files:**
- Create: `bjyo-admin/src/main/resources/static/bjyo/css/bjyo-pages.css`

- [x] **Step 1: Define the style contract**

Use a light workbench background, white content surfaces, deep-ink text, BJYO green primary actions, amber warning accents, and compact 8px-or-less radii for operational UI.

- [x] **Step 2: Implement reusable selectors**

Cover existing Bootstrap/RuoYi selectors used across pages: `body.gray-bg`, `.container-div`, `.select-list`, `.ibox`, `.panel`, `.table`, `.bootstrap-table`, `.form-control`, `.btn`, `.modal-content`, `.layui-layer`, `.middle-box`, `.lockscreen`, `.list-unstyled`, timeline, tabs, trees, and plugin controls.

- [x] **Step 3: Keep behavior untouched**

Only add CSS overrides. Do not rename form fields, change JavaScript hooks, or alter server-rendered data bindings.

### Task 2: Shared Template Entry

**Files:**
- Modify: `bjyo-admin/src/main/resources/templates/include.html`

- [x] **Step 1: Load the stylesheet from the common header**

Add `@{/bjyo/css/bjyo-pages.css?v=20260705}` after `bjyo-ui.css` so most system, monitor, quartz, generator, tool, and demo pages inherit the page refresh.

### Task 3: Standalone Template Entry

**Files:**
- Modify: `bjyo-admin/src/main/resources/templates/error/404.html`
- Modify: `bjyo-admin/src/main/resources/templates/error/500.html`
- Modify: `bjyo-admin/src/main/resources/templates/error/service.html`
- Modify: `bjyo-admin/src/main/resources/templates/error/unauth.html`
- Modify: `bjyo-admin/src/main/resources/templates/lock.html`
- Modify: `bjyo-admin/src/main/resources/templates/skin.html`
- Modify: `bjyo-admin/src/main/resources/templates/demo/form/timeline.html`

- [x] **Step 1: Add the same stylesheet to standalone pages**

Insert `@{/bjyo/css/bjyo-pages.css?v=20260705}` after the existing base CSS links.

- [x] **Step 2: Preserve standalone scripts and inline behavior**

Do not alter lockscreen AJAX, theme selection, error-page navigation, or timeline demo JavaScript.

### Task 4: Verification

**Files:**
- Verify: modified templates and Maven project

- [x] **Step 1: Check CSS reference coverage**

Run: `rg -n "bjyo-pages.css" bjyo-admin/src/main/resources/templates`

Expected: `include.html` plus all standalone templates show the stylesheet reference.

- [x] **Step 2: Check HTML tag balance for modified standalone templates**

Run a small parser script that counts `<html>`, `<head>`, `</head>`, `<body>`, and `</body>` for the edited standalone templates.

Expected: each modified standalone template reports one opening and closing page skeleton tag.

- [x] **Step 3: Build the project**

Run: `mvn clean package -DskipTests`

Expected: Maven exits with code 0.

- [x] **Step 4: Commit and push**

Stage only the new style, modified templates, and this plan file. Do not stage `bjyo-admin/src/main/resources/application-druid.yml` because it contains pre-existing local changes.
