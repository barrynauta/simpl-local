import { useState, useEffect, useCallback, useRef } from 'react';
import {
  handleLoginCallback,
  createLoginURL,
  refreshAccessToken,
  buildLogoutUrl,
  LOGOUT_DELETE_COOKIES_QUERY_PARAM,
} from '../keycloak';
import { isAuthenticationEnabled } from '../authConfig';

const REFRESH_TIMEOUT_MARGIN = 30; // seconds

const generateCodeVerifier = () => {
  const possible =
    'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~';
  const randomValues = new Uint8Array(43);
  globalThis.crypto.getRandomValues(randomValues);
  return Array.from(randomValues)
    .map(val => possible[val % possible.length])
    .join('');
};

export const useAuth = () => {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [loading, setLoading] = useState(true);
  const hasRun = useRef(false);

  const getFullRedirectUri = useCallback(() => 'http://localhost:3001/', []);

  const clearSession = useCallback(() => {
    sessionStorage.removeItem('access_token');
    sessionStorage.removeItem('refresh_token');
    sessionStorage.removeItem('id_token');
    sessionStorage.removeItem('expires_at');
    sessionStorage.removeItem('refresh_expires_at');
    setIsAuthenticated(false);
  }, []);

  const redirectToLogin = useCallback(async () => {
    const pkceCodeVerifier = generateCodeVerifier();
    localStorage.setItem('pkceCodeVerifier', pkceCodeVerifier);
    const loginUrl = await createLoginURL(
      getFullRedirectUri(),
      pkceCodeVerifier
    );
    globalThis.location.href = loginUrl;
  }, [getFullRedirectUri]);

  const logout = useCallback(() => {
    // Auth disabled (VITE_PUBLIC_AUTH_MODE): no external IdP to sign out from.
    if (!isAuthenticationEnabled()) return;

    const idToken = sessionStorage.getItem('id_token');
    const logoutUrl = buildLogoutUrl(
      getFullRedirectUri(),
      idToken || undefined
    );
    clearSession();
    globalThis.location.href = logoutUrl;
  }, [clearSession, getFullRedirectUri]);

  const handleLogoutParam = useCallback(
    (logoutParam: string | null) => {
      if (!logoutParam) return false;
      clearSession();
      globalThis.location.href = globalThis.location.origin;
      return true;
    },
    [clearSession]
  );

  const handleAuthCodeFlow = useCallback(
    async (sessionExists: boolean) => {
      if (sessionExists) {
        setIsAuthenticated(true);
        setLoading(false);
        return true;
      }

      const tokens = await handleLoginCallback(getFullRedirectUri());
      if (!tokens) {
        clearSession();
        redirectToLogin();
        return false;
      }

      const {
        access_token,
        refresh_token,
        id_token,
        expires_in,
        refresh_expires_in,
      } = tokens;
      sessionStorage.setItem('access_token', access_token);
      sessionStorage.setItem('refresh_token', refresh_token);
      sessionStorage.setItem('id_token', id_token);
      sessionStorage.setItem(
        'expires_at',
        (Date.now() + expires_in * 1000).toString()
      );
      sessionStorage.setItem(
        'refresh_expires_at',
        (Date.now() + refresh_expires_in * 1000).toString()
      );
      setIsAuthenticated(true);
      globalThis.history.replaceState(
        {},
        document.title,
        globalThis.location.pathname
      );
      return true;
    },
    [clearSession, redirectToLogin]
  );

  const handleSessionFlow = useCallback(async () => {
    const expiresAt = sessionStorage.getItem('expires_at');
    if (
      expiresAt &&
      Date.now() > parseInt(expiresAt) - REFRESH_TIMEOUT_MARGIN * 1000
    ) {
      try {
        const newTokens = await refreshAccessToken(
          sessionStorage.getItem('refresh_token') as string,
          getFullRedirectUri()
        );
        sessionStorage.setItem('access_token', newTokens.access_token);
        sessionStorage.setItem(
          'expires_at',
          (Date.now() + newTokens.expires_in * 1000).toString()
        );
        setIsAuthenticated(true);
      } catch (error) {
        clearSession();
        redirectToLogin();
      }
    } else {
      setIsAuthenticated(true);
    }
  }, [clearSession, getFullRedirectUri, redirectToLogin]);

  const processAuthFlow = useCallback(async () => {
    if (hasRun.current) return;
    hasRun.current = true;

    // ── Auth disabled via configuration (VITE_PUBLIC_AUTH_MODE=disabled) ──
    // Run with a static local identity: mark authenticated, skip Keycloak
    // entirely (no redirect, no token exchange). This is the configurable
    // bypass for local evaluation.
    if (!isAuthenticationEnabled()) {
      setIsAuthenticated(true);
      setLoading(false);
      return;
    }

    setLoading(true);
    try {
      const urlHasAuthCode = globalThis.location.search.includes('code=');
      const sessionExists = !!sessionStorage.getItem('access_token');
      const logoutParam = new URLSearchParams(globalThis.location.search).get(
        LOGOUT_DELETE_COOKIES_QUERY_PARAM
      );

      if (handleLogoutParam(logoutParam)) return;

      if (urlHasAuthCode) {
        await handleAuthCodeFlow(sessionExists);
      } else if (sessionExists) {
        await handleSessionFlow();
      } else {
        redirectToLogin();
      }
    } catch (error) {
      console.error('Authentication error:', error);
      clearSession();
    } finally {
      setLoading(false);
    }
  }, [
    clearSession,
    handleAuthCodeFlow,
    handleLogoutParam,
    handleSessionFlow,
    redirectToLogin,
  ]);

  useEffect(() => {
    processAuthFlow();
  }, [processAuthFlow]);

  return { isAuthenticated, loading, logout };
};
