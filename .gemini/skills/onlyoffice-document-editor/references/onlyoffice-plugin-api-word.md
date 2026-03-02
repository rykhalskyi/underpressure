# OnlyOffice Plugin API for Word Documents

This document provides a summary of the OnlyOffice Plugin API for Word documents, extracted from the `rag-test mcp server` collection "Only Office Plugin Api Word".

## Methods and their Descriptions

### FocusEditor
Returns focus to the editor.
Syntax: `expression.FocusEditor();`
Parameters: This method doesn't have any parameters.
Returns: This method doesn't return any data.
Source: https://api.onlyoffice.com/docs/plugin-and-macros/interacting-with-editors/text-document-api/Methods/FocusEditor/

### SetEditingRestrictions
Sets the document editing restrictions.
Syntax: `expression.SetEditingRestrictions(restrictions);`
Parameters:
| Name | Required/Optional | Data type | Default | Description |
|---|---|---|---|---|
| restrictions | Required | DocumentEditingRestrictions | | The document editing restrictions. |
Returns: This method doesn't return any data.
Example: `window.Asc.plugin.executeMethod("SetEditingRestrictions", ["readOnly"]);`
Source: https://api.onlyoffice.com/docs/plugin-and-macros/interacting-with-editors/text-document-api/Methods/SetEditingRestrictions/

### Api Class Methods Overview
Represents the Api class.
Methods:
| Method | Returns | Description |
|---|---|---|
| AcceptReviewChanges | None | Accepts review changes. |
| AddAddinField | None | Creates a new addin field with the data specified in the request. |
| AddComment | string | null | Adds a comment to the document. |
| AddContentControl | ContentControl | Adds an empty content control to the document. |
| AddContentControlCheckBox | None | Adds an empty content control checkbox to the document. |
Source: https://api.onlyoffice.com/docs/plugin-and-macros/interacting-with-editors/text-document-api/Methods/

### GetCurrentBookmark
Returns the current bookmark.
Syntax: `expression.GetCurrentBookmark();`
Parameters: This method doesn't have any parameters.
Returns: string | null
Source: https://api.onlyoffice.com/docs/plugin-and-macros/interacting-with-editors/text-document-api/Methods/GetCurrentBookmark/

### PasteText
Pastes text into the document.
Syntax: `expression.PasteText(text);`
Parameters:
| Name | Required/Optional | Data type | Default | Description |
|---|---|---|---|---|
| text | Required | string | | A string value that specifies the text to be pasted into the document. |
Returns: This method doesn't return any data.
Example: `window.Asc.plugin.executeMethod ("PasteText", ["ONLYOFFICE for developers"]);`
Source: https://api.onlyoffice.com/docs/plugin-and-macros/interacting-with-editors/text-document-api/Methods/PasteText/

### SetPluginsOptions
Configures plugins from an external source. The settings can be set for all plugins or for a specific plugin. For example, this method can be used to pass an authorization token to the plugin. This method can be used only with the connector class.
Syntax: `expression.SetPluginsOptions(options);`
Parameters:
| Name | Required/Optional | Data type | Default | Description |
|---|---|---|---|---|
| options | Required | PluginOption | | Plugin options. |
Source: https://api.onlyoffice.com/docs/plugin-and-macros/interacting-with-editors/text-document-api/Methods/SetPluginsOptions/

### Other Api Class Methods (Partial List) There're many other
*   `SetProperties`: Sets the properties to the document.
*   `ShowButton`: Shows or hides buttons in the header.
*   `ShowError`: Shows an error/warning message.
*   `ShowInputHelper`: Shows the input helper.
*   `StartAction`: Specifies the start action for long operations.
*   `UnShowInputHelper`: Unshows the input helper.
*   `Undo`: Undoes the user's last action.
*   `RemovePlugin`: Removes a plugin with the specified GUID.
*   `RemoveSelectedContent`: Removes the selected content from the document.
*   `ReplaceCurrentSentence`: Replaces the current sentence with the specified string.
*   `ReplaceCurrentWord`: Replaces the current word with the specified string.
*   `ReplaceTextSmart`: Replaces each paragraph (or text in cell) in the select with the corresponding text from an array of strings.
*   `SetDisplayModeInReview`: Sets the display mode for track changes.
*   `SetFormValue`: Sets a value to the specified form.
*   `SetMacros`: Sets macros to the document.
*   `CoAuthoringChatSendMessage`: Sends a message to the co-authoring chat.
*   `ConvertDocument`: Converts a document to Markdown or HTML text.
*   `EditOleObject`: Edits an OLE object in the document.
*   `EndAction`: Specifies the end action for long operations.
*   `GetAllAddinFields`: Returns all addin fields from the current document.
*   `GetAllComments`: Returns all the comments.