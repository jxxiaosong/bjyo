# BJYO Visual Refresh Design

## Goal

Refresh BJYO's first impression by redesigning both the public authentication screens and the post-login workbench while preserving the existing Java, Thymeleaf, and JavaScript behavior.

## Direction

Use a restrained enterprise operations-center style: dark ink surfaces, clean white content, teal-green accents, and small amber status highlights. The AI-generated `bjyo-ops-bg.png` provides the authentication hero atmosphere without adding text or brand marks into the image itself.

## Scope

- Update `login.html` and `register.html` structure while preserving `signupForm`, `registerForm`, field names, captcha bindings, and existing scripts.
- Add `bjyo-auth.css` for the authentication layout.
- Replace the old introduction page in `main.html` with a workbench-style dashboard.
- Add `bjyo-workbench.css` for the workbench layout.
- Add `bjyo-shell.css` and include it in the admin shell pages to modernize sidebar/topbar surfaces without changing navigation behavior.
- Keep business controllers, services, and database behavior unchanged.

## Validation

Run `mvn clean package -DskipTests` and scan for broken resource references introduced by the redesign.
