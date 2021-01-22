import { Component, OnInit } from '@angular/core';
import { SearchService } from '../search.service';
import { trigger, state, style, transition, animate } from '@angular/animations';
import { SearchResult } from '../domain/searchResult';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css'],
  animations : [
    // Here we are defining what are the states our panel can be in 
    // and the style each state corresponds to.
    trigger('panelState', [
      state('closed', style({ height: '32px', overflow: 'hidden' })),
      state('open', style({ height: '*' })),
      transition('closed <=> open', animate('300ms ease-in-out')),
    ]),
  ],  
})
export class HomeComponent implements OnInit {

  results: SearchResult[];
  folded = [];
  searchText: String;
  page :number = 1
  pageSize :number = 5  

  constructor(
    private searchService: SearchService,
  ) { }

  ngOnInit() {
    
  }

  search(): void {
    this.searchService.search(this.searchText).subscribe(results => {
      this.results = results;
      for (let index = 0; index < results.length; index++) {
        this.folded[index] = 'closed';
      }      
    })
  }

  toggleFold(index){
    this.folded[index] = this.folded[index] === 'open' ? 'closed' : 'open';
  }   
}
