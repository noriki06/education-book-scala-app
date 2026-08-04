# 命名の選定記録 — スタンプまわり

## 経緯

最初の3階層案（`StampCardType` / `StampCard` / `Stamp`）は、⑧（親子の向き）が4点で、
重く見ている軸のひとつで実質的な欠陥を抱えていた。`Stamp` という語根が最上位（`StampCardType`）と
中間（`StampCard`）の両方に現れ、しかも深い階層ほど名前が短くなる——`Cart` 系列
（`Cart` < `CartItem` < `CartItemOption`、深いほど長い）とは逆向きの並びだった。

これを解消するため、**`Stamp` を末端の1語に限定し、上位の階層は別の語根から組み立てる**方向で再検討した。
上位の語根が変われば、マスタと保有カードは別コンテキスト（common / sales）だからという理由で
評価を分けずに済む。3階層をまとめて1つの語根で読める名前を探す。

## 評価軸

APPENDIX_02 の7軸に、前回足した⑧を加えた8軸。①③④⑧を重く見る。4点以下は致命的欠点とする。

① 役割差 ② 正確さ ③ 並び ④ 業務語 ⑤ 長さ ⑥ 拡張余地 ⑦ 多重度
⑧ 親子の向き（構造上の親子と、接頭辞が示す親子が一致するか）

## 候補と評価

| マスタ / 保有カード / スタンプ | ① | ② | ③ | ④ | ⑤ | ⑥ | ⑦ | ⑧ | 総合 | 却下理由 |
|---|---|---|---|---|---|---|---|---|---|---|
| **Reward / RewardCard / RewardCardStamp** | 9 | 9 | 10 | 7 | 8 | 9 | 9 | 10 | **9** | 採用 |
| Coupon / CouponCard / CouponCardStamp | 9 | 7 | 10 | 8 | 8 | 8 | 9 | 10 | 8 | Coupon は「発行され、使ったら消える券」の語。貯めている途中（0〜9個）のカードを `CouponCard` と呼ぶのは早すぎる |
| Campaign / CampaignCard / CampaignCardStamp | 8 | 8 | 9 | 7 | 7 | 8 | 9 | 10 | 7 | Campaign は期間限定の催しを指す語。恒常的な通常スタンプまで含めると意味が広がりすぎる |
| Reward / RewardCard / RewardStamp | 7 | 9 | 10 | 7 | 9 | 9 | 9 | 7 | 7 | `Card` を省いた分は短いが、`RewardCard` の子であることが名前から読めなくなる（`Reward` 直下に見える） |
| StampCard / StampCardIssue / Stamp | 8 | 8 | 10 | 9 | 8 | 6 | 9 | 7 | 7 | `StampCard` を種別の名前に転用すると、既存の合意（会員が持つ1枚を `StampCard` と呼ぶ）と衝突する |
| StampCardType / StampCard / Stamp（当初案） | 9 | 9 | 10 | 10 | 9 | 8 | 9 | **4** | 6 | ⑧ が低い（経緯を参照） |
| StampCard / StampCardIssue / StampCardIssueStamp | 9 | 8 | 10 | 6 | 5 | 7 | 9 | 10 | 6 | `Issue`（発行）が技術寄りの語。孫の名前も長い |

## 決定打

**採用は `Reward` / `RewardCard` / `RewardCardStamp`。対抗は `Coupon` 系（総合8）。**

⑧ で満点（10）を取れる案は複数あるが、`Reward` 系が勝つ決定打は ②⑥。

**② 正確さ：`Reward` は「特典」そのものの語で、誤解の余地がない。**
`Coupon` は「発行され、使われたら消える券」を指す語。貯めている途中（まだ特典に届いていない）カードを
`CouponCard` と呼ぶのは早すぎる——0個の状態でも `Coupon` と呼ぶことになってしまう。
`Reward` には「発行されて消える」という含意がなく、「特典を得るためのカード」と読めるので、
貯め始めの0個でも、満杯の10個でも、同じ名前で通る。

**⑥ 拡張余地：`Reward` は誕生日特典のような、スタンプを使わない特典も同じ枠に収められる。**
マスタが `Reward`（特典の定義）である以上、必要スタンプ数・配布期間・特典の内容を持つ形は変えずに、
将来「スタンプを貯めずに条件を満たしたら発行される特典」が来ても、同じ `Reward` の1種として扱える。
`StampCard` 系の名前で始めていたら、マスタの名前自体がスタンプを前提にしてしまい、この余地がなかった。
03_design.md 付録B で「スタンプ以外の発行理由が来たら `Coupon` に切り替える」としていた代案は、
この採用によって不要になった——`Reward` そのものが、その拡張余地を先取りしている。

`Coupon` を採らなかったのは②が理由だが、**`Coupon` という語は空けておく。**
将来、発行されたら消える実際のクーポン券（配布して終わりのもの）が要件に入ったときのために取っておく。
`CouponCard` で確定させてしまうと、そのとき名前が使えなくなる。

**代償は④（業務語）の7点。** 会員も店舗スタッフも「スタンプカード」と呼ぶが、クラス名は `RewardCard` になる。

> 英語名は業務の言葉の直訳でなくてよいが、業務の人が聞いて納得する名前にしてください。（02_BURGER_DOMAIN）

`Reward`（特典）は業務の語彙の中にある言葉であり、`Order` を `Transaction` と呼ぶような距離ではない。
ユビキタス言語の表で「スタンプカード＝`RewardCard`」と対応づけておけば、会話とコードは繋がる（→ 02_analysis.md）。

## 名前を並べて確認する

**クラス名**
- MenuItem                 ← 既存（common）
- Reward                   ← 今回追加（common）。旧 `StampCardType`
- Cart ... Payment         ← 既存（sales）
- RewardCard               ← 今回追加（sales）。旧 `StampCard`
- RewardCardStamp          ← 今回追加（sales）。旧 `Stamp`

**テーブル名**
- common_menu_item              ← 既存
- common_reward                 ← 今回追加（13字）
- sales_cart ... sales_payment  ← 既存
- sales_reward_card             ← 今回追加（17字）
- sales_reward_card_stamp       ← 今回追加（23字）

深い階層ほど名前が長くなる：`Reward`(6) < `RewardCard`(10) < `RewardCardStamp`(15)。
`Cart`(4) < `CartItem`(8) < `CartItemOption`(14) と同じ向きになった。前回の並び逆転は解消している。

**参照・フィールド名の変更**（→ 02_analysis.md / 03_design.md に反映）
- `StampCard.typeId` → `RewardCard.rewardId`（参照先が `Reward` に変わったため）
- `Stamp.stampCardId` → `RewardCardStamp.rewardCardId`

## 判断が変わる条件

| 条件 | どうなるか |
|---|---|
| 発行されたら消える実際のクーポン券（配布して終わりのもの）が要件に入る | `Coupon` という語が使える。`Reward` の1種として持たせるか、別に並べるかを検討する |
| 業務が「スタンプカード」ではなく「リワードカード」と呼ぶようになる | ④ が10点に上がり、対抗との差がさらに開く |
| 業務が「種別」を「キャンペーン」と呼ぶようになる | `Campaign` 系を再検討する余地が生まれるが、通常スタンプ（期間限定でない）が同じ枠にある限り④は上がらない |
