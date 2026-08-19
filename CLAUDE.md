# education-book-scala-app

研修用の最小フルスタック雛形。`app`（SvelteKit）/ `app-api`（Play + ixias）/ `app-lib`（ドメイン + 永続化）の3モジュール。
依存の向きは **`app` →(HTTP)→ `app-api` →(sbt 依存)→ `app-lib`** の一方向。逆流させない。

## いま何をしているか

`docs/domain/` のドメイン設計を進めている段階。**実装より先に設計ドキュメントを固める**運用。
新しいドメインは 01_requirements → 02_analysis → 03_design → naming の4点セットで書く（→ `docs/domain/CLAUDE.md`）。

## 置き場所

| 層 | パス | 規約 |
|---|---|---|
| ドメインモデル | `app-lib/framework/app-core/src/main/scala/edu/<context>/model/` | 1ファイル1モデル。ファイル名＝クラス名 |
| 永続化 | `app-lib/.../edu/<context>/persistence/` | Repository。`table/` に SlickTable |
| リクエスト型 | `app-api/app/model/<context>/reads/` | |
| コントローラー | `app-api/app/controllers/<group>/` | **1コントローラー＝1エンドポイント**。クラス名は `XxxController` |
| ルート | `app-api/conf/routes` | |
| API 契約 | `etc/openapi/` | **型は仕様から生成する。手書きしない** |
| マイグレーション | `etc/database/migration/<db>/common/*.sql` | |

`<context>` は DDD の文脈。既存は `udb`（会員・認証）のみ。設計中のものは `common`（本部が管理する定義）と
`sales`（注文まわり）、そして Osasoi アプリの `event`（お誘い）と `shop`（お店）（→ どちらも `docs/domain/osasoi/`）。
**参照は `sales` → `common`、`event` → `udb`、`shop` → `udb` の一方向。`udb` は参照される側で、逆流させない。
`event` と `shop` は互いに参照しない（結合ゼロ）。**

## コマンド

| やること | どこで | コマンド |
|---|---|---|
| `app-lib` を変更した | `app-lib` の sbt シェル | `publishLocal`（**やらないと `app-api` に反映されない**） |
| マイグレーション SQL を追加した | `app-api` の sbt シェル | `migrateAll` |
| API を起動 | `app-api` の sbt シェル | `run`（:9000。コード変更は Play が自動再コンパイル） |
| OpenAPI を変更した | シェル | `./etc/openapi/build.sh` |
| フロントの型チェック | `app` | `pnpm -r check` |

MySQL は `docker compose` で起動する。JDK は Amazon Corretto 21、Node は 24、pnpm。

## モデルを書くときの型の作法

`edu/udb/model/User.scala` が唯一の実装済みサンプル。**新しいモデルはこれに合わせる。**

- 先頭に既存ファイルと同じライセンスヘッダを付ける
- `case class Xxx(...) extends EntityModel[Id]`、`object Xxx` に `type Id = Id.Repr` と `object Id extends Entity.Id[Long]`
- `id: Option[Id]` が先頭（永続化前は `None`）、`updatedAt` / `createdAt` が末尾
- 各フィールドの右に **日本語のコメントを桁揃えで** 付ける
- 区分値は `enum Status(val code: Short) extends EnumStatus[Short]`。
  **code はマイナスが失効・異常、プラスが進行中〜正常終了。100刻みで将来の状態を間に挟める**
- テーブル名は `<context>_<snake_case>`（例：`udb_user`、`common_reward`、`sales_reward_card`）。
  ただし**エンティティ名がコンテキスト名で始まる場合は重ねない**（例：`event`、`shop`。`event_event` にしない）

## 設計の判断で守っていること

- **求まるものは保存しない。** 例外は集計要求があるときだけで、そのときは代償（書き忘れ・ズレ）を決めごとに明記する
- **可変な列を履歴の根拠にしない。** 過去の事実は、書き換わらない列か追加専用の台帳で表す
- 名前は**深い階層ほど長くする**（`Cart` < `CartItem` < `CartItemOption`）。逆向きにすると階層を取り違える
- 呼称は日本語と英語を1対1で対応させる。**用語表の「言い換えない」欄に入っている語は使わない**
- 判断は必ず付録Bに残す。採用案だけでなく**却下案とその代償**も書く

## 読まなくていいもの

`target/`、`node_modules/`、`.svelte-kit/`、`app-api/target/scala-*/routes/`（生成物）。
`.gitignore` に `.claude` が入っているので、**設定を共有したいならリポジトリ直下の `CLAUDE.md` に書く**
（`.claude/CLAUDE.md` はコミットされない）。
