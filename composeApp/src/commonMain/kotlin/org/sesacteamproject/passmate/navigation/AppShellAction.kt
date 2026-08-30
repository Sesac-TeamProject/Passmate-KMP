package org.sesacteamproject.passmate.navigation

sealed interface AppShellAction {
    data class SelectTab(val tab: AppTab) : AppShellAction
}
