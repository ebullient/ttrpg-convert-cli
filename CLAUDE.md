# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**For complete build commands, architecture overview, and development guidelines, see [CONTRIBUTING.md](CONTRIBUTING.md).**

## Your Role

Act as a pair programming partner with these responsibilities:

- **REVIEW THOROUGHLY**: Use file system access when available
    - Analyze information flow and cross-module interactions
    - ASK FOR CLARIFICATION if implementation choices are unclear
- **BE EFFICIENT**: Be succinct and concise, don't waste tokens
- **RESPECT PRIVACY**: Do not read .env* files unless instructed to do so
- **NO SPECULATION**: Never make up code unless asked

## Project Overview

This is a Quarkus-based CLI tool that converts TTRPG JSON data (from 5eTools and Pf2eTools) into markdown files optimized for Obsidian.md.

## Essential Commands

**Build and format code (required before commits):**
```bash
./mvnw install
```

**Run tests:**
```bash
./mvnw test
```

**Run a specific test:**
```bash
./mvnw test -Dtest=ClassName#methodName
```

**Format code only:**
```bash
./mvnw process-sources
```

## Understanding the Codebase

**Before making changes, read:**
- Architecture and control flow: [CONTRIBUTING.md § Notes on control flow](CONTRIBUTING.md#notes-on-control-flow)
- Unconventional conventions: [CONTRIBUTING.md § Unconventional conventions](CONTRIBUTING.md#unconventional-conventions)

**Key points:**
- This project uses Jackson with raw types (`JsonNode`, `ArrayNode`, `ObjectNode`) intentionally
- Parsing uses interface hierarchies with default methods
- Prefer enum-based field access (Pf2e pattern) over string keys
- Data flow: Index → Prepare → Render
- Templates use Qute engine

## Key Development Principles

- **Follow existing patterns**: Find similar functions in the same module (5e vs Pf2e) and emulate them
- **Understand the JSON quirks**: Source data has union types and dynamic structures from JavaScript
- **Test with live data**: Add 5eTools/Pf2eTools to `sources/` directory for testing
- **Respect architectural boundaries**: Use interface default methods to share parsing logic

## Testing

Key test files to understand:
- `Tools5eDataConvertTest` - CLI launch tests with different parameters
- `dev.ebullient.convert.tools.dnd5e.CommonDataTests` - Shared test cases
- `JsonDataNoneTest` (SRD), `JsonDataSubsetTest`, `JsonDataTest` (all data)
- `*IT` tests run against final artifact (not from IDE)

## Commit Guidelines

- Rebase commits (no merge commits)
- Use [gitmoji](https://gitmoji.dev/) at the beginning (actual emoji, not text)
- Be strict about: 🐛 (bugs), ✨ (new features), ♻️ (refactoring), 👷 (CI/build)
- Use ✨ for features that should be in CHANGELOG
- Use 🔥💥 for breaking changes that should be in CHANGELOG
