package org.sesacteamproject.passmate.ui.auth

// 시스템 브라우저로 OAuth 페이지를 연다 — Android=Custom Tabs, Desktop=기본 브라우저 (T022)
expect fun openSignInPage(url: String)
