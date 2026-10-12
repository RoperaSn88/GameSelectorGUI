# GameSelectorGUI

事前にCSVへ登録したゲームを一覧表示し、起動できるアプリケーションです。

<img width="1276" height="664" alt="image" src="https://github.com/user-attachments/assets/d94d9b66-7e68-4e68-bbc8-3222480057d0" />
<img width="1278" height="685" alt="image" src="https://github.com/user-attachments/assets/86044f9f-5e16-412c-a934-06a491726303" />

> [!CAUTION]
> 対応しているOSはWindowsのみです。

---
## 使い方
Releasesからzipファイルをダウンロードし、解凍してから使用してください。

実行ファイルは2種類あります。
1. GameEditorGUI.exe
2. GameSelectorGUI.exe

---
### 1. ゲームを登録する
ゲーム一覧に表示する情報は、`GameEditorGUI.exe`を実行して編集できます。

<img width="603" height="470" alt="image" src="https://github.com/user-attachments/assets/bcf7db56-06e1-4d70-857c-f77bf190e1db" />

新しくゲームを追加する場合は、`新しくゲームを追加`から以下の情報を入力してください。
<img width="449" height="302" alt="image" src="https://github.com/user-attachments/assets/c5483c5e-1efe-4feb-b7e4-274728ba5b93" />

- ゲーム名
- exeファイルの相対パス
- アイコン画像の相対パス
- 背景画像の相対パス(任意。未指定の場合はアイコン画像を背景として使用します)
- ゲームの説明(任意)
- 操作方法(任意)

編集が完了したら、`GameEditorGUI`を閉じてください。

---
### 2. ゲームを選択する
ゲーム情報の入力が完了したら、`GameSelectorGUI.exe`を起動してください。
以下の画面のように、`GameMaster.csv`に登録したデータが反映されていることを確認してください。

<img width="1277" height="682" alt="image" src="https://github.com/user-attachments/assets/e5ad2361-5966-47b2-8a5c-e64106c70a46" />
<img width="1278" height="685" alt="image" src="https://github.com/user-attachments/assets/86044f9f-5e16-412c-a934-06a491726303" />

> [!WARNING]
> 反映されていない場合は、`GameMaster.csv`の以下の点を確認してください。
> - パスの記載ミスや、指定先のファイルが存在しないこと(パスは相対パスで記載してください)
> - ゲーム名などの入力漏れ

---
### 3. 操作方法
| キー | 操作 |
| --- | --- |
| ↑・W | 上の項目を選択 |
| ↓・S | 下の項目を選択 |
| Space・Enter | 決定 |
| Esc | 閉じる |
