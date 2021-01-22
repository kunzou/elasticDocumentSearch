export interface SearchResult {
    fileName: String;
    score: Number;
    contentHighlights: String[];
    titleHighlights: String[];
    keywordsHighlights: String[];    
}