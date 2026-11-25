# Login and accounts

Status: design agreed, not implemented. Code arrives in Step 2.

## Who logs in with what

| Profile | Login | Why |
|---|---|---|
| Cashier | 4-digit PIN | Speed at the counter. A Google account picker on every bill is not acceptable. |
| Admin | Sign in with Google | Real identity for the person who can change prices, void bills and run reports. |
| Admin, on a device without Google Play services | PIN | The terminal cannot do Google sign-in at all. |

A PIN fallback exists on every device, always. A till that cannot be opened is
worse than a till with a weaker login.

## The Play services problem

Sign in with Google on Android runs through Google Play services. Google's own
documentation says devices without the Play Store do not have Play services
installed, and tells developers to check for it before calling any Google API.
Android POS terminals frequently ship without it.

So the app must, at startup:

1. Ask `GoogleApiAvailability.isGooglePlayServicesAvailable(context)`.
2. If it is missing and the landing profile is Admin, show the PIN screen with a
   clear line explaining why, not a broken button.

`LoginPolicy` in `app/src/main/kotlin/com/skyplus/billdesk/auth/LoginPolicy.kt`
holds this rule and is unit tested.

## What the Google account gives us in v0.1

Identity only. The app stores the account's `sub` (stable Google user id), email
and display name locally, and stamps the audit fields on bills and stock movements
with it. Nothing is uploaded anywhere, and no bill leaves the device.

An honest limitation: without a backend, an ID token cannot be fully verified
against Google's servers. For v0.1 the account is a local identity label, not a
security boundary - the PIN is what actually protects the till. Real verification
needs a backend and is a later decision.

## Setup needed before the Google login can work

1. A Google Cloud project for SKYPLUS.
2. An **Android** OAuth client, which requires the exact package name
   (`com.skyplus.billdesk`) and the **SHA-1 fingerprint of the signing key**.
3. Separate OAuth clients for the debug key and the release key. They have
   different fingerprints and one client cannot serve both.
4. The OAuth consent screen filled in, and brand verification submitted. Google
   requires the brand to be verified before the app name appears on the consent
   screen, and approval can take days.

## The dependency this creates

The signing key must be decided before the release OAuth client is created. If the
signing key changes after release, every installed copy loses Google sign-in. This
is the same pending decision as the weather app, and it cannot be deferred past
Step 2.

## Data stored

`users` gains:

| Column | Meaning |
|---|---|
| `authMethod` | PIN or GOOGLE |
| `googleSubject` | The Google `sub` value, stable per account |
| `email` | Display only |
| `displayName` | Display only |
| `pinHash`, `pinSalt` | For PIN logins; never stored in clear |

A Google account is never trusted to grant admin by itself: the account must be on
the local allow-list of admin accounts, stored on the device.
