import { Injectable } from '@angular/core';
import { environment } from 'src/environments/environment';
import * as auth0 from 'auth0-js';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  auth0 = new auth0.WebAuth({
    clientID: environment.auth.clientID,
    domain: environment.auth.domain,
    responseType: 'token',
    redirectUri: environment.auth.redirect,
    audience: environment.auth.audience,
    scope: environment.auth.scope
  });

  accessToken: string;
  expiresAt: number;
  private authenticated: boolean;

  constructor(private router: Router) {
    this.getAccessToken();
  }

  login() {
    // Auth0 authorize request
    this.auth0.authorize();
  }

  handleLoginCallback() {
    // When Auth0 hash parsed, get profile
    this.auth0.parseHash((err, authResult) => {
      if (authResult && authResult.accessToken) {
        window.location.hash = '';
        this.getUserInfo(authResult);
      } else if (err) {
        console.error(`Error: ${err.error}`);
      }
      this.router.navigate(['/dashboard']);
    });
  }

  getAccessToken() {
    this.auth0.checkSession({}, (err, authResult) => {
      if (authResult && authResult.accessToken) {
        this.getUserInfo(authResult);
      }
    });
  }

  getUserInfo(authResult) {
    // Use access token to retrieve user's profile and set session
    this.auth0.client.userInfo(authResult.accessToken, (err, profile) => {
      if (profile) {
        this._setSession(authResult, profile);
      }
    });
  }

  private _setSession(authResult, profile) {
    // Save authentication data and update login status subject
    this.expiresAt = authResult.expiresIn * 1000 + Date.now();
    this.accessToken = authResult.accessToken;
    this.authenticated = true;
  }

  logout() {
    // Log out of Auth0 session
    // Ensure that returnTo URL is specified in Auth0
    // Application settings for Allowed Logout URLs
    sessionStorage.clear();
    this.auth0.logout({
      returnTo: environment.logtoutRri,
      clientID: environment.auth.clientID
    });
  }

  public isAuthenticated(): boolean {
    // Check if current date is before token
    // expiration and user is signed in locally
    return Date.now() < this.expiresAt && this.authenticated;
  }
}
