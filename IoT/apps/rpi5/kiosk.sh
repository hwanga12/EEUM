#!/usr/bin/env bash
set -euo pipefail

waitHtmlPath="${WAIT_HTML:-/home/a105/eeum/wait.html}"
chromeProfileDir="${USER_DATA_DIR:-/tmp/eeum-chrome}"

browserCmd=""
for candidate in chromium chromium-browser; do
  if command -v "$candidate" >/dev/null 2>&1; then
    browserCmd="$candidate"
    break
  fi
done
[[ -n "$browserCmd" ]] || exit 1

waitUrl="about:blank"
[[ -f "$waitHtmlPath" ]] && waitUrl="file://${waitHtmlPath}"

gsettings set org.gnome.desktop.a11y.applications screen-keyboard-enabled true >/dev/null 2>&1 || true

exec "$browserCmd" \
  --ozone-platform=wayland \
  --enable-features=UseOzonePlatform \
  --enable-wayland-ime \
  --wayland-text-input-version=3 \
  --app="$waitUrl" \
  --start-maximized \
  --user-data-dir="$chromeProfileDir" \
  --incognito \
  --password-store=basic \
  --use-mock-keychain \
  --no-first-run \
  --no-default-browser-check \
  --disable-infobars \
  --disable-session-crashed-bubble \
  --disable-translate \
  --disable-features=TranslateUI,CloudMessaging,TouchpadOverscrollHistoryNavigation,UseX11Platform \
  --overscroll-history-navigation=0 \
  --disable-pinch \
  --disable-background-networking \
  --disable-sync \
  --disable-component-update
