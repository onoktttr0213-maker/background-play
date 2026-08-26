# 正論パンチ（仮）

2人対戦カードゲーム「正論パンチ（仮）」（仕様書 Ver.0.7）の実装です。ルール仕様は
[`docs/spec.md`](docs/spec.md)、カードデータは [`docs/cards.json`](docs/cards.json) を参照してください。

このリポジトリには3つの成果物があります。

## 1. すぐ試せる Web 版（テストプレイ用）

`web/index.html` は仕様書のルールをそのまま実装した、依存ライブラリなし・単一ファイルの
Web アプリです。スマホのブラウザで開くだけで、1台の端末を2人で交互に渡して
ローカル対戦できます（オフラインでも動作します）。

このセッションでは Playwright を使って実際にゲームを最初から最後まで自動プレイし、
勝敗が正しく決まることとエラーが出ないことを確認済みです。

## 2. Android アプリ（Kotlin + Jetpack Compose）

`android/` が本体の Android Studio プロジェクトです。

- `android/core` — ルールエンジン部分。Android に依存しない純粋な Kotlin ライブラリです。
- `android/app` — Jetpack Compose 製の UI。`android/core` を利用し、1台のスマホを
  2人で交互に渡して遊ぶ「パスプレイ」方式を実装しています（手札は各プレイヤーのみが見られるよう、
  ターン開始時や攻撃・防御のコンボ選択時に「画面を渡してください」という確認画面を挟みます）。

### ビルド方法

1. [Android Studio](https://developer.android.com/studio)（Giraffe 以降）をインストール
2. `android/` フォルダを「Open」で開く
3. Gradle Sync が終わったら実機または AndroidエミュレーターでRun

> **注記:** この開発環境（サンドボックス）には Android SDK が無く、`dl.google.com` への
> ネットワークアクセスもブロックされているため、`android/app` を実際にビルド・実機確認することが
> できませんでした。標準的な Jetpack Compose の構成（AGP 8.5.2 / Kotlin 1.9.24 / Compose BOM
> 2024.06.00）に沿って実装していますが、初回ビルド時に軽微な修正が必要になる可能性があります。
> ルールエンジン（`android/core`）自体は下記の方法でこのサンドボックス内でも実際にコンパイル・
> テスト済みです。

## 3. ルールエンジンの自動テスト

`android/core` はゲームロジックのみを持つ純粋な Kotlin モジュールなので、Android SDK なしで
単体テストできます。このリポジトリでは `sandbox-test/` という最小限の Gradle ルートから
同じソース（`android/core/src`）を参照してテストを実行しています（コードの二重管理なし）。

```sh
cd sandbox-test
./../android/gradlew -p . :core:test   # もしくは単に `gradle :core:test`
```

14件のテストで以下を検証しています：カード40枚のデータ整合性、属性三すくみの判定、
準備フェーズの配札、ベンチ配置、ドローフェイズ、攻撃成功時のライフ移動、ブロック成功時の
カード温存、コンボ加算、1ターン中の同一カード二重攻撃の禁止、ライフ全損による勝敗判定、
山札切れ時の捨て札シャッフル、ベンチ回復ドロー、ターン交代。

## ディレクトリ構成

```
docs/            仕様書とカードデータ（原本）
web/index.html   すぐ遊べる単一ファイルWeb版
android/         Android Studioプロジェクト（core: ルールエンジン / app: Compose UI）
sandbox-test/    android/core を単体テストするための最小Gradleルート
```

## 既知の未確定事項

仕様書 Ver.0.7 の「未確定・今後詰める必要がある点」に記載の通り、以下は暫定値です。
`android/core/src/main/kotlin/com/logicpunch/core/GameConfig.kt`（および `web/index.html` の
`CONFIG` オブジェクト）にまとめてあるので、バランス調整はここを変更するだけで反映されます。

- 属性有利ボーナス（+1000）、コンボ上限（3枚）、役割タイプ別数値、山札40枚という枚数など
- 先手決めのミニゲームは未実装（仮に先攻/後攻をランダムでなく固定にしています）
