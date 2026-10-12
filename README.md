# GameSelectorGUI

事前にcsvに登録したゲームの一覧を表示・起動することができるアプリケーションです。
> [!CAUTION]
> 対応しているOSはWindowsのみです。

## 使い方
Releaseからzipをダウンロードしてください。
ダウンロードしたzipは解凍して使用してください。

実行ファイルは2種類あります。
1. GameEditorGUI.exe
2. GameSelectorGUI.exe

### 1. ゲームを登録する。
ゲーム一覧で表示したい情報は、`GameEditorGUI.exe`を実行することで編集が可能です。

<img width="603" height="470" alt="image" src="https://github.com/user-attachments/assets/bcf7db56-06e1-4d70-857c-f77bf190e1db" />

新しくゲームを追加する場合、`新しくゲームを追加`から以下の情報を入力してください。
- ゲーム名
- exeファイルの相対パス
- アイコン画像の相対パス
- 背景画像の相対パス(任意。未指定の場合はアイコン画像を背景として使用します)
- ゲームの説明(任意)
- 操作方法(任意)

編集が完了したら、`GameEditorGUI`を閉じてください。

### 2. ゲームを選択する。
ゲーム情報の入力が完了したら、`GameSelectorGUI.exe`を起動してください。
以下の画面のように、`GameMaster.csv`に書き込んだデータが反映されているのを確認してください。

<img width="1277" height="682" alt="image" src="https://github.com/user-attachments/assets/e5ad2361-5966-47b2-8a5c-e64106c70a46" />

> [!WARNING]
> 反映されてない場合、`GameMaster.csv`の以下の情報を確認してください。
> - パスの記載ミス、もしくはパスの指定先が見つからない。パスは相対パスで記載してください。
> - タイトルなどの入力忘れ

### 3. 操作方法
| キー入力 | 期待される操作 |
| --- | --- |
| ↑・W | 上選択 |
| ↓・S | 下選択 |
| スペース・エンター | 決定 | 
| Esc | 閉じる |
