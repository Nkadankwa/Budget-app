## AccountY

AccountY is an Android budgeting app that helps you track expenses, manage budgets, and convert currencies in real time.

## Features

- **Budget management**: Create and manage budgets with limits.
- **Expense tracking**: Add and review transactions by category and tag.
- **Currency conversion**: Convert between currencies using up-to-date exchange rates.

## Technology

- **Platform**: Android (Kotlin, AndroidX, Material Components)
- **Networking**: Retrofit with Gson
- **Local storage**: Room database for caching currency rates and budgeting data

## Currency API Credit

Currency data is provided by the open source [`@fawazahmed0/currency-api`](https://github.com/fawazahmed0/currency-api), accessed via jsDelivr CDN:

`https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/`

This app does not embed any API keys or authentication tokens; it uses the public currency API endpoint only.

## Privacy and Sensitive Information

- **Network endpoints**: The app only communicates with the currency API URL shown above.
- **Local configuration**: `local.properties` contains only your local Android SDK path and no credentials.
- **Secrets**: No API keys, tokens, passwords, or other secrets are hard-coded in the source under `app/src`.

Before publishing, ensure `local.properties` is excluded from version control according to standard Android project practices.