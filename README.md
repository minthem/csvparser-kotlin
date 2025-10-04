# csvparser-kotlin


## 📋 CSV パーサーの要件まとめ

### 1. 基本機能
- **行単位の読み込み**
    - 改行コードは `\n`, `\r\n`, `\r` すべてに対応
    - クオート内の改行はオリジナルのまま保持できるようにする
- **セル分割**
    - デリミタ（`,` や `\t`）で分割
    - クオート内のデリミタは無視
    - `""` → `"` のエスケープ処理
    - 空セル（`,,`）や末尾カンマも正しく扱う

### 2. Config 設定
- **基本設定**
    - `delimiter`（区切り文字: `,` や `\t`）
    - `quoteChar`（通常は `"`）
    - `skipRows`（先頭のスキップ行数）
    - `hasHeader`（ヘッダ行の有無）
    - `locale`（数値・日付変換用）
    - `strictMode`（変換失敗時の挙動: 例外 or デフォルト値適用）
- **lineSeparator**
    - Reader 側で自動検出（最初に見つかった改行コードを採用）
    - Writer 側で再利用して出力
- **preserveLineSeparator**
    - true の場合は 1文字ずつ読み込み、クオート内改行をオリジナル保持

### 3. 型変換
- 標準対応型: `String`, `Int`, `Long`, `Double`, `Float`, `Boolean`, `BigDecimal`, `LocalDate`, `LocalDateTime`, `Enum<T>`
- `CsvValueConverter<T>` インターフェースで拡張可能
- `@CsvColumn` アノテーションで `format` や `converter` を指定可能
- `defaultValue` → Config.defaults → null/例外 の順でフォールバック

### 4. アノテーション仕様
```kotlin
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class CsvColumn(
    val name: String = "",
    val index: Int = -1,
    val format: String = "",
    val converter: KClass<out CsvValueConverter<*>> = DefaultConverter::class,
    val defaultValue: String = ""
)
```

### 5. Row クラス
- 1行を表現するクラス
- インデックスアクセス: `row[0]`
- カラム名アクセス: `row["columnName"]`
- 元のセル文字列を保持

### 6. Reader / Writer
- **Reader**
    - `CsvCharReader`（低レベル: 1レコード文字列を構築、余りバッファ管理）
    - `CsvParser`（セル分割、クオート解除）
    - `EntityMapper`（Row → データクラス変換）
- **Writer**
    - Row/Entity を文字列化
    - Config の lineSeparator を使って出力

### 7. エラーハンドリング
- strictMode = true → 変換失敗で例外
- strictMode = false → defaultValue / Config.defaults を適用

### 8. テスト必須ポイント
- 改行コードの違い（LF, CRLF, CR）
- クオート内改行の保持
- クオート内カンマ、エスケープ `" "`
- 空セル、末尾カンマ
- EOF 直前の行（改行なし）
- strictMode の挙動

---

## 🛠 実装順序（おすすめ）
1. **CsvCharReader**（低レベル: 1文字/バッファ読み込み、余りバッファ管理）
2. **CsvParser**（セル分割、クオート解除）
3. **Row クラス**（インデックス/カラム名アクセス）
4. **CsvValueConverter 標準群**（Int, Double, LocalDate など）
5. **EntityMapper**（Row → データクラス）
6. **Writer**（Row/Entity → CSV 出力）
7. **Config**（Builder パターン、不変オブジェクト）
8. **テスト**（ライン分割・セル分割を徹底的にユニットテスト）

---

## 📦 パッケージ構成（例）
```
io.github.minthem.csvparser
├── config
│   ├── CsvConfig.kt
│   └── LineSeparator.kt
├── core
│   ├── CsvCharReader.kt
│   ├── CsvParser.kt
│   ├── Row.kt
│   └── CsvWriter.kt
├── annotation
│   └── CsvColumn.kt
├── converter
│   ├── CsvValueConverter.kt
│   ├── IntConverter.kt
│   ├── LocalDateConverter.kt
│   └── ...
├── mapping
│   └── EntityMapper.kt
└── util
    └── CsvException.kt
```

---

## 🔹 状態一覧
- **START**: セルの先頭（まだ何も読んでいない）
- **IN_FIELD**: 非クオートのセルを読み込み中
- **IN_QUOTED_FIELD**: クオートで囲まれたセルを読み込み中
- **IN_QUOTED_FIELD_QUOTE**: クオート内で `"` を読んだ直後（閉じかエスケープか判定中）

---

## 🔹 状態遷移表（エラー含む）

| 現在の状態 | 入力文字 | 遷移先 | アクション | 備考 |
|------------|----------|--------|------------|------|
| START | delimiter | START | 空セル(null)を追加 | 区切りが連続した場合 |
| START | quote | IN_QUOTED_FIELD | クオート開始 | |
| START | その他 | IN_FIELD | 文字を追加 | |
| IN_FIELD | delimiter | START | セル確定 | |
| IN_FIELD | quote | **ERROR** | - | セル途中でクオート開始は不正 |
| IN_FIELD | その他 | IN_FIELD | 文字を追加 | |
| IN_QUOTED_FIELD | quote | IN_QUOTED_FIELD_QUOTE | - | クオート候補 |
| IN_QUOTED_FIELD | その他 | IN_QUOTED_FIELD | 文字を追加 | delimiter も含めて文字扱い |
| IN_QUOTED_FIELD_QUOTE | quote | IN_QUOTED_FIELD | `"` を追加 | エスケープされたクオート |
| IN_QUOTED_FIELD_QUOTE | delimiter | START | セル確定 | 区切りとして有効 |
| IN_QUOTED_FIELD_QUOTE | 行末 | END | セル確定 | 行末で閉じる |
| IN_QUOTED_FIELD_QUOTE | その他 | **ERROR** | - | クオート閉じ後に不正文字 |
| END | - | - | - | 行末処理 |

---

## 🔹 行末処理
- **START / IN_FIELD / IN_QUOTED_FIELD_QUOTE** → セルを確定して終了
- **IN_QUOTED_FIELD** → **エラー（未閉じクオート）**

図解（状態遷移図）** に落としてみましょうか？？