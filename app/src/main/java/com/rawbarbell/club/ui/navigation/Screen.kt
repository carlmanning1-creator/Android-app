package com.rawbarbell.club.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Programs : Screen("programs")
    object Maxes : Screen("maxes")
    object Journal : Screen("journal/{weekId}") {
        fun createRoute(weekId: String) = "journal/$weekId"
    }
    object ProgramDetail : Screen("program/{programId}") {
        fun createRoute(id: String) = "program/$id"
    }
    object Session : Screen("session/{dayId}/{weekId}") {
        fun createRoute(dayId: String, weekId: String) = "session/$dayId/$weekId"
    }
    object ProgramBuilder : Screen("builder")
    object ImportSheet : Screen("import")
}
