# 単体テスト仕様書

| 項目 | 内容 |
|------|------|
| 文書番号 | UT-SPEC-001 |
| バージョン | 1.0.0 |
| 作成日 | 2026-04-26 |
| 対象システム | 家族スケジュール共有システム（Spring Boot 3.3.4） |
| 作成者 | テストリーダー指示に基づき自動生成 |
| ステータス | ドラフト |

---

## 目次

1. [テスト方針](#1-テスト方針)
2. [テスト観点一覧](#2-テスト観点一覧)
3. [テストケース群](#3-テストケース群)
   - 3.1 [ScheduleValidator テストケース（UT-V-01〜UT-V-08）](#31-schedulevalidator-テストケース)
   - 3.2 [ScheduleService テストケース（UT-S-01〜UT-S-12）](#32-scheduleservice-テストケース)
   - 3.3 [MemberService テストケース（UT-M-01〜UT-M-12）](#33-memberservice-テストケース)
4. [状態遷移マトリクス](#4-状態遷移マトリクス)
5. [既知バグと対応テストケース](#5-既知バグと対応テストケース)
6. [実行方法まとめ](#6-実行方法まとめ)

---

## 1. テスト方針

### 1.1 目的

本仕様書は、家族スケジュール共有システムの以下3クラスに対する単体テストを定義する。
テストを通じてビジネスロジックの正確性を検証し、既知バグの再現・修正確認を可能にすることを目的とする。

- `ScheduleValidator`：入力バリデーションロジック
- `ScheduleService`：スケジュールのCRUDおよび状態遷移ロジック
- `MemberService`：メンバー管理ロジック

### 1.2 スコープ

| 対象 | 内容 |
|------|------|
| テスト種別 | 単体テスト（JUnit 5 + Mockito） |
| テスト対象 | ScheduleValidator、ScheduleService、MemberService |
| 除外事項 | データベース結合テスト（リポジトリ層）、UIテスト、APIエンドポイントテスト |
| モック対象 | ScheduleRepository、MemberRepository（Mockitoでスタブ化） |

### 1.3 合格基準

- 優先度 H のテストケースが全件 PASS すること
- カバレッジ：対象クラスのステートメントカバレッジ 80% 以上
- 既知バグ（BUG-VALIDATOR）が再現できること（= 修正前はテストが FAIL する）

### 1.4 除外事項

- 外部サービス連携テスト
- パフォーマンステスト
- セキュリティテスト（別途スコープ）

### 1.5 実行コマンド（全体）

```bash
# 全単体テスト実行
mvn test

# クラス単位で実行
mvn test -Dtest=ScheduleValidatorTest
mvn test -Dtest=ScheduleServiceTest
mvn test -Dtest=MemberServiceTest

# レポート生成（Surefireレポート）
mvn surefire-report:report
```

---

## 2. テスト観点一覧

| 観点ID | 観点名 | 説明 | 優先度 |
|--------|--------|------|--------|
| V-NML | 正常系バリデーション | 正常な入力値でエラーが発生しないこと | H |
| V-NULL | null・空値 | null や空文字でバリデーションエラーが適切に発生すること | H |
| V-BOUNDARY | 境界値 | コードポイント数の境界（100/101）で仕様通りの動作をすること | H |
| V-SURROGATE | サロゲートペア | 絵文字等のサロゲートペア文字がコードポイント単位で正しく計算されること | H |
| V-MULTI | 複数エラー | 複数フィールドが同時に不正な場合、全エラーが返ること | H |
| V-MEMBER | メンバーID範囲 | 有効メンバーID（1〜5）のハードコード仕様および越境値のテスト | H |
| S-CRUD | スケジュールCRUD | create/update/delete/restore/purge が正常に実行されること | H |
| S-SOFTDELETE | 論理削除 | delete が物理削除ではなく deleted_at をセットすること | H |
| S-NOTFOUND | 存在しないID | 存在しないまたは削除済みIDへの操作で NotFoundException が発生すること | H |
| S-STRIP | トリム処理 | content の前後スペースが除去されること | H |
| S-RANGE | 期間バリデーション | findRange で from > to、または null 引数の場合に ValidationException が発生すること | H |
| M-CRUD | メンバーCRUD | create/rename/delete が正常に実行されること | H |
| M-VALIDATION | メンバーバリデーション | 名前空白・長さ超過・上限超過・重複でそれぞれ ValidationException が発生すること | H |
| M-RENAME | 改名特殊ケース | 自分と同じ名前への改名は重複チェックをスキップして許可されること | H |
| M-LENGTHIMPL | 文字列長実装差異 | MemberService は `String.length()` を使用しており、ScheduleValidator の `codePointCount` と異なる点を確認すること | H |

---

## 3. テストケース群

### 3.1 ScheduleValidator テストケース

> テスト対象クラス: `ScheduleValidator`
> テストクラス: `ScheduleValidatorTest`

---

#### UT-V-01 正常値（全フィールド有効）

| 項目 | 内容 |
|------|------|
| テストケースID | UT-V-01 |
| テスト対象 | `ScheduleValidator.validate()` |
| 前提条件 | なし |
| 優先度 | H |

| ステップ | 操作・入力 | 期待結果 |
|---------|-----------|---------|
| 1 | `validate(memberId=1, date=2026-04-26, content="塾")` を呼び出す | `errors` マップが空（エラーなし） |
| 2 | `validate(memberId=3, date=2026-04-26, content="A")` を呼び出す | `errors` マップが空（エラーなし） |
| 3 | `validate(memberId=5, date=2026-04-26, content="1")` を呼び出す | `errors` マップが空（エラーなし） |

実行コマンド:
```bash
mvn test -Dtest=ScheduleValidatorTest#正常値全フィールド有効
```

---

#### UT-V-02 memberId 範囲外（BUG-VALIDATOR 確認）

| 項目 | 内容 |
|------|------|
| テストケースID | UT-V-02 |
| テスト対象 | `ScheduleValidator.validate()` |
| 前提条件 | なし |
| 優先度 | H |

> **注意: これはバグ確認テストです。**
> `VALID_MEMBER_IDS = Set.of(1, 2, 3, 4, 5)` がソースコードにハードコードされており、
> メンバーが追加された場合に自動的に対応しない設計上のバグ（BUG-VALIDATOR）が存在する。
> 本テストでは `memberId=6` がエラーになることを「再現テスト」として検証する。
> 修正対応については [5. 既知バグと対応テストケース](#5-既知バグと対応テストケース) を参照のこと。

| ステップ | 操作・入力 | 期待結果 |
|---------|-----------|---------|
| 1 | `validate(memberId=6, date=2026-04-26, content="塾")` を呼び出す | `errors.get("memberId")` = `"不正なメンバーです"` |
| 2 | `validate(memberId=0, date=2026-04-26, content="塾")` を呼び出す | `errors.get("memberId")` = `"不正なメンバーです"` |
| 3 | `validate(memberId=-1, date=2026-04-26, content="塾")` を呼び出す | `errors.get("memberId")` = `"不正なメンバーです"` |
| 4 | （バグ再現確認）`validate(memberId=6, ...)` が **現状ではエラーになること**を確認する。修正後はDBから動的にメンバーIDを取得する実装に変更し、本テストを「memberId=6 が有効な場合はエラーにならない」よう更新する | BUG-VALIDATOR が存在する間は PASS、修正後は仕様変更に合わせて期待値を更新すること |

実行コマンド:
```bash
mvn test -Dtest=ScheduleValidatorTest#memberIdBugValidator
```

---

#### UT-V-03 content 空・空白のみ

| 項目 | 内容 |
|------|------|
| テストケースID | UT-V-03 |
| テスト対象 | `ScheduleValidator.validate()` |
| 前提条件 | なし |
| 優先度 | H |

| ステップ | 操作・入力 | 期待結果 |
|---------|-----------|---------|
| 1 | `validate(memberId=1, date=2026-04-26, content="")` を呼び出す | `errors.get("content")` = `"内容を入力してください"` |
| 2 | `validate(memberId=1, date=2026-04-26, content="   ")` を呼び出す（半角スペースのみ） | `errors.get("content")` = `"内容を入力してください"` |
| 3 | `validate(memberId=1, date=2026-04-26, content="\t\n")` を呼び出す（タブ・改行のみ） | `errors.get("content")` = `"内容を入力してください"` |
| 4 | `validate(memberId=1, date=2026-04-26, content=null)` を呼び出す | `errors.get("content")` = `"内容を入力してください"` |

実行コマンド:
```bash
mvn test -Dtest=ScheduleValidatorTest#content空白のみ
```

---

#### UT-V-04 content 境界値（100コードポイント / 101コードポイント）

| 項目 | 内容 |
|------|------|
| テストケースID | UT-V-04 |
| テスト対象 | `ScheduleValidator.validate()` |
| 前提条件 | なし |
| 優先度 | H |

| ステップ | 操作・入力 | 期待結果 |
|---------|-----------|---------|
| 1 | `validate(memberId=1, date=2026-04-26, content="あ"×100)` を呼び出す | `errors` マップが空（エラーなし） |
| 2 | `validate(memberId=1, date=2026-04-26, content="あ"×101)` を呼び出す | `errors.get("content")` = `"内容は100文字以内で入力してください"` |
| 3 | `validate(memberId=1, date=2026-04-26, content="a"×100)` を呼び出す（ASCII 100文字） | `errors` マップが空（エラーなし） |
| 4 | `validate(memberId=1, date=2026-04-26, content="a"×101)` を呼び出す（ASCII 101文字） | `errors.get("content")` = `"内容は100文字以内で入力してください"` |

実行コマンド:
```bash
mvn test -Dtest=ScheduleValidatorTest#content境界値
```

---

#### UT-V-05 絵文字（サロゲートペア）100コードポイント

| 項目 | 内容 |
|------|------|
| テストケースID | UT-V-05 |
| テスト対象 | `ScheduleValidator.validate()`、`ScheduleValidator.codePointLength()` |
| 前提条件 | なし |
| 優先度 | H |

> **補足:** 絵文字（例: 😀）はUTF-16でサロゲートペア（2 char）として表現されるため、
> `String.length()` では 2 とカウントされるが、`codePointCount` では 1 とカウントされる。
> `ScheduleValidator` は `codePointLength()` を使用しているため、絵文字 100個は 100コードポイントとして正しくカウントされる必要がある。

| ステップ | 操作・入力 | 期待結果 |
|---------|-----------|---------|
| 1 | `codePointLength("😀".repeat(100))` を直接呼び出す | 戻り値 = `100` |
| 2 | `codePointLength("😀".repeat(101))` を直接呼び出す | 戻り値 = `101` |
| 3 | `validate(memberId=1, date=2026-04-26, content="😀".repeat(100))` を呼び出す（絵文字100個） | `errors` マップが空（エラーなし） |
| 4 | `validate(memberId=1, date=2026-04-26, content="😀".repeat(101))` を呼び出す（絵文字101個） | `errors.get("content")` = `"内容は100文字以内で入力してください"` |
| 5 | `"😀".length()` の値を確認する | `2`（1コードポイントが2charで表現されることを確認） |

実行コマンド:
```bash
mvn test -Dtest=ScheduleValidatorTest#絵文字サロゲートペア100コードポイント
```

---

#### UT-V-06 date=null

| 項目 | 内容 |
|------|------|
| テストケースID | UT-V-06 |
| テスト対象 | `ScheduleValidator.validate()` |
| 前提条件 | なし |
| 優先度 | H |

| ステップ | 操作・入力 | 期待結果 |
|---------|-----------|---------|
| 1 | `validate(memberId=1, date=null, content="塾")` を呼び出す | `errors.get("date")` = `"日付を入力してください"` |
| 2 | 上記のとき、他のフィールド（memberId、content）はエラーなしであること | `errors` に `"date"` キーのみ存在する |

実行コマンド:
```bash
mvn test -Dtest=ScheduleValidatorTest#dateNull
```

---

#### UT-V-07 req=null（リクエスト自体がnull）

| 項目 | 内容 |
|------|------|
| テストケースID | UT-V-07 |
| テスト対象 | `ScheduleValidator.validate()` |
| 前提条件 | なし |
| 優先度 | H |

| ステップ | 操作・入力 | 期待結果 |
|---------|-----------|---------|
| 1 | `validate(null)` を呼び出す | `errors.get("_")` = `"リクエストが空です"` |
| 2 | 上記のとき、メソッドが例外をスローせずに `errors` マップを返すこと | NullPointerException が発生しない |
| 3 | 上記のとき、`errors` マップのサイズが 1 であること（`_` キーのみ） | `errors.size()` = `1` |

実行コマンド:
```bash
mvn test -Dtest=ScheduleValidatorTest#requestNull
```

---

#### UT-V-08 複数フィールド同時エラー

| 項目 | 内容 |
|------|------|
| テストケースID | UT-V-08 |
| テスト対象 | `ScheduleValidator.validate()` |
| 前提条件 | なし |
| 優先度 | H |

| ステップ | 操作・入力 | 期待結果 |
|---------|-----------|---------|
| 1 | `validate(memberId=null, date=null, content="")` を呼び出す | `errors` に `"memberId"`・`"date"`・`"content"` の3キーが存在する |
| 2 | 各エラーメッセージを確認する | `errors.get("memberId")` = `"誰を選んでください"`、`errors.get("date")` = `"日付を入力してください"`、`errors.get("content")` = `"内容を入力してください"` |
| 3 | `validate(memberId=6, date=null, content=null)` を呼び出す | `errors` に `"memberId"`・`"date"`・`"content"` の3キーが存在する |

実行コマンド:
```bash
mvn test -Dtest=ScheduleValidatorTest#複数フィールド同時エラー
```

---

### 3.2 ScheduleService テストケース

> テスト対象クラス: `ScheduleService`
> テストクラス: `ScheduleServiceTest`
> モック: `ScheduleRepository`（Mockitoを使用）

---

#### UT-S-01 create 正常（save が呼ばれる）

| 項目 | 内容 |
|------|------|
| テストケースID | UT-S-01 |
| テスト対象 | `ScheduleService.create()` |
| 前提条件 | `ScheduleRepository` をモック化する |
| 優先度 | H |

| ステップ | 操作・入力 | 期待結果 |
|---------|-----------|---------|
| 1 | `create(memberId=1, date=2026-04-26, content="塾")` を呼び出す | バリデーションエラーが発生しない |
| 2 | 上記の操作後、`repository.save()` が 1 回呼ばれていることを確認する | `verify(repository, times(1)).save(any())` が通る |
| 3 | 戻り値（保存されたエンティティ）が null でないことを確認する | 戻り値 != null |

実行コマンド:
```bash
mvn test -Dtest=ScheduleServiceTest#create正常
```

---

#### UT-S-02 content 前後スペースのstrip（"  塾  " → "塾"）

| 項目 | 内容 |
|------|------|
| テストケースID | UT-S-02 |
| テスト対象 | `ScheduleService.create()` |
| 前提条件 | `ScheduleRepository` をモック化する |
| 優先度 | H |

| ステップ | 操作・入力 | 期待結果 |
|---------|-----------|---------|
| 1 | `create(memberId=1, date=2026-04-26, content="  塾  ")` を呼び出す | バリデーションエラーが発生しない |
| 2 | `repository.save()` に渡されたエンティティの content を確認する | エンティティの content = `"塾"`（前後スペースが除去されている） |
| 3 | `create(memberId=1, date=2026-04-26, content="\t学校\n")` を呼び出す | エンティティの content = `"学校"`（タブ・改行も除去される） |

実行コマンド:
```bash
mvn test -Dtest=ScheduleServiceTest#contentStripWhitespace
```

---

#### UT-S-03 create バリデーション NG → save されない

| 項目 | 内容 |
|------|------|
| テストケースID | UT-S-03 |
| テスト対象 | `ScheduleService.create()` |
| 前提条件 | `ScheduleRepository` をモック化する |
| 優先度 | H |

| ステップ | 操作・入力 | 期待結果 |
|---------|-----------|---------|
| 1 | `create(memberId=null, date=2026-04-26, content="塾")` を呼び出す | `ValidationException`（または同等の例外）がスローされる |
| 2 | 上記の操作後、`repository.save()` が呼ばれていないことを確認する | `verify(repository, never()).save(any())` が通る |
| 3 | `create(memberId=1, date=null, content="")` を呼び出す | 例外がスローされ、`repository.save()` が呼ばれない |

実行コマンド:
```bash
mvn test -Dtest=ScheduleServiceTest#createValidationNgSaveNotCalled
```

---

#### UT-S-04 findRange from > to → ValidationException

| 項目 | 内容 |
|------|------|
| テストケースID | UT-S-04 |
| テスト対象 | `ScheduleService.findRange()` |
| 前提条件 | なし |
| 優先度 | H |

| ステップ | 操作・入力 | 期待結果 |
|---------|-----------|---------|
| 1 | `findRange(from=2026-05-01, to=2026-04-01)` を呼び出す（from が to より後） | `ValidationException` がスローされる |
| 2 | `findRange(from=2026-04-26, to=2026-04-26)` を呼び出す（from = to、同日） | 例外がスローされず、結果リストが返る（同日検索は有効） |

実行コマンド:
```bash
mvn test -Dtest=ScheduleServiceTest#findRangeFromAfterTo
```

---

#### UT-S-05 findRange null 引数 → ValidationException

| 項目 | 内容 |
|------|------|
| テストケースID | UT-S-05 |
| テスト対象 | `ScheduleService.findRange()` |
| 前提条件 | なし |
| 優先度 | H |

| ステップ | 操作・入力 | 期待結果 |
|---------|-----------|---------|
| 1 | `findRange(from=null, to=2026-04-30)` を呼び出す | `ValidationException` がスローされる |
| 2 | `findRange(from=2026-04-01, to=null)` を呼び出す | `ValidationException` がスローされる |
| 3 | `findRange(from=null, to=null)` を呼び出す | `ValidationException` がスローされる |

実行コマンド:
```bash
mvn test -Dtest=ScheduleServiceTest#findRangeNullArgument
```

---

#### UT-S-06 delete → softDelete() が呼ばれる（物理削除されない）

| 項目 | 内容 |
|------|------|
| テストケースID | UT-S-06 |
| テスト対象 | `ScheduleService.delete()` |
| 前提条件 | `ScheduleRepository` をモック化し、ID=1 のエンティティが存在する（`deleted_at=null`）状態にスタブ化する |
| 優先度 | H |

| ステップ | 操作・入力 | 期待結果 |
|---------|-----------|---------|
| 1 | `delete(id=1)` を呼び出す | 例外がスローされない |
| 2 | エンティティの `softDelete()` が呼ばれ、`deleted_at` がセットされていることを確認する | エンティティの `deletedAt` が null でない |
| 3 | `repository.delete(entity)` が呼ばれていないことを確認する（物理削除されない） | `verify(repository, never()).delete(any())` が通る |
| 4 | `repository.save(entity)` が呼ばれていることを確認する（論理削除の保存） | `verify(repository, times(1)).save(any())` が通る |

実行コマンド:
```bash
mvn test -Dtest=ScheduleServiceTest#deleteSoftDeleteCalled
```

---

#### UT-S-07 delete 存在しないID → NotFoundException

| 項目 | 内容 |
|------|------|
| テストケースID | UT-S-07 |
| テスト対象 | `ScheduleService.delete()` |
| 前提条件 | `repository.findByIdAndDeletedAtIsNull(999)` が空の Optional を返すようにスタブ化する |
| 優先度 | H |

| ステップ | 操作・入力 | 期待結果 |
|---------|-----------|---------|
| 1 | `delete(id=999)` を呼び出す（存在しないID） | `NotFoundException` がスローされる |
| 2 | 上記の操作後、`repository.delete()` が呼ばれていないことを確認する | `verify(repository, never()).delete(any())` が通る |

実行コマンド:
```bash
mvn test -Dtest=ScheduleServiceTest#deleteNotFoundId
```

---

#### UT-S-08 restore 正常

| 項目 | 内容 |
|------|------|
| テストケースID | UT-S-08 |
| テスト対象 | `ScheduleService.restore()` |
| 前提条件 | `repository.findByIdAndDeletedAtIsNotNull(1)` が削除済みエンティティを返すようにスタブ化する |
| 優先度 | H |

| ステップ | 操作・入力 | 期待結果 |
|---------|-----------|---------|
| 1 | `restore(id=1)` を呼び出す（削除済みのエンティティ） | 例外がスローされない |
| 2 | エンティティの `restore()` が呼ばれ、`deleted_at` が null に戻ることを確認する | エンティティの `deletedAt` = null |
| 3 | `repository.save(entity)` が呼ばれていることを確認する | `verify(repository, times(1)).save(any())` が通る |

実行コマンド:
```bash
mvn test -Dtest=ScheduleServiceTest#restore正常
```

---

#### UT-S-09 restore 未削除ID → NotFoundException

| 項目 | 内容 |
|------|------|
| テストケースID | UT-S-09 |
| テスト対象 | `ScheduleService.restore()` |
| 前提条件 | `repository.findByIdAndDeletedAtIsNotNull(1)` が空の Optional を返すようにスタブ化する（= 削除されていないID） |
| 優先度 | H |

| ステップ | 操作・入力 | 期待結果 |
|---------|-----------|---------|
| 1 | `restore(id=1)` を呼び出す（削除されていないIDに対してrestore） | `NotFoundException` がスローされる |
| 2 | 上記の操作後、`repository.save()` が呼ばれていないことを確認する | `verify(repository, never()).save(any())` が通る |

実行コマンド:
```bash
mvn test -Dtest=ScheduleServiceTest#restoreNotDeletedId
```

---

#### UT-S-10 purge 正常（repository.delete() が呼ばれる）

| 項目 | 内容 |
|------|------|
| テストケースID | UT-S-10 |
| テスト対象 | `ScheduleService.purge()` |
| 前提条件 | `repository.findByIdAndDeletedAtIsNotNull(1)` が削除済みエンティティを返すようにスタブ化する |
| 優先度 | H |

| ステップ | 操作・入力 | 期待結果 |
|---------|-----------|---------|
| 1 | `purge(id=1)` を呼び出す（削除済みエンティティの物理削除） | 例外がスローされない |
| 2 | `repository.delete(entity)` が 1 回呼ばれていることを確認する | `verify(repository, times(1)).delete(any())` が通る |
| 3 | `repository.save()` が呼ばれていないことを確認する（purgeはsaveしない） | `verify(repository, never()).save(any())` が通る |

実行コマンド:
```bash
mvn test -Dtest=ScheduleServiceTest#purge正常
```

---

#### UT-S-11 purge 存在しないID → NotFoundException

| 項目 | 内容 |
|------|------|
| テストケースID | UT-S-11 |
| テスト対象 | `ScheduleService.purge()` |
| 前提条件 | `repository.findByIdAndDeletedAtIsNotNull(999)` が空の Optional を返すようにスタブ化する |
| 優先度 | H |

| ステップ | 操作・入力 | 期待結果 |
|---------|-----------|---------|
| 1 | `purge(id=999)` を呼び出す（削除済みリストに存在しないID） | `NotFoundException` がスローされる |
| 2 | 上記の操作後、`repository.delete()` が呼ばれていないことを確認する | `verify(repository, never()).delete(any())` が通る |

実行コマンド:
```bash
mvn test -Dtest=ScheduleServiceTest#purgeNotFoundId
```

---

#### UT-S-12 update 削除済みID → NotFoundException

| 項目 | 内容 |
|------|------|
| テストケースID | UT-S-12 |
| テスト対象 | `ScheduleService.update()` |
| 前提条件 | `repository.findByIdAndDeletedAtIsNull(1)` が空の Optional を返すようにスタブ化する（= IDが削除済み） |
| 優先度 | H |

| ステップ | 操作・入力 | 期待結果 |
|---------|-----------|---------|
| 1 | バリデーション通過する入力値でリクエストを作成する（`memberId=1, date=2026-04-26, content="塾"`） | バリデーションエラーなし |
| 2 | `update(id=1, request)` を呼び出す（削除済みIDへの更新） | `NotFoundException` がスローされる |
| 3 | 上記の操作後、`repository.save()` が呼ばれていないことを確認する | `verify(repository, never()).save(any())` が通る |

実行コマンド:
```bash
mvn test -Dtest=ScheduleServiceTest#updateDeletedId
```

---

### 3.3 MemberService テストケース

> テスト対象クラス: `MemberService`
> テストクラス: `MemberServiceTest`
> モック: `MemberRepository`（Mockitoを使用）

---

#### UT-M-01 create 正常

| 項目 | 内容 |
|------|------|
| テストケースID | UT-M-01 |
| テスト対象 | `MemberService.create()` |
| 前提条件 | `repository.count()` = 0、`repository.existsByName("太郎")` = false |
| 優先度 | H |

| ステップ | 操作・入力 | 期待結果 |
|---------|-----------|---------|
| 1 | `create("太郎")` を呼び出す | `ValidationException` がスローされない |
| 2 | `repository.save()` が 1 回呼ばれていることを確認する | `verify(repository, times(1)).save(any())` が通る |

実行コマンド:
```bash
mvn test -Dtest=MemberServiceTest#create正常
```

---

#### UT-M-02 create 名前が空白 → ValidationException

| 項目 | 内容 |
|------|------|
| テストケースID | UT-M-02 |
| テスト対象 | `MemberService.create()` |
| 前提条件 | なし |
| 優先度 | H |

| ステップ | 操作・入力 | 期待結果 |
|---------|-----------|---------|
| 1 | `create("")` を呼び出す（空文字） | `ValidationException` がスローされる |
| 2 | `create("   ")` を呼び出す（空白のみ） | `ValidationException` がスローされる |
| 3 | `create("\t")` を呼び出す（タブのみ） | `ValidationException` がスローされる |
| 4 | `repository.save()` が呼ばれていないことを確認する | `verify(repository, never()).save(any())` が通る |

実行コマンド:
```bash
mvn test -Dtest=MemberServiceTest#createNameBlank
```

---

#### UT-M-03 create 名前が20文字超 → ValidationException

| 項目 | 内容 |
|------|------|
| テストケースID | UT-M-03 |
| テスト対象 | `MemberService.create()` |
| 前提条件 | `repository.count()` = 0 |
| 優先度 | H |

> **実装差異注記:**
> `MemberService` は名前の長さを `name.length()` で判定している。
> これは Java の `String.length()` であり、サロゲートペア（絵文字等）は 2 としてカウントされる。
> 一方、`ScheduleValidator` は `codePointCount` を使用しており、同じ絵文字は 1 としてカウントされる。
> この差異により、絵文字を含む名前では両者の長さ判定が異なる点に注意すること。
> 例: `"😀".repeat(10)` は `String.length()` では 20 になるが、`codePointCount` では 10 になる。
> 本テストでは `String.length()` に基づく動作を検証する。

| ステップ | 操作・入力 | 期待結果 |
|---------|-----------|---------|
| 1 | `create("a".repeat(20))` を呼び出す（ASCII 20文字、`length()` = 20） | `ValidationException` がスローされない（境界値:ちょうどOK） |
| 2 | `create("a".repeat(21))` を呼び出す（ASCII 21文字、`length()` = 21） | `ValidationException` がスローされる |
| 3 | `create("あ".repeat(20))` を呼び出す（全角 20文字、`length()` = 20） | `ValidationException` がスローされない |
| 4 | `create("あ".repeat(21))` を呼び出す（全角 21文字、`length()` = 21） | `ValidationException` がスローされる |
| 5 | `create("😀".repeat(10))` を呼び出す（絵文字 10個、`length()` = 20） | `ValidationException` がスローされない（`length()` = 20 で判定） |
| 6 | `create("😀".repeat(11))` を呼び出す（絵文字 11個、`length()` = 22） | `ValidationException` がスローされる（`length()` = 22 で制限超過） |

実行コマンド:
```bash
mvn test -Dtest=MemberServiceTest#createNameTooLong
```

---

#### UT-M-04 create メンバー上限（10人）超 → ValidationException

| 項目 | 内容 |
|------|------|
| テストケースID | UT-M-04 |
| テスト対象 | `MemberService.create()` |
| 前提条件 | `repository.count()` が返す値をスタブ化する |
| 優先度 | H |

| ステップ | 操作・入力 | 期待結果 |
|---------|-----------|---------|
| 1 | `repository.count()` = 9 の状態で `create("太郎")` を呼び出す | `ValidationException` がスローされない（9人 + 1人 = 10人でOK） |
| 2 | `repository.count()` = 10 の状態で `create("次郎")` を呼び出す | `ValidationException` がスローされる（上限超過） |
| 3 | `repository.count()` = 11 の状態で `create("三郎")` を呼び出す | `ValidationException` がスローされる |

実行コマンド:
```bash
mvn test -Dtest=MemberServiceTest#createMemberCountLimit
```

---

#### UT-M-05 create 名前重複 → ValidationException

| 項目 | 内容 |
|------|------|
| テストケースID | UT-M-05 |
| テスト対象 | `MemberService.create()` |
| 前提条件 | `repository.count()` = 1、`repository.existsByName("太郎")` = true |
| 優先度 | H |

| ステップ | 操作・入力 | 期待結果 |
|---------|-----------|---------|
| 1 | `create("太郎")` を呼び出す（既存名と重複） | `ValidationException` がスローされる |
| 2 | `repository.save()` が呼ばれていないことを確認する | `verify(repository, never()).save(any())` が通る |

実行コマンド:
```bash
mvn test -Dtest=MemberServiceTest#createDuplicateName
```

---

#### UT-M-06 delete 正常

| 項目 | 内容 |
|------|------|
| テストケースID | UT-M-06 |
| テスト対象 | `MemberService.delete()` |
| 前提条件 | ID=1 のメンバーが存在する状態にスタブ化する |
| 優先度 | H |

| ステップ | 操作・入力 | 期待結果 |
|---------|-----------|---------|
| 1 | `delete(id=1)` を呼び出す | 例外がスローされない |
| 2 | `repository.delete()` または `repository.deleteById()` が呼ばれていることを確認する | 削除メソッドが 1 回呼ばれる |

実行コマンド:
```bash
mvn test -Dtest=MemberServiceTest#delete正常
```

---

#### UT-M-07 delete 存在しないID → NotFoundException

| 項目 | 内容 |
|------|------|
| テストケースID | UT-M-07 |
| テスト対象 | `MemberService.delete()` |
| 前提条件 | ID=999 のメンバーが存在しない状態にスタブ化する |
| 優先度 | H |

| ステップ | 操作・入力 | 期待結果 |
|---------|-----------|---------|
| 1 | `delete(id=999)` を呼び出す | `NotFoundException` がスローされる |
| 2 | 削除メソッドが呼ばれていないことを確認する | `verify(repository, never()).delete(any())` が通る |

実行コマンド:
```bash
mvn test -Dtest=MemberServiceTest#deleteNotFoundId
```

---

#### UT-M-08 rename 正常

| 項目 | 内容 |
|------|------|
| テストケースID | UT-M-08 |
| テスト対象 | `MemberService.rename()` |
| 前提条件 | ID=1 のメンバー（名前="太郎"）が存在し、`repository.existsByName("次郎")` = false |
| 優先度 | H |

| ステップ | 操作・入力 | 期待結果 |
|---------|-----------|---------|
| 1 | `rename(id=1, newName="次郎")` を呼び出す | 例外がスローされない |
| 2 | `repository.save()` が呼ばれていることを確認する | `verify(repository, times(1)).save(any())` が通る |
| 3 | 保存されたエンティティの名前が "次郎" になっていることを確認する | エンティティの `name` = `"次郎"` |

実行コマンド:
```bash
mvn test -Dtest=MemberServiceTest#rename正常
```

---

#### UT-M-09 rename 自分と同じ名前への改名は許可（重複チェックをスキップ）

| 項目 | 内容 |
|------|------|
| テストケースID | UT-M-09 |
| テスト対象 | `MemberService.rename()` |
| 前提条件 | ID=1 のメンバー（名前="太郎"）が存在し、`repository.existsByName("太郎")` = true |
| 優先度 | H |

> **仕様説明:**
> `rename` メソッドは `!m.getName().equals(name)` の条件で重複チェックを行う。
> これにより、改名先の名前が現在の名前と同じ場合、重複チェックをスキップして
> `ValidationException` をスローしない（同名への改名は「変更なし」として扱う）。

| ステップ | 操作・入力 | 期待結果 |
|---------|-----------|---------|
| 1 | `rename(id=1, newName="太郎")` を呼び出す（同名への改名） | `ValidationException` がスローされない（重複チェックスキップ） |
| 2 | `repository.existsByName("太郎")` が `true` であっても例外が発生しないことを確認する | エラーなしで処理が完了する |
| 3 | ID=2 のメンバー（名前="花子"）が既に "太郎" という名前を使用している状態で、ID=1 のメンバーが `rename(id=1, newName="太郎")` を呼び出す | `ValidationException` がスローされる（別ユーザーとの重複は許可しない） |

実行コマンド:
```bash
mvn test -Dtest=MemberServiceTest#renameSameNameSkipsDuplicateCheck
```

---

#### UT-M-10 rename 新しい名前が他メンバーと重複 → ValidationException

| 項目 | 内容 |
|------|------|
| テストケースID | UT-M-10 |
| テスト対象 | `MemberService.rename()` |
| 前提条件 | ID=1 のメンバー（名前="太郎"）が存在し、`repository.existsByName("花子")` = true（別メンバーが使用中） |
| 優先度 | H |

| ステップ | 操作・入力 | 期待結果 |
|---------|-----------|---------|
| 1 | `rename(id=1, newName="花子")` を呼び出す（別メンバーと重複） | `ValidationException` がスローされる |
| 2 | `repository.save()` が呼ばれていないことを確認する | `verify(repository, never()).save(any())` が通る |

実行コマンド:
```bash
mvn test -Dtest=MemberServiceTest#renameDuplicateWithOtherMember
```

---

#### UT-M-11 rename 空白名 → ValidationException

| 項目 | 内容 |
|------|------|
| テストケースID | UT-M-11 |
| テスト対象 | `MemberService.rename()` |
| 前提条件 | ID=1 のメンバーが存在する |
| 優先度 | H |

| ステップ | 操作・入力 | 期待結果 |
|---------|-----------|---------|
| 1 | `rename(id=1, newName="")` を呼び出す | `ValidationException` がスローされる |
| 2 | `rename(id=1, newName="   ")` を呼び出す（空白のみ） | `ValidationException` がスローされる |

実行コマンド:
```bash
mvn test -Dtest=MemberServiceTest#renameBlankName
```

---

#### UT-M-12 rename 20文字超 → ValidationException

| 項目 | 内容 |
|------|------|
| テストケースID | UT-M-12 |
| テスト対象 | `MemberService.rename()` |
| 前提条件 | ID=1 のメンバーが存在する |
| 優先度 | H |

> **実装差異注記（UT-M-03 と同様）:**
> `rename` においても名前の長さ判定は `String.length()` を使用している。
> サロゲートペア（絵文字等）は 2 としてカウントされる点に注意すること。

| ステップ | 操作・入力 | 期待結果 |
|---------|-----------|---------|
| 1 | `rename(id=1, newName="a".repeat(20))` を呼び出す | `ValidationException` がスローされない（20文字はOK） |
| 2 | `rename(id=1, newName="a".repeat(21))` を呼び出す | `ValidationException` がスローされる |
| 3 | `rename(id=1, newName="😀".repeat(11))` を呼び出す（絵文字11個, `length()` = 22） | `ValidationException` がスローされる（`length()` = 22 で制限超過） |

実行コマンド:
```bash
mvn test -Dtest=MemberServiceTest#renameTooLongName
```

---

## 4. 状態遷移マトリクス

### Schedule エンティティの状態遷移

```
active（通常）
  │
  ├─[delete]────→ deleted（deleted_at = 設定済み）
  │                  │
  │                  ├─[restore]──→ active（deleted_at = null に戻す）
  │                  │
  │                  └─[purge]───→ （エンティティ消滅：物理削除）
  │
  └─[update]──→ active（内容更新）
```

### 状態遷移テストマトリクス

| 現在の状態 | 操作 | 遷移先の状態 | テストケースID |
|-----------|------|-------------|--------------|
| active | create | active | UT-S-01 |
| active | update | active | UT-S-12（削除済みの場合の失敗パターン） |
| active | delete | deleted | UT-S-06 |
| active | restore | ※エラー（未削除のためNG） | UT-S-09 |
| active | purge | ※エラー（削除済みでないためNG） | UT-S-11 |
| deleted | update | ※エラー（NotFoundException） | UT-S-12 |
| deleted | delete | ※エラー（findByIdAndDeletedAtIsNull が空を返す） | UT-S-07 |
| deleted | restore | active | UT-S-08 |
| deleted | purge | 消滅（物理削除） | UT-S-10 |
| 存在しないID | delete | NotFoundException | UT-S-07 |
| 存在しないID | restore | NotFoundException | UT-S-09 |
| 存在しないID | purge | NotFoundException | UT-S-11 |

### 状態の定義

| 状態 | 条件 | リポジトリ取得メソッド |
|------|------|-----------------|
| active | `deleted_at IS NULL` | `findByIdAndDeletedAtIsNull(id)` |
| deleted | `deleted_at IS NOT NULL` | `findByIdAndDeletedAtIsNotNull(id)` |
| 消滅 | レコード自体が存在しない | いずれのメソッドも空 Optional を返す |

---

## 5. 既知バグと対応テストケース

### バグ一覧

| バグID | 対象クラス | 概要 | 重大度 | 対応テストケース | ステータス |
|--------|-----------|------|--------|----------------|-----------|
| BUG-VALIDATOR | `ScheduleValidator` | `VALID_MEMBER_IDS` がソースコードにハードコードされており、DBのメンバー増減に追従しない | High | UT-V-01、UT-V-02 | 未修正 |

---

### BUG-VALIDATOR 詳細

#### 概要

```java
// ScheduleValidator.java 内の問題箇所
private static final Set<Integer> VALID_MEMBER_IDS = Set.of(1, 2, 3, 4, 5); // ← ハードコードバグ
```

`VALID_MEMBER_IDS` に `{1, 2, 3, 4, 5}` がハードコードされているため、以下の問題が発生する。

#### 問題の影響

| シナリオ | 影響 |
|---------|------|
| 6人目のメンバーをDBに追加した場合 | `memberId=6` でスケジュール作成ができない（「不正なメンバーです」エラーが出る） |
| メンバーを削除した場合（例: ID=3を削除） | 削除されたメンバーID=3 でもスケジュール作成が通ってしまう |
| メンバーIDの採番が1〜5以外になる場合 | バリデーションが常に失敗する |

#### テストによる再現方法

- **UT-V-02**（ステップ1）: `memberId=6` を渡すと「不正なメンバーです」エラーが返ることを確認する
- 修正前: UT-V-02 が PASS（バグが再現できている）
- 修正後: UT-V-02 の期待値を「memberId=6 が有効なメンバーとして登録されていれば、エラーにならない」に更新する必要がある

#### 修正方針

| 項目 | 内容 |
|------|------|
| 修正方法 | `MemberRepository`（またはService）をDIし、有効なメンバーIDを動的に取得する |
| 修正後の実装イメージ | `if (!memberRepository.existsById(req.memberId()))` でチェックする |
| 注意事項 | 修正後は `ScheduleValidator` がstaticクラスからインスタンスを持つクラスに変わる可能性がある。テストも合わせてモック注入方式に変更が必要 |

---

## 6. 実行方法まとめ

### 6.1 全テスト実行

```bash
# 全単体テストを実行
mvn test

# テスト結果を確認（Surefireレポート）
mvn surefire-report:report
# レポート出力先: target/site/surefire-report.html
```

### 6.2 クラス単位での実行

```bash
# ScheduleValidator テストのみ実行
mvn test -Dtest=ScheduleValidatorTest

# ScheduleService テストのみ実行
mvn test -Dtest=ScheduleServiceTest

# MemberService テストのみ実行
mvn test -Dtest=MemberServiceTest
```

### 6.3 テストケース単位での実行

```bash
# UT-V-02: BUG-VALIDATOR 確認テスト
mvn test -Dtest=ScheduleValidatorTest#memberIdBugValidator

# UT-V-04: content 境界値テスト
mvn test -Dtest=ScheduleValidatorTest#content境界値

# UT-V-05: 絵文字サロゲートペアテスト
mvn test -Dtest=ScheduleValidatorTest#絵文字サロゲートペア100コードポイント

# UT-S-06: 論理削除テスト
mvn test -Dtest=ScheduleServiceTest#deleteSoftDeleteCalled

# UT-M-09: 自分と同じ名前への改名テスト
mvn test -Dtest=MemberServiceTest#renameSameNameSkipsDuplicateCheck
```

### 6.4 カバレッジ計測（JaCoCo使用時）

```bash
# カバレッジ計測を含めてテスト実行
mvn test jacoco:report

# レポート出力先: target/site/jacoco/index.html
```

### 6.5 テスト結果の確認ポイント

| 確認項目 | 方法 |
|---------|------|
| BUG-VALIDATOR が再現されているか | UT-V-02 が PASS していることを確認（修正前は PASS = バグ再現成功） |
| 論理削除が物理削除を行っていないか | UT-S-06 で `repository.delete()` が呼ばれていないことを確認 |
| サロゲートペアの境界値 | UT-V-05 でコードポイント単位で 100/101 が正しく判定されることを確認 |
| `String.length()` と `codePointCount` の差異 | UT-M-03 のステップ5・6で絵文字の動作を確認 |
