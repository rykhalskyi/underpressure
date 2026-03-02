# OnlyOffice Text Document API (Office JS API)

This document provides a summary of the OnlyOffice Text Document API, which is part of the Office JS API and is used within the OnlyOffice API container. This content is extracted from the `rag-test mcp server` collection "OnlyOffice Text Document API".

## Some Methods and their Descriptions. Use query_collection tool to find api you need

### CreateInlineLvlSdt
Creates a new inline container.
Syntax: `expression.CreateInlineLvlSdt();`
Parameters: This method doesn't have any parameters.
Returns: An `ApiInlineLvlSdt` object.
Example: This example adds an inline container to the document.
```javascript
let doc = Api.GetDocument();
let paragraph = Api.CreateParagraph();
let inlineSdt = Api.CreateInlineLvlSdt();
paragraph.AddElement(inlineSdt);
doc.InsertContent([paragraph]);
```
Source: https://api.onlyoffice.com/docs/office-api/usage-api/text-document-api/Api/Methods/CreateInlineLvlSdt/

### CreateBlockLvlSdt
Creates a new block level container.
Syntax: `expression.CreateBlockLvlSdt();`
Parameters: This method doesn't have any parameters.
Returns: An `ApiBlockLvlSdt` object.
Example: This example shows how to add a block level container to the document.
```javascript
let doc = Api.GetDocument();
let blockSdt = Api.CreateBlockLvlSdt();
blockSdt.GetContent().GetElement(0).AddText("This is a block content control.");
doc.InsertContent([blockSdt]);
```
Source: https://api.onlyoffice.com/docs/office-api/usage-api/text-document-api/Api/Methods/CreateBlockLvlSdt/

### ApiDocumentContent.ToJSON
Converts the ApiDocumentContent object into a JSON object.
Syntax: `expression.ToJSON(usedStyles);`
Parameters:
| Name | Required/Optional | Data type | Default | Description |
|---|---|---|---|---|
| usedStyles | Optional | boolean | `false` | Specifies if the used styles will be written to the JSON object or not. |
Returns: `JSON`
Example: This example converts the ApiDocumentContent object into the JSON object.
```javascript
let doc = Api.GetDocument();
let paragraph = doc.GetElement(0);
let jsonDocContent = paragraph.ToJSON(true); // with styles
console.log(jsonDocContent);
```
Source: https://api.onlyoffice.com/docs/office-api/usage-api/text-document-api/ApiDocumentContent/Methods/ToJSON/

### ApiInlineLvlSdt.GetAlias
Returns the alias attribute for the current inline container.
Syntax: `expression.GetAlias();`
Parameters: This method doesn't have any parameters.
Returns: `string`
Example: This example shows how to get the alias attribute for the container.
```javascript
let doc = Api.GetDocument();
let paragraph = doc.GetElement(0);
let inlineSdt = paragraph.GetElement(0); // Assuming the first element is an inline Sdt
if (inlineSdt && inlineSdt.GetClassType() === 'inlineLvlSdt') {
    let alias = inlineSdt.GetAlias();
    console.log("Alias: " + alias);
}
```
Source: https://api.onlyoffice.com/docs/office-api/usage-api/text-document-api/ApiInlineLvlSdt/Methods/GetAlias/

### ApiPath.GetCommands
Returns all commands of the current path.
Syntax: `expression.GetCommands();`
Parameters: This method doesn't have any parameters.
Returns: An array of command objects.
Example: Analyzes path commands of a star shape.
```javascript
// This example would typically involve a shape element.
// Assuming 'shape' is an ApiShape object
// let commands = shape.GetPath().GetCommands();
// console.log(commands);
```
Source: https://api.onlyoffice.com/docs/office-api/usage-api/text-document-api/ApiPath/Methods/GetCommands/

### ApiHyperlink.GetElementsCount
Returns the number of elements within the hyperlink.
Syntax: `expression.GetElementsCount();`
Parameters: This method doesn't have any parameters.
Returns: `number`
Example:
```javascript
let doc = Api.GetDocument();
let paragraph = Api.CreateParagraph();
let hyperlink = paragraph.AddHyperlink("https://www.onlyoffice.com", "ONLYOFFICE Website");
let elementsCount = hyperlink.GetElementsCount();
paragraph.AddText("Number of elements in hyperlink: " + elementsCount);
doc.InsertContent([paragraph]);
```
Source: https://api.onlyoffice.com/docs/office-api/usage-api/text-document-api/ApiHyperlink/Methods/GetElementsCount/

### ApiBlockLvlSdt.GetAlias
Returns the alias attribute for the current block level container.
Syntax: `expression.GetAlias();`
Parameters: This method doesn't have any parameters.
Returns: `string`
Example:
```javascript
let doc = Api.GetDocument();
let blockSdt = Api.CreateBlockLvlSdt();
blockSdt.SetAlias("MyBlockControl");
doc.InsertContent([blockSdt]);
let alias = blockSdt.GetAlias();
console.log("Block Sdt Alias: " + alias);
```
Source: https://api.onlyoffice.com/docs/office-api/usage-api/text-document-api/ApiBlockLvlSdt/Methods/GetAlias/

### ApiRun.GetClassType
Returns a type of the ApiRun class, which is "run".
Syntax: `expression.GetClassType();`
Parameters: This method doesn't have any parameters.
Returns: `"run"`
Example: This example gets a class type and inserts it into the document.
```javascript
let doc = Api.GetDocument();
let paragraph = Api.CreateParagraph();
let run = Api.CreateRun();
run.AddText("This is a run.");
paragraph.AddElement(run);
doc.InsertContent([paragraph]);
let classType = run.GetClassType();
console.log("Run Class Type: " + classType);
```
Source: https://api.onlyoffice.com/docs/office-api/usage-api/text-document-api/ApiRun/Methods/GetClassType/