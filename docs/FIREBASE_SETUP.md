# Trace Firebase setup

Firebase is configured for optional Google accounts. Project `trace-fa25c`, Android registration `com.podhod.app`, Google provider (public name Trace, support address trace.app.registration@gmail.com) and Firestore Standard `(default)` in `eur3` are ready. Spark billing remains unchanged; no Analytics was enabled.

The owner downloaded the Android configuration; package, project and web OAuth client were validated. `app/google-services.json` remains gitignored. Google Services plugin 4.5.0 applies only when this local file exists; unconfigured builds retain guest mode.

On 2026-09-28 the owner published `firestore.rules`. A fresh console tab confirmed the current published 22:23 revision matches the private per-UID rules. See FIRESTORE_RULES_TESTS.md for access checks.

The account settings UI, Credential Manager sign-in, revision transport, conflict selection and walk union are implemented. Build and unit tests pass. A read-only instrumentation check on the connected phone passed Firebase/OAuth configuration, validation of its actual saved data, compression round trip and merge validation without uploads. Google sign-in, successful cloud upload and readback are now verified on the owner's phone, including one current and three paused workouts. Cross-device/offline integration is not yet verified.

Lint is pinned separately to 9.1.1 using android.experimental.lint.version, while AGP remains 8.9.2. A full uncached lint run passes without the previous Firebase Kotlin metadata mismatch. The build script escapes the Windows SDK drive separator for Java properties.

## Android app registration

- App nickname: Trace Android
- Package: `com.podhod.app`
- Current distributed APK signing certificate SHA-1: `CC:8F:59:EB:23:59:DF:A0:95:40:E4:CD:28:3D:F3:1E:7F:FB:04:64`
- SHA-256: `33:90:61:80:25:C7:B7:39:1B:F5:3B:B7:2D:D6:CF:1F:0B:11:1E:53:22:A7:FE:AD:C3:CF:54:31:DC:03:E5:45`

These are public certificate fingerprints, not private keys. Recheck them with `scripts/build.ps1 -Tasks signingReport` when changing signing configuration. Register the Play App Signing certificate separately if distributing through Google Play later.

## Console checklist

1. Create the owner's Trace project after the owner accepts Firebase Terms. Do not enable billing or optional Analytics as part of account setup.
2. Register the Android app with its signing fingerprints.
3. Enable Google in Authentication after the owner supplies the public support email.
4. Download the updated Android configuration after enabling Google sign-in so OAuth client information is included.
5. Set up Firestore with private-by-default access. Deploy and test per-UID rules before any user data upload; do not use public test-mode rules.
6. Implement and test the revision/conflict protocol described in `ACCOUNT_SYNC.md` before enabling production sync.

Never embed service-account private keys or admin credentials in the APK. Account setup alone does not upload user data. The in-app sign-in copy explains that an existing account is loaded automatically and an empty account receives phone data.

## Official implementation references

- [Google sign-in on Android with Firebase and Credential Manager](https://firebase.google.com/docs/auth/android/google-signin)
- [Firestore security](https://firebase.google.com/docs/firestore/security/overview)
- [Firestore transactions](https://firebase.google.com/docs/firestore/manage-data/transactions)
- [Firestore limits](https://firebase.google.com/docs/firestore/quotas)
