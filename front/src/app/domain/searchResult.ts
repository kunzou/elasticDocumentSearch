export interface SearchResult {
    id: String;
    fileName: String;
    score: Number;
    contentHighlights: String[];
    titleHighlights: String[];
    keywordsHighlights: String[];    
}