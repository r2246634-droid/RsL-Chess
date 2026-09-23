# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

No Maven/Gradle — build is done with direct `javac`/`java` commands. JavaFX SDK path is hardcoded to `C:\Users\Ruslan\Desktop\javafx-sdk-26.0.1\lib`.

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

**En passant** is transient, session-only state kept on `GameController.enPassantTarget` (the square a pawn jumped over on the immediately preceding move, or `null`). It is **not** tracked through the AI's search tree (`ChessAI` always passes `null` for it) — an accepted simplification, since it rarely matters at shallow search depths.

**`Piece.copy(Coordinate pos)`** — all pieces implement this abstract method, and must copy the `moved` flag too. Used by `Board.copy()` to create deep copies for AI tree search / legal-move simulation without mutating the live board.

**Thread safety for AI:** `GameController.triggerAIMove()` runs AI on a daemon thread. It calls `board.copy()` before handing off to the thread, so the AI never touches the live board. Results are posted back via `uiExecutor` (set to `Platform::runLater` by `ChessBoardUI`). `GameController` has no JavaFX dependency — the `Consumer<Runnable> uiExecutor` field decouples it. `GameTimer`'s countdown fields are `volatile` since they're written on its own daemon thread and read from the JavaFX thread via `onTick`.

**`ChessBoardUI` piece layer:** piece glyphs are **not** children of the 64 grid `StackPane`s. They live in a separate always-on-top `pieceLayer` `Pane`, tracked by object identity in `Map<Piece, StackPane> pieceNodes` / `Map<Piece, Coordinate> pieceCoords`. `refreshPieces()` diffs the current `Board` against that state each time it's called (after every move): a piece whose reference is still on the board but at a new square slides there (`TranslateTransition`); a piece whose reference disappeared (captured, or replaced by promotion) fades out; a brand-new reference (initial setup, promotion result, or a full rebuild after a theme switch) pops in. This depends on `Board.setPiece()` preserving `Piece` object identity across an ordinary move — only promotion and capture actually discard/replace a reference. The 64 grid cells still separately hold background fill, hover highlight, selection overlay, move-hint markers, and the check ring — none of that changed.

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
