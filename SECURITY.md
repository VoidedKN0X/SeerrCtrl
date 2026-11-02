# Security policy

Thank you for helping keep SeerrCtrl and its users safe.

## Reporting a problem

**Please don't report security problems in a public issue.** Report them privately instead:

1. Go to the [Security tab](https://github.com/VoidedKN0X/SeerrCtrl/security) of this repo.
2. Click **Report a vulnerability** and describe what you found, how to reproduce it, and which version of SeerrCtrl you used.

Only the maintainer can see the report. I'll reply as soon as I can, work on a fix, and credit you in the changelog if you'd like.

## Supported versions

Only the latest release gets security fixes. Please update before reporting, and check the [changelog](CHANGELOG.md) to see whether it's already fixed.

## What counts

- Your session, password, Plex or Jellyfin sign-in, or the API keys shown in the server settings leaking to anyone other than your own Seerr / Jellyseerr server
- Ways around the app lock (for example approving or declining a request, or seeing your account, without unlocking SeerrCtrl), or protected screens showing up in screenshots when they shouldn't
- Another app on the phone, a link, or a forged push being able to make SeerrCtrl do something you didn't ask for
- A server certificate you didn't approve being accepted, or a secure connection silently falling back to plain http
- Anything that lets someone run code or read files they shouldn't

Problems in Seerr, Jellyseerr or Overseerr itself should go to that project.

## How releases are protected

Every release is built from this repo by GitHub Actions and signed with the same release key. Its SHA-256 certificate fingerprint is:

```
92:08:DF:BB:6B:CD:BF:5B:AA:9C:EA:6C:5D:C6:D2:51:C2:80:46:56:35:01:51:AC:19:E8:CD:66:D6:98:3D:83
```

The [CtrlF-Droid repo](https://voidedkn0x.github.io/CtrlF-Droid/) only accepts APKs signed with this key.
