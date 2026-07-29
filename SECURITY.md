# Security Policy

## Reporting a vulnerability

**Please do not report security vulnerabilities through public GitHub issues, discussions, or pull
requests.** A public report gives anyone watching the repository a working exploit before a fix
exists.

Instead, please use GitHub's **private vulnerability reporting** for this repository: go to the
**Security** tab → **Report a vulnerability**. This opens a private advisory visible only to you
and the maintainers, where you can describe the issue and we can coordinate a fix and disclosure
timeline before anything is made public.

If private vulnerability reporting is ever unavailable for this repository, please open a regular
issue asking a maintainer to provide an alternative private contact — without including any
vulnerability details in that issue itself.

## What to include

When possible, please include:

- A description of the vulnerability and its potential impact.
- Steps to reproduce, or a proof of concept.
- The affected version/commit.

## Scope

Togetherly stores every family's data locally on their own device — there is no backend server and
no account system for this project to compromise. Relevant reports include (but aren't limited to):

- A way for one app installation to read or affect another's local data.
- A flaw in how a third-party SDK (RevenueCat, PostHog, Sentry) is integrated that could leak more
  than the documented, consent-gated data described in [docs/telemetry.md](docs/telemetry.md) and
  [docs/privacy.md](docs/privacy.md).
- A dependency with a known, exploitable vulnerability.

## Response

This is an actively-developed, early-stage project maintained by a small team — response times may
vary, but every private report will be acknowledged and investigated.
