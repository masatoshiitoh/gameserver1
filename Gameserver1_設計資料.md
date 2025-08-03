# Gameserver1 設計資料

## 1. プロジェクト概要

Gameserver1は、ゲームバックエンドサービス用のREST APIサーバーとWebクライアントで構成されるプロジェクトです。

**主要構成要素:**
- **APIサーバー** (`apiserver/`): Vert.x基盤のREST APIサーバー
- **テストクライアント** (`testclient/`): Web UIクライアント

## 2. 技術スタック

### バックエンド (APIサーバー)
- **フレームワーク**: Eclipse Vert.x 4.4.4
- **言語**: Java 17
- **データベース**: H2 (インメモリ)
- **認証**: JWT (JSON Web Token)
- **ビルドツール**: Maven 3.x
- **テストフレームワーク**: JUnit 5

### フロントエンド (テストクライアント)
- **言語**: HTML5, CSS3, JavaScript (ES6+)
- **UI**: レスポンシブデザイン
- **状態管理**: localStorage

## 3. システム構成

### ディレクトリ構造
```
gameserver1/
├── apiserver/                    # バックエンドAPIサーバー
│   ├── src/main/java/           # メインソースコード
│   │   └── com/gameserver/api/  
│   │       ├── ApiServerApplication.java  # メインアプリケーション
│   │       ├── DatabaseService.java       # データベースサービス
│   │       └── JwtService.java           # JWT認証サービス
│   ├── src/test/java/           # テストコード
│   ├── pom.xml                  # Maven設定
│   └── start-server.bat         # サーバー起動スクリプト
└── testclient/                  # フロントエンドクライアント
    ├── index.html               # メインHTML
    ├── script.js                # JavaScriptロジック
    ├── styles.css               # スタイリング
    └── test-client.html         # テスト用HTML
```

## 4. 主要機能

### 4.1 認証機能
- **ユーザーログイン**: ユーザー名/パスワード認証
- **JWT トークン**: セッション管理
- **トークン有効期限**: 24時間

**実装場所**: `JwtService.java:35`, `ApiServerApplication.java:48`

### 4.2 インベントリ管理
- **アイテム表示**: ユーザーのアイテム一覧取得
- **アイテム分類**: weapon, armor, consumable, special
- **プロパティ管理**: アイテム固有の属性

**実装場所**: `DatabaseService.java:168`, `ApiServerApplication.java:128`

### 4.3 データベース機能
- **H2インメモリDB**: 開発用軽量データベース
- **テーブル**: users, inventory
- **サンプルデータ**: 自動投入

**実装場所**: `DatabaseService.java:28`

## 5. APIエンドポイント

### POST /api/login
**説明**: ユーザー認証  
**リクエスト**: `{"username": "player1", "password": "password123"}`  
**レスポンス**: `{"success": true, "userId": 1, "username": "player1", "accessToken": "jwt_token"}`

### GET /api/inventory
**説明**: インベントリ取得  
**ヘッダー**: `Authorization: Bearer <token>`  
**レスポンス**: ユーザーのアイテム一覧

## 6. アーキテクチャ設計

### 6.1 サーバーアーキテクチャ
- **非同期処理**: Vert.x Future/Promise パターン
- **レイヤー分離**: Application Layer → Service Layer → Database Layer
- **エラーハンドリング**: 統一的な例外処理

### 6.2 認証アーキテクチャ
- **ステートレス**: JWTベースの認証
- **トークン検証**: 手動実装 + Vert.x JWT Auth
- **CORS対応**: クロスオリジンリクエスト対応

### 6.3 データベース設計
```sql
-- ユーザーテーブル
users (id, username, password, created_at)

-- インベントリテーブル  
inventory (id, user_id, item_name, item_type, quantity, properties, created_at)
```

## 7. セキュリティ考慮事項

### 実装済み
- JWT認証による保護されたエンドポイント
- CORS設定
- リクエストバリデーション

### 改善点
- パスワードハッシュ化未実装 (平文保存)
- 秘密鍵のハードコーディング
- HTTPS未対応

## 8. テスト戦略

**テストカバレッジ**:
- 統合テスト: `IntegrationTest.java`
- API テスト: `AuthenticationApiTest.java`, `InventoryApiTest.java`
- サービステスト: `DatabaseServiceTest.java`, `JwtServiceTest.java`

## 9. デプロイメント

**起動方法**:
```bash
# APIサーバー起動
cd apiserver
mvn vertx:run

# または
mvn compile exec:java -Dexec.mainClass="com.gameserver.api.ApiServerApplication"
```

**サーバー情報**:
- ポート: 8080
- URL: http://localhost:8080/api

## 10. サンプルデータ

**テストユーザー**:
- player1 / password123
- player2 / password456  
- admin / admin123

**サンプルアイテム**:
- Iron Sword (weapon)
- Health Potion (consumable)
- Leather Armor (armor)
- Admin Key (special)

## 11. 今後の拡張予定

- パスワードハッシュ化の実装
- より多くのゲーム機能 (戦闘、トレード等)
- データベースの永続化
- ユーザー登録機能
- リアルタイム通信 (WebSocket)