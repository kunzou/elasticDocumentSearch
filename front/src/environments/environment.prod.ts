export const environment = {
  production: true,
  baseUrl: 'http://dev-nlas-st02.cwb.local:9280/api',
  authRedirectUri: 'http://kunzou.me/dashboard',
  logtoutRri: 'http://kunzou.me',

  auth: {
    clientID: 'ZWXIN07Gn5UTbFYoJYPynaJBk6IsBrJC',
    domain: 'kunzou.auth0.com',
    audience: 'https://kunzou.auth0.com/api/v2/',
    redirect: 'http://kunzou.me/callback',
    scope: 'openid'
  }  
};
