---
name: onlyoffice-document-editor
description: This skill enables editing of OnlyOffice DOCX/Word documents. It leverages the OnlyOffice Plugin API via 'run_code' and the Office JS API via 'run_api_code' to perform document manipulations. This skill requires the 'rag-test mcp server' to be online to function. to get plain text content of the file use 'get_content' function.
---

# Onlyoffice Document Editor

## Overview
This skill provides the capability to programmatically edit OnlyOffice DOCX/Word documents by executing JavaScript code within the OnlyOffice environment. It utilizes two main commands: `run_code` for the OnlyOffice Plugin API and `run_api_code` for the Office JS API.

## Core Capabilities

This skill enables the execution of JavaScript code snippets directly within the OnlyOffice document editor, providing powerful automation and customization options. This skills needs 'rag-test' mcp server to be connected. Documents are available by extensions. use 'extension_list' tool of the server to get all connected only office docx files

### 1. `run_code` (OnlyOffice Plugin API)

This command executes JavaScript code that interacts with the OnlyOffice Plugin API. This API allows for broader integration and control over the editor's UI and functionality.

*   **API Specification**: Refer to `references/onlyoffice-plugin-api-word.md` for a comprehensive guide on the available methods and objects.

### Example: Getting version and pasting HTML using `run_code`

To get the editor version and paste HTML content, you would provide the following JavaScript snippet to the `run_code` command:

```javascript
let version = await Editor.callMethod("GetVersion");
console.log(version);
await Editor.callMethod("PasteHtml", ["<span>Hello, </span><span><b>world</b></span><span>!</span>"]);
```

### 2. `run_api_code` (Office JS API)

This command executes JavaScript code that directly utilizes the Office JS API within the OnlyOffice API container. This API provides fine-grained control over the document's content and structure.

*   **API Specification**: Refer to `references/onlyoffice-text-document-api.md` for a comprehensive guide on the available methods and objects.
*   **Execution Context**: The code provided to `run_api_code` is executed within a `new Function()` wrapper, which includes `try...catch` for error handling:

    ```javascript
    const func = new Function(
        'try {\n' +
        '  const __result = (function() {\n' +
        '    ' + code + '\n' +
        '  })();\n' +
        '  return { success: true, result: __result === undefined ? null : __result };\n' +
        '} catch(e) {\n' +
        '  return { success: false, message: e && e.message ? e.message : String(e) };\n' +
        '}'
    );
    ```

### 3. `get_content`(get plain text content of document)
This command returns plain text content of the document. if structure of document is yet not cleat you can use 'GetFileHtml' method of plugin API.

### Example: Inserting "Hello world!" using `run_api_code`

To insert "Hello world!" into an OnlyOffice document, you would provide the following JavaScript snippet to the `run_api_code` command:

```javascript
var oDocument = Api.GetDocument();
var oParagraph = Api.CreateParagraph();
oParagraph.AddText("Hello world!");
oDocument.InsertContent([oParagraph]);
```

## Resources

This skill includes example resource directories that demonstrate how to organize different types of bundled resources:

### scripts/
Executable code that can be run directly to perform specific operations.

**Examples from other skills:**
- PDF skill: fill_fillable_fields.cjs, extract_form_field_info.cjs - utilities for PDF manipulation
- CSV skill: normalize_schema.cjs, merge_datasets.cjs - utilities for tabular data manipulation

**Appropriate for:** Node.cjs scripts (cjs), shell scripts, or any executable code that performs automation, data processing, or specific operations.

**Note:** Scripts may be executed without loading into context, but can still be read by Gemini CLI for patching or environment adjustments.

### references/
Documentation and reference material intended to be loaded into context to inform Gemini CLI's process and thinking.

**Examples from other skills:**
- Product management: communication.md, context_building.md - detailed workflow guides
- BigQuery: API reference documentation and query examples
- Finance: Schema documentation, company policies

**Appropriate for:** In-depth documentation, API references, database schemas, comprehensive guides, or any detailed information that Gemini CLI should reference while working.

### assets/
Files not intended to be loaded into context, but rather used within the output Gemini CLI produces.

**Examples from other skills:**
- Brand styling: PowerPoint template files (.pptx), logo files
- Frontend builder: HTML/React boilerplate project directories
- Typography: Font files (.ttf, .woff2)

**Appropriate for:** Templates, boilerplate code, document templates, images, icons, fonts, or any files meant to be copied or used in the final output.

---

**Any unneeded directories can be deleted.** Not every skill requires all three types of resources.
