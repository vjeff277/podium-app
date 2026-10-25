# Podium App

This is a minimal Android podcast app scaffold created for the `vjeff277/podium-app` repository.

It includes:
- user-installed/self-signed certificate trust support via a custom `X509TrustManager`
- a queue button in the player UI
- an editable queue sheet with remove actions
- automatic next-episode progression when the current episode is marked finished

## What was added
- Android app project skeleton
- Compose-based player UI
- custom trust manager for custom certs in app storage and Android system trust store
- queue/autoplay logic in `PlayerViewModel`

## Notes
The app is intentionally small and geared toward the features you asked for. You can expand it by wiring the queue to your real podcast feed and media player implementation.
