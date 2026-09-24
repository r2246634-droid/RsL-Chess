# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

No Maven/Gradle — build is done with direct `javac`/`java` commands. `run.bat`/`package.bat` read the JavaFX SDK from the `JAVAFX_HOME` environment variable (its `lib` folder), falling back to `C:\Users\Ruslan\Desktop\javafx-sdk-26.0.1\lib` when it isn't set.

**Compile and run (primary script):**
```bat
run.bat
```

**Compile only (PowerShell) — must list every package, including this one:**
```powershell
$MP = "C:\Users\Ruslan\Desktop\javafx-sdk-26.0.1\lib"
javac --module-path $MP --add-modules javafx.controls,javafx.graphics -d bin `
  src\main\java\com\chess\core\*.java `
  src\main\java\com\chess\pieces\*.java `
  src\main\java\com\chess\game\*.java `
  src\main\java\com\chess\ai\*.java `
  src\main\java\com\chess\i18n\*.java `
  src\main\java\com\chess\sound\*.java `
  src\main\java\com\chess\theme\*.java `
  src\main\java\com\chess\network\*.java `
  src\main\java\com\chess\ui\*.java
```

**Run only:**
```powershell
$MP = "C:\Users\Ruslan\Desktop\javafx-sdk-26.0.1\lib"
java --module-path $MP --add-modules javafx.controls,javafx.graphics -cp bin com.chess.ui.Main
```

**Package as a Windows .exe (jpackage):**
```bat
package.bat
```

Output goes to `bin/`. There are no tests.

> **IDE false positives:** VS Code shows "javafx cannot be resolved" errors because the JavaFX module path is not configured in the IDE. These are not real errors — `run.bat` compiles correctly.

## Architecture

### Package Overview

| Package | Responsibility |
|---|---|
| `com.chess.core` | `Board` (8×8 matrix), `Coordinate`/`Move` (records), `GameConfig`, `GameTimer`, `ChessRules` (check/castling/en passant, shared by human and AI play) |
| `com.chess.pieces` | Abstract `Piece` (tracks `moved` for castling rights) + 6 concrete types |
| `com.chess.game` | `GameController` (game flow, legal-move filtering, AI trigger), `GameHistory` (writes `game_history/*.txt`) |
| `com.chess.ai` | `AIPlayer` interface, `ChessAI` (negamax + alpha-beta pruning, depth by difficulty) |
| `com.chess.i18n` | `Lang` (TR/EN/RU), `I18n` (static translation table + current-language singleton) |
| `com.chess.network` | `NetworkManager` — plain TCP, pipe-delimited line protocol for 2-player online play |
| `com.chess.sound` | `SoundEngine` — procedurally synthesized SFX (no audio assets) |
| `com.chess.theme` | `BoardTheme` (record), `Themes` (5 presets), `ThemeManager` (current selection, singleton) |
| `com.chess.ui` | `Main` → `SplashScreen` → `MainMenu` (+ `MultiplayerMenu`) → `GameScreen` + `ChessBoardUI` |

### Flow

```
Main.start()
  └─ SplashScreen.show()  → onFinished →
       MainMenu.show()    → onConfig(GameConfig) ──────────────► GameScreen.show()
                           → onMultiplayer() → MultiplayerMenu.show() → onGame(GameConfig, NetworkManager) ─┘
```

`GameScreen` owns the `GameController` and wires callbacks. `ChessBoardUI` is a pure view: it calls `controller.executeMove()` on clicks and reacts to `onBoardChanged`.

### Board Coordinate System

- `Coordinate(row, col)`: row 0 = **top** of screen = **BLACK's** back rank, row 7 = bottom = **WHITE's** back rank.
- BLACK pieces start at rows 0–1, WHITE at rows 6–7 (see `GameController.initBoard()`).
- WHITE pawns move toward **lower** row numbers (direction `-1`); BLACK toward higher (`+1`).
- `GameHistory.toAlgebraic(row, col)` converts to standard chess notation: `(0,0)` → `a8`, `(7,7)` → `h1`.

### Key Design Contracts

**`Piece.calculateMoves(Board board)`** — every piece class receives the *real* board so blocking/capture logic works, and returns **pseudo-legal, non-special** moves only (no castling, no en passant, no king-safety filtering). The caller is responsible for passing the correct board state; pieces do not store a board reference.

**`ChessRules`** (`com.chess.core`) — the single source of truth for check detection (`isInCheck`/`isSquareAttacked`), castling generation, and en passant generation/resolution. Both `GameController.getLegalMoves()` (human/network play) and `ChessAI.getAllMoves()` (search tree) call into it, so the two never diverge on what counts as legal. `GameController` additionally filters `ChessRules`' pseudo-legal+specials output by simulating each move on a `board.copy()` and rejecting any that leaves the mover's own king in check.

**Castling rights** are tracked via `Piece.isMoved()` (set by `ChessRules.finalizeMove()` after every executed move, and propagated through `Piece.copy()`), not via separate boolean flags — a king or rook that has never moved (and whose corresponding corner still holds an un-moved rook/king) is eligible.

**En passant** is transient, session-only state kept on `GameController.enPassantTarget` (the square a pawn jumped over on the immediately preceding move, or `null`). `GameController` passes it to `ChessAI.chooseMove(board, color, enPassantTarget)` for the **root** moves only (otherwise a position whose only escape from check is en passant would leave the AI with no move and freeze the game); it is **not** tracked deeper in the search tree (`negamax` passes `null`) — an accepted simplification, since it rarely matters at shallow search depths.

**50-move rule:** `GameController.halfmoveClock` counts half-moves since the last pawn move or capture (castling doesn't reset it, matching the real rule); it resets to 0 on either, increments otherwise, and hitting 100 (50 full moves) ends the game as a draw. Checkmate/stalemate are still checked first each turn, since either takes priority over this counter.

**Threefold repetition:** `GameController.positionCounts` maps a `positionKey()` string (full board layout + side to move + castling rights, computed via `canCastleRight()` from the same `Piece.isMoved()` flags — *not* full FEN, but equivalent for this purpose — + en passant target) to how many times it's occurred; the initial position is seeded in the constructor. Hitting 3 ends the game as a draw. Checked after checkmate/stalemate, before the 50-move/insufficient-material checks. Not tracked through the AI's search tree (same simplification as en passant — real per-node position history threading is real complexity most simple engines skip entirely).

**Insufficient material:** `isInsufficientMaterial()` is the common (not exhaustive-per-FIDE) simplification used by most chess platforms: K vs K, K+minor vs K, or K+B vs K+B with same-colored bishops. Any pawn/rook/queen, two minors on one side, or opposite-colored bishops all count as sufficient (even where a forced mate isn't realistically achievable, e.g. two knights) — deliberately conservative rather than trying to detect every theoretical dead position.

**AI prefers the fastest mate:** `ChessAI.negamax()`'s terminal "no legal moves" case returns `-(MATE_SCORE + depth)` (not a flat sentinel) when the side to move is checkmated, where `depth` is the *remaining* search budget at that node — so a mate found with more depth still unused (i.e. fewer plies from wherever that branch started) scores as more punishing for the mated side. Propagated back through negamax's alternating negation, this makes the search prefer delivering mate in fewer moves over a slower one, and — for free, from the same formula — prefer delaying its own mate as long as possible when it's the one losing. `MATE_SCORE` (1,000,000) is far above any realistic material evaluation so it always dominates. Note the `depth == 0` case still short-circuits straight to `evaluate()` *before* checking for legal moves, so a mate landing exactly on the search horizon isn't recognized as mate there — a standard fixed-depth-search limitation, not something this change addresses.

**Positional evaluation (`PieceSquareTables`):** `ChessAI.evaluate()` adds a per-square bonus/penalty on top of raw material (central knights/bishops, advanced pawns, rooks toward the 7th rank, a castled-corner king in the middlegame). Tables are written from "row 0 = the piece's own back rank is far away" — i.e. directly usable for WHITE at `table[row][col]`, and mirrored (`table[7-row][col]`) for BLACK. Bonuses are small (double-digit to low hundreds) relative to material (100–20000) and to `MATE_SCORE`, so they only ever break ties or nudge between similarly-valued moves, never override a material or mate difference.

**Undo:** `GameController.undo()` pops a `Snapshot` (a `board.copy()` plus turn/en-passant/halfmove-clock/moveLog/positionCounts, pushed just before every `executeMove` mutates state) and restores everything at once — simpler and less error-prone than trying to incrementally reverse each piece of bookkeeping. `undoLastMove()` calls `undo()` twice when playing against the AI (once for its reply, once for the human's move), so the human always lands back on their own turn. Undo is disabled once the game is over, and the UI (`GameScreen`) also disables it for network games and timed games — the `Snapshot` doesn't capture clock state, so undoing under a running clock would desync the displayed time from what the rule should be.

**Captured pieces:** `GameController.capturedPieces` records every captured `Piece` (including en passant) in `executeMove` and is part of the `Snapshot`, so undo restores it. `GameScreen` shows each side's captures (sorted Q/R/B/N/P, plus a `+N` material lead) in a row next to that side's timer box, refreshed from the same `updateStatus` callback as the move list — so it works identically in every mode.

**Move validation & disposal:** `executeMove` rejects any move whose piece isn't the side to move or whose target isn't in `getLegalMoves(from)` — this guards against stale UI selections (e.g. after undo) and malformed network messages. `GameController.dispose()` (called by `GameScreen`'s back-to-menu button) stops the timer and makes any late AI result / timer timeout / network event a no-op, so nothing can pop a game-over dialog after the player has left the screen. `NetworkManager.disconnect()` likewise sets a `closing` flag so a locally-initiated close is not reported as the opponent disconnecting.

**Network time control:** the host's `HELLO` carries its time control (`HELLO|name|WHITE|initialMs|incrementMs`); the client uses those values instead of its own menu selection so both clocks match.

**Resign / draw offers over network:** `GameController.resign(color)` and `.agreeDraw()` only touch local state; sending the corresponding `NetworkManager` message (`RESIGN`, `DRAW_OFFER`, `DRAW_RESPONSE`) is the caller's job (`GameScreen`, the same split already used for chat). This keeps `GameController` free of any network-message-shaped decisions — it just needs telling "the game ended this way."

**Promotion choice:** `ChessBoardUI.doMove()` detects a promoting pawn *before* calling `executeMove`, shows `PromotionDialog` (blocking, `Stage.showAndWait()`) for **local human moves only**, and passes the chosen type through `executeMove(from, to, promotionType)`. AI and remote-network moves default to `null` → Queen inside `GameController` unless the network message carries an explicit choice. The `MOVE` network message therefore carries a 5th field (`""` or `"Queen"/"Rook"/"Bishop"/"Knight"`) so both sides apply the same promoted piece.

**SAN notation:** `SanNotation.toSan()` builds everything except the trailing `+`/`#` (piece letter, disambiguation, capture `x`, destination, `=Q` for promotion, `O-O`/`O-O-O` for castling) from the board *before* the move mutates it; `GameController` appends the check/mate suffix afterward once the post-move state is known, and that's what both the live move-list panel and the saved `game_history/*.txt` file display — there's no separate "coordinate pair" format anymore, `GameController.moveLog` (SAN strings) is the single source of truth for both.

**`Piece.copy(Coordinate pos)`** — all pieces implement this abstract method, and must copy the `moved` flag too. Used by `Board.copy()` to create deep copies for AI tree search / legal-move simulation without mutating the live board.

**Thread safety for AI:** `GameController.triggerAIMove()` runs AI on a daemon thread. It calls `board.copy()` before handing off to the thread, so the AI never touches the live board. Results are posted back via `uiExecutor` (set to `Platform::runLater` by `ChessBoardUI`). `GameController` has no JavaFX dependency — the `Consumer<Runnable> uiExecutor` field decouples it. `GameTimer`'s countdown fields are `volatile` since they're written on its own daemon thread and read from the JavaFX thread via `onTick`.

**`ChessBoardUI` piece layer:** piece glyphs are **not** children of the 64 grid `StackPane`s. They live in a separate always-on-top `pieceLayer` `Pane`, tracked by object identity in `Map<Piece, StackPane> pieceNodes` / `Map<Piece, Coordinate> pieceCoords`. `refreshPieces()` diffs the current `Board` against that state each time it's called (after every move): a piece whose reference is still on the board but at a new square slides there (`TranslateTransition`); a piece whose reference disappeared (captured, or replaced by promotion) fades out; a brand-new reference (initial setup, promotion result, or a full rebuild after a theme switch) pops in. This depends on `Board.setPiece()` preserving `Piece` object identity across an ordinary move — only promotion and capture actually discard/replace a reference. The 64 grid cells still separately hold background fill, hover highlight, selection overlay, move-hint markers, and the check ring — none of that changed.

**Localization (`I18n`):** every user-facing string is a lookup, `I18n.t("some.key")`, against a hardcoded `Map<String, String[]>` (index = `Lang.ordinal()`) — no `.properties`/`ResourceBundle` files, so the `javac` wildcard build never needs to copy non-`.java` resources. There is no reactive binding: screens are rebuilt from scratch on every navigation anyway (`MainMenu.show()`, `GameScreen.show()`, …), so a language change just needs to trigger a rebuild of the *current* screen to take effect — see `MainMenu`'s language row, which calls `I18n.set(lang)` then re-invokes its own scene-building method. `GameConfig.modeLabel()/timerLabel()`, `GameController`'s end-of-game messages, and `GameHistory`'s saved `.txt` output all go through `I18n` too, so switching language before/at game start localizes the whole run, including the saved log file. New user-facing text needs a key added to `I18n`'s static initializer for **all three languages** — `I18n.t()` silently falls back to returning the raw key if a language slot is missing, which is easy to miss visually, so grep the key after adding it.

### AI Depths

| Mode | Depth |
|---|---|
| Standart AI (Easy) | 1 — move list is shuffled for variety |
| Orta Zorluk (Medium) | 3 |
| Uzman (Expert) | 4 |

### Adding a New Package

When adding a new package (e.g., `com.chess.foo`), add it to the `javac` command in **both** `run.bat` and `package.bat`:
```bat
src\main\java\com\chess\foo\*.java ^
```
