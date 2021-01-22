// This file can be replaced during build by using the `fileReplacements` array.
// `ng build --prod` replaces `environment.ts` with `environment.prod.ts`.
// The list of file replacements can be found in `angular.json`.

export const environment = {
  production: false,
  baseUrl: 'http://localhost:9280/api',

  authRedirectUri: 'http://localhost:4200/dashboard',
  logtoutRri: 'http://localhost:4200',
  auth: {
    clientID: 'ZWXIN07Gn5UTbFYoJYPynaJBk6IsBrJC',
    domain: 'kunzou.auth0.com',
    audience: 'https://kunzou.auth0.com/api/v2/',
    redirect: 'http://localhost:4200/callback',
    scope: 'openid'
  }    
};

/*
 * For easier debugging in development mode, you can import the following file
 * to ignore zone related error stack frames such as `zone.run`, `zoneDelegate.invokeTask`.
 *
 * This import should be commented out in production mode because it will have a negative impact
 * on performance if an error is thrown.
 */
// import 'zone.js/dist/zone-error';  // Included with Angular CLI.
