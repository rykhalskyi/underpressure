---
name: implementation-planner
description: A streamlined 2-phase workflow (Research & Plan -> Task Generation) for decomposing GitHub issues into atomic coding tasks. Aligns with .design-specs/ documents.
---

# implementation-planner

## Overview
This skill guides a structured process to analyze GitHub issues and generate a technical implementation plan followed by atomic, agent-ready tasks. **Do not implement code; only generate and save .md specification files.**

## PHASE 1: Analysis & Implementation Plan
1.  **Issue Retrieval**: Fetch the issue details for the provided issue number from repo `rykhalskyi/underpressure` (or as specified) using `mcp_github_issue_read`.
2.  **Codebase Research**: 
    - Use `grep_search` or `glob` to find existing implementations of similar features or patterns.
    - Identify exact files, interfaces, and utilities that must be leveraged.
3.  **Steering Alignment**: Briefly review relevant sections of `.design-specs/` (`tech.md`, `codestyle.md`, `structure.md`). *Optimization: Use targeted reads if files are large.*
4.  **Draft Plan**: Enter **Plan Mode** to draft an implementation plan in `.design-specs/specs/plan-{issue_number}.md`.
    - **Schema**:
      - **Overview**: Goal and value.
      - **Steering**: Key alignments with tech/style/structure.
      - **Impact**: Files to create/modify, data/API changes.
      - **Strategy**: Step-by-step logic and patterns.
      - **Verification**: Testing strategy (Unit/E2E).

## PHASE 2: Atomic Task Generation
1.  **Generate Tasks**: Immediately following the plan (while context is fresh), generate atomic tasks in `.design-specs/specs/tasks-{issue_number}.md`. **Do not re-read the plan file if already in context.**
2.  **Task Requirements**:
    - **Atomic**: 1-3 files per task, ~20 mins work.
    - **Structure**:
      - `[ ] {Task Number}. {Title}`
      - `File`: Path
      - `Description`: Clear "what" and "why".
      - `_Leverage_`: Existing files/patterns to use.
      - `_Prompt_`: A self-contained, high-signal instruction for an agent (Role, Task, Restrictions, Success Criteria).
3.  **State Management**: If the session is interrupted, check for existing `plan-{issue_number}.md` or `tasks-{issue_number}.md` before restarting from Phase 1.
4.  **Cleanup**: Exit Plan Mode and notify the user of the generated files.

## Token Efficiency Tips
- **Consolidate Turns**: Perform research and planning in one continuous flow.
- **Surgical Reads**: Don't read whole steering docs; use `grep_search` for specific keywords (e.g., "database", "ui") relevant to the issue.
- **No Boilerplate**: Omit generic examples in generated files; focus on the specific issue.
