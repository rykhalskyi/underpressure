---
name: implementation-planner
description: A 3-step workflow for creating an implementation plan and atomic tasks from a GitHub issue. Follows project steering documents from .design-specs. Use when asked to plan or implement a GitHub issue for user 'rykhalskyi'.
---

# implementation-planner

## Overview

This skill guides Gemini CLI through a structured 3-step process to plan and decompose GitHub issues into actionable tasks, ensuring alignment with project-specific steering documents.
DO NOT IMPLEMENT CODE. RESULT IS md FILE with TASKS

## STEP 1: GitHub Issue Retrieval

1.  Connect to GitHub user `rykhalskyi`.
2.  Identify the repository (default is `ragatool`). If not clear from context, ask the user.
3.  Fetch the issue details for the provided issue number using `mcp_github_issue_read`.
4.  If the issue does not exist or cannot be accessed, notify the user.

## STEP 2: Implementation Plan Creation

1.  Analyze the retrieved issue and the following steering documents from `.design-specs/`:
    -   `tech.md` (Technical standards)
    -   `codestyle.md` (Coding conventions)
    -   `structure.md` (Project organization)
2.  Use **Plan Mode** to draft a detailed implementation plan.
3.  Follow the structure defined in `assets/plan-template.md`.
4.  Switch off **Plan Mode** or let user do it.
5.  Save the resulting plan to `.design-specs/specs/plan-{issue_number}.md`.
6.  The plan MUST include technical details:
    -   Files to be created or modified.
    -   Data model changes (if any).
    -   API changes (if any).
    -   Specific logic to be implemented.

## STEP 3: Atomic Tasks Generation

1.  Load the `plan-{issue_number}.md` file generated in Step 2.
2.  Also load steering documents for extra context.
3.  Generate a list of atomic, executable coding tasks.
4.  **Template to Follow**: Use the exact structure from `.design-specs/templates/tasks-template.md`.
5.  **Task Criteria**:
    -   **File Scope**: Touching 1-3 related files maximum.
    -   **Time Boxing**: 15-30 minutes for an experienced developer.
    -   **Single Purpose**: One testable outcome per task.
    -   **Prompt-Ready**: Each task must include a detailed "Prompt" section for agentic implementation.
6.  Save the tasks to `.design-specs/specs/tasks-{issue_number}.md`.
7.  Do not implement task themselfes. Save then ONLY
