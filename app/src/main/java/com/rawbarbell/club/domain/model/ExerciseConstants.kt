package com.rawbarbell.club.domain.model

object ExerciseConstants {

    val SPORT_TYPES = listOf("WEIGHTLIFTING", "POWERLIFTING", "SUPERTOTAL")

    val PERFORMANCE_RATINGS = listOf("VERY GOOD", "GOOD", "STANDARD", "NOT GREAT", "ROUGH")

    val EXERCISE_TYPES = listOf("PRIMARY", "SECONDARY", "TOP SET", "PRIMER")

    val WEIGHTLIFTING_CATEGORIES = listOf(
        "SQUAT", "BENCH", "DEADLIFT", "SNATCH", "CLEAN_AND_JERK",
        "BACK", "SHOULDERS", "LEGS", "ACCESSORIES"
    )

    val POWERLIFTING_CATEGORIES = listOf(
        "SQUAT", "BENCH", "DEADLIFT", "BACK", "SHOULDERS", "LEGS", "ACCESSORIES"
    )

    val PROGRESSION_MODELS = listOf(
        "STANDARD",
        "TOP STRENGTH (PEAK)",
        "TOP STRENGTH (STR)",
        "OLY",
        "OLY (STR)",
        "TOP OLY (PEAK)",
        "TOP OLY (STR)",
        "REPS",
        "2 REPS",
        "NO PROG"
    )

    val FORMATS = listOf("Per", "GYM", "MoB", "CON-C")

    val WEIGHTLIFTING_EXERCISES: Map<String, List<String>> = mapOf(
        "SQUAT" to listOf(
            "Back Squat",
            "Front Squat",
            "Overhead Squat (OHS)",
            "Paused Back Squat",
            "Paused Front Squat",
            "Tempo Back Squat",
            "Bulgarian Split Squat",
            "Goblet Squat",
            "Raised Heel Goblet Squat",
            "Box Squat",
            "Snatch Balance",
            "Jerk Balance"
        ),
        "BENCH" to listOf(
            "Bench Press",
            "Strict Press",
            "Push Press",
            "Jerk",
            "Split Jerk",
            "Power Jerk",
            "Paused Push Press",
            "Machine OH Press",
            "Dumbbell Bench Press"
        ),
        "DEADLIFT" to listOf(
            "Clean Deadlift",
            "Snatch Deadlift",
            "Conventional Deadlift",
            "Romanian Deadlift",
            "Dumbbell RDL",
            "Block Clean Pull",
            "Block Snatch Pull",
            "Low Block Clean Pull",
            "Floating Snatch Deadlift",
            "3 Height Snatch Pull",
            "Good Mornings",
            "Good Morning with Pause"
        ),
        "SNATCH" to listOf(
            "Snatch",
            "Power Snatch",
            "Hang Snatch",
            "Low Hang Snatch",
            "Below Knee Hang Snatch",
            "Hip Snatch",
            "Snatch Pull",
            "3 Position Snatch",
            "Pos 1 Snatch",
            "Snatch Grip Sotts Press",
            "Overhead Squat",
            "Snatch Grip Push Press"
        ),
        "CLEAN_AND_JERK" to listOf(
            "Clean & Jerk",
            "Power Clean + Push Press",
            "Clean + Front Squat + Jerk",
            "Clean + 2 Jerks",
            "Hang Clean",
            "Below Knee Hang Clean",
            "Hip Clean",
            "Clean Pull",
            "Split Jerk",
            "Power Jerk",
            "Jerk in Split",
            "Jerk Recovery",
            "Push Press + Jerk",
            "Front Squat + Jerk"
        ),
        "BACK" to listOf(
            "Barbell Bent Over Row",
            "Cable Machine Face Pull",
            "Chest Supported Dumbbell Row",
            "Good Mornings with Pause",
            "Prone Thoracic Extensions",
            "Ring Row",
            "Lat Pulldown",
            "Seated Cable Row"
        ),
        "SHOULDERS" to listOf(
            "Barbell Bent Over Row",
            "Dumbbell Front Raise",
            "Ring Row",
            "Lateral Raise",
            "Arnold Press",
            "Cable Lateral Raise"
        ),
        "LEGS" to listOf(
            "Hollow Hold",
            "Leg Press",
            "Walking Lunges",
            "Step Ups",
            "Nordic Curls",
            "Leg Curl"
        ),
        "ACCESSORIES" to listOf(
            "Good Morning",
            "Pike HS Hold",
            "Air Bike Cals",
            "Dumbbell Bench Press in Hollow",
            "V Ups",
            "Plank",
            "Dead Bug",
            "Ab Wheel",
            "Farmer Carry"
        )
    )

    val POWERLIFTING_EXERCISES: Map<String, List<String>> = mapOf(
        "SQUAT" to listOf(
            "Back Squat",
            "Paused Squat",
            "Box Squat",
            "Tempo Squat",
            "Front Squat",
            "Bulgarian Split Squat",
            "Safety Bar Squat",
            "Leg Press"
        ),
        "BENCH" to listOf(
            "Bench Press",
            "Close Grip Bench",
            "Board Press",
            "Paused Bench",
            "Incline Bench",
            "Strict Press",
            "Dumbbell Press",
            "Tricep Pushdown"
        ),
        "DEADLIFT" to listOf(
            "Conventional Deadlift",
            "Sumo Deadlift",
            "Romanian Deadlift",
            "Stiff Leg Deadlift",
            "Block Pull",
            "Deficit Deadlift",
            "Trap Bar Deadlift",
            "Good Mornings"
        ),
        "BACK" to listOf(
            "Barbell Row",
            "Dumbbell Row",
            "Cable Row",
            "Lat Pulldown",
            "Pull-Ups",
            "Face Pulls"
        ),
        "SHOULDERS" to listOf(
            "Overhead Press",
            "Arnold Press",
            "Lateral Raise",
            "Front Raise",
            "Face Pulls"
        ),
        "LEGS" to listOf(
            "Leg Press",
            "Leg Curl",
            "Nordic Curls",
            "Walking Lunges",
            "Step Ups"
        ),
        "ACCESSORIES" to listOf(
            "Plank",
            "Ab Wheel",
            "V Ups",
            "Farmer Carry",
            "Sled Push",
            "GHR"
        )
    )
}
