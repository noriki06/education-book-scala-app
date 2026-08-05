# スタンプドメイン — 引き継ぎメモ

別のチャット／別の人に文脈を渡すためのまとめ。
正は `01_requirements.md` / `02_analysis.md` / `03_design.md` / `naming.md` の4点セット。
このファイルは「いまどうなっているか」と「なぜそうなったか」を1枚で読むためのもの。

---

## 1. いまの構造

エンティティは3つ。**既存モデル（`User` / `Shop` / `Order` / `MenuItem`）には属性を1つも足さない。**

| 日本語 | クラス | テーブル | コンテキスト | 役割 |
|---|---|---|---|---|
| リワード | `Reward` | `common_reward` | common | カードの中身を定める定義。本部が管理 |
| リワードカード | `RewardCard` | `sales_reward_card` | sales | 会員が持つ1枚。有効期限と状態を持つ |
| スタンプ | `RewardCardStamp` | `sales_reward_card_stamp` | sales | 1枚に押された1個。追加専用の台帳 |

多重度は `Reward` 1:* `RewardCard` 1:* `RewardCardStamp`。
`Reward` *:1 `MenuItem`、`User` 1:* `RewardCard`、`Order` 1:1 `RewardCardStamp`。
参照は `sales → common` の一方向。

## 2. エンティティモデル

```scala
case class Reward(
  id:                   Option[Id],
  name:                 String,                // 例："通常スタンプ"、"サマースタンプ"
  distributionStartAt:  Option[LocalDate],     // 配布期間の開始。None なら常に配布中
  distributionEndAt:    Option[LocalDate],     // 配布期間の終了。None なら終了日なし
  requiredStampCount:   Int,                   // 満杯になる数
  cardValidityPeriod:   Period,                // カードができた日からの有効な長さ
  bonusExtensionPeriod: Period,                // 特典の有効期限を、カードの有効期限からどれだけ延ばすか
  maxCardsPerUser:      Option[Int],           // 1人が作れる最大枚数。None なら上限なし
  menuItemId:           MenuItem.Id,           // 特典の対象商品
  bonusType:            BonusType,             // 全額無料 / 金額割引
  discountAmount:       Option[Int] = None,    // 割引額（円）
  updatedAt:            LocalDateTime = Now,
  createdAt:            LocalDateTime = Now
) extends EntityModel[Id]

  enum BonusType(val code: Short) extends EnumStatus[Short]:
    case IS_FULLY_FREE extends BonusType(code = 1)
    case IS_AMOUNT_OFF extends BonusType(code = 2)
```

```scala
case class RewardCard(
  id:          Option[Id],
  userId:      User.Id,
  rewardId:    Reward.Id,                     // できた時点で決まり、以後変えない
  expiredAt:   LocalDate,                     // できた日 ＋ 有効期間で確定。以後延長しない（再開時の加算を除く）
  state:       Status = Status.IS_COLLECTING,
  suspendedAt: Option[LocalDate] = None,      // 中断した日
  usedOrderId: Option[Order.Id] = None,
  usedAt:      Option[LocalDateTime] = None,
  updatedAt:   LocalDateTime = Now,
  createdAt:   LocalDateTime = Now
) extends EntityModel[Id]

  enum Status(val code: Short) extends EnumStatus[Short]:
    case IS_EXPIRED    extends Status(code =  -1) // 期限切れ
    case IS_COLLECTING extends Status(code = 100) // 収集中
    case IS_FILLED     extends Status(code = 200) // 特典獲得
    case IS_USED       extends Status(code = 300) // 使用済み
```

```scala
case class RewardCardStamp(
  id:           Option[Id],
  rewardCardId: RewardCard.Id,
  orderId:      Order.Id,            // 一意
  shopId:       Shop.Id,             // 集計用。判定には使わない
  updatedAt:    LocalDateTime = Now,
  createdAt:    LocalDateTime = Now  // ＝ 付与日時
) extends EntityModel[Id]
```

## 3. 期限が2つあること

1枚のカードに期限が2つある。どちらを見るかは `state` で決まる。

| state | 見るべき期限 |
|---|---|
| `IS_COLLECTING` | `expiredAt`（スタンプを押せる期限） |
| `IS_FILLED` | `expiredAt` ＋ `Reward.bonusExtensionPeriod`（特典を使える期限） |

**特典の有効期限は保存しない。** カードの `expiredAt` から計算する。

---

## 4. 主要な判断と理由

### リワードカードをエンティティにした

期限が「一律」＝カードにつき1つの値なので、スタンプ全件に分散させると重複し、全件 UPDATE が要る。
カードが持てば判定は1〜2列で終わる。
**もし要件が「スタンプごとに押した日から1年」ならカードは不要だった。** この分岐がすべての起点。

### 会員につき複数枚にした

1枚だと、期限切れの後に来店したとき期限が未来へ更新され、失効したはずのスタンプが復活する。
複数枚なら期限切れのカードを更新しないだけで済み、古いカードのスタンプには構造上たどれない。

### 使用済み日時をカードに持たせた

交換は必要数まとめての1回。スタンプに持つと同じ値が複数行に重複する。
1枚につき交換は1回なので、カードの属性に過不足なく収まる。
結果 `RewardCardStamp` から `Option` が消え、追加専用の台帳になった。

### 状態を4値の区分値にした

4値はどれも他のデータから求まる（件数・`usedAt`・`expiredAt`）。
それでも保存するのは、管理側の「特典の利用実績を集計したい」という要求のため。
**代償**：実態とズレる余地がある。とくに `IS_EXPIRED` は日次バッチで書き込むため、バッチが落ちると遅れる。
そこで **判定は `state` と `expiredAt` の両方を見る。集計は `state` だけ。** 役割を分けた。

### 消費は注文確定時、付与は受け渡し完了時

付与を支払い時にすると、受け取らずに特典だけ取れる。
消費を受け渡し時にすると、必要数を持った状態で2件注文して両方に適用できる。

### 付与日時を専用カラムにしない

スタンプは受け渡し完了の瞬間にしか作られないので、`createdAt` と必ず同じ値になる。
**代償**：レコードを作り直すと付与日時が変わる。データ移行や手動付与が発生したら `stampedAt` を分ける。

### 特典の対象商品をリワードが持つ

商品側にフラグを持たせる案もあったが、対象が複数になると半額や金額割引が商品ごとに違う額になる。
1つに決めれば特典の内容が金額で確定し、**既存の `MenuItem` に何も足さずに済む。**

### 半額を区分値にせず、金額割引で表す

対象商品が1つに決まったので「価格の半分」を金額で書ける。区分値が1つ減る。
**代償**：価格改定で半額でなくなる。改定したら `discountAmount` も見直す。
率のバリエーション（30%引きなど）が来たら割引率の列を新設して見直す。

### 発行上限を独立の属性にした

「配布期間があるかどうか」で通常と期間限定を見分ける案は採らない。導出に頼ると、
通常スタンプに期間を設定した瞬間に上限1枚として扱われてしまう。`maxCardsPerUser` として独立に持つ。

### 中断を `suspendedAt` で表し、区分値を5値にしない

中断は `IS_COLLECTING` の内側の区別。`state` は「どこまで貯まったか」を表す列のままにする。
中断日数も保存せず、再開時に `今日 - suspendedAt` で求めて `expiredAt` に足す。

---

## 5. 実装で真っ先に踏む落とし穴

型でもDB制約でも守れない。明示的にテストを書く。

1. **中断中のカード（`suspendedAt` が `Some`）を失効させてはいけない。**
   日次バッチの対象から除く。除くと中断中に期限が来て `IS_EXPIRED` になり、
   再開時に中断日数を加算する機会が永久に失われる。
2. **発行済み枚数を数えるとき `state` で絞り込んではいけない。**
   `userId` と `rewardId` が一致する `RewardCard` の全件を数える（期限切れ・使用済みも含む）。
   絞ると失効や交換のあとに期間限定の2枚目が作れてしまう。
3. **`IS_FILLED` のカードを `expiredAt` だけで判定してはいけない。**
   特典が使えるのは `expiredAt` ＋ `bonusExtensionPeriod` まで。
4. **収集中のカードを探すクエリには必ず `suspendedAt` が `None` の条件を付ける。**
   忘れると中断中のカードにスタンプが押される。
5. `RewardCardStamp` のレコードを後から作り直さない。付与日時が変わり、有効期限の起点がズレる。
6. `state` と実体は必ず一致させる。更新経路を操作関数に絞り、個別に書き換えない。

---

## 6. 命名（`naming.md` の結論）

`Reward` / `RewardCard` / `RewardCardStamp`。

**⑧ 親子の向き**という軸をレビュー指摘から追加した。3階層になると深い階層ほど名前が短くなりやすく、
`Cart` < `CartItem` < `CartItemOption` と逆向きになると階層を取り違える。
`Stamp` を語根にすると最上位と中間の両方に `Stamp` が現れるため、
**`Stamp` を末端の1語に限定し、上位2階層を別の語根から組み立てた。**

`Coupon` を採らなかったのは②（正確さ）。「発行され、使ったら消える券」の語なので、
貯めている途中のカードを `CouponCard` と呼ぶのは早すぎる。**`Coupon` という語は空けておく。**

**日本語も英語に合わせた。** 要求文にある業務語は「スタンプ」だけで、カードやその種別を
業務が何と呼ぶかは確認できていない。確認できていない語を業務語として扱わない。
用語表の「言い換えない」欄に「スタンプカード」を登録済み。

### 呼称の禁止語（過去に事故った箇所）

- `Reward` を指す語は **「リワード」だけ**。「マスタ」「種別」と書き分けない
- `RewardCard` は **「リワードカード」だけ**。「保有カード」「スタンプカード」と書かない
- ただし用語表の「言い換えない」欄そのものは禁止語を列挙する場所なので、置換してはいけない

---

## 7. いま扱っていないこと（付録A の要点）

- 有効期限が近いことのお知らせ通知
- スタンプの譲渡・合算 / 1注文で複数個の付与
- 期限切れカード・スタンプの物理削除
- 店舗ごとの発行数の集計画面（`shopId` は持つが画面は作らない）
- リワードの編集・削除（作成のみ）
- 期間限定への参加を取り消す機能
- スタンプを使わない特典（誕生日特典など）
- 発行されたら消える実際のクーポン券

## 8. 判断が変わる条件

| 条件 | どうなるか |
|---|---|
| 業務が「スタンプカード」と呼んでいると確認できる | 日本語の呼称を戻すか、対応表を持つかを検討 |
| スタンプを使わない発行理由が要件に入る | `Coupon` を新設し、`usedOrderId` / `usedAt` を移す |
| 率の割引（30%引きなど）が要件に入る | 割引率の列を新設し、`bonusType` を見直す |
| データ移行や手動付与が発生する | `createdAt` では付与日時を表せない。`stampedAt` を分ける |
| 後入れ先出し以外の再開順が必要になる | `suspendedAt` の新しい順という単純なルールでは足りなくなる（→ 03_design.md 付録B） |

（**追記：**複数の期間限定リワードの同時開催は、誕生日クーポンのような個人単位の期間限定リワードを
見据えて、**今回から許す判断に変更済み**。中断中のカードは複数枚になりうる前提で、再開順は
後入れ先出し（LIFO）とした。詳細は `03_design.md` 付録B「期間限定のリワードの同時開催を許すか、許さないか」）

---

## 9. 書き方のルール（`docs/domain/CLAUDE.md` より）

- 追加の要求が来たら、既存の記述を上書きせず **① → ② → ③ の順**に節を分けて並べる
  （①当初からある分 / ②追加で変わった分 / ③追加で加わった分）
- 唯一の例外は `01_requirements.md` の業務フロー。時系列がフローそのものなので、
  並べ替えず行末に〔追加要求〕〔追加要求で変更〕を付ける
- 「受け取った要求」「追加の要求」の引用は、用語を統一する一括置換の対象から外す
- 付録A は「今回やらないこと」。「将来やるかも」ではなく「今回やらない」と書く
- 付録B は「判断の記録」。**代償を書かない判断は、書いたことにならない**
- ER図は手書きSVG。行を増やすときは rect の height と外枠も一緒に広げ、
  `03_design.md` 側の代替テキストも直す
- `_sample/` は初期の下書き。**参照しない**

## 10. いまの進捗

設計4点セットは書き上がっている。**実装（Scala）はまだ1行も無い。**
`Reward` を `edu/common`、`RewardCard` / `RewardCardStamp` を `edu/sales` に作るところから。
