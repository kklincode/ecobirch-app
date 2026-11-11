#!/usr/bin/env bash
# Скрипт сборки APK через официальный Google Bubblewrap CLI
set -e
echo "========================================================"
echo "🚀 Сборка APK для RuStore (ru.ecobirch.fa.app)"
echo "========================================================"

if ! command -v bubblewrap &> /dev/null; then
    echo "📦 Установка @bubblewrap/cli..."
    npm install -g @bubblewrap/cli
fi

echo "Инициализация проекта TWA из манифеста..."
bubblewrap init --manifest="https://ais-pre-i3lhmx3lrfuttqcpomcf6q-39270860517.asia-northeast1.run.app/manifest.json"

echo "Сборка подписанного APK..."
bubblewrap build

echo "✅ Готово! Файл app-release-signed.apk готов для загрузки в RuStore Консоль."
