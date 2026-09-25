package com.maxtasy.wakku.ui.theme

import androidx.compose.ui.graphics.Color

// Shared palette with the Expense Tracker web app (its Tailwind `@theme`
// tokens). Dark-only, like that app. Names mirror the web tokens where one
// exists; the rest are derived tints needed to fill Material3 roles.

// Neutrals
val WakkuBackground = Color(0xFF0B0E14)
val WakkuSurface = Color(0xFF12161F)
val WakkuSurfaceHover = Color(0xFF171C27)
val WakkuBorder = Color(0xFF1F2430)
val WakkuFg = Color(0xFFE8EAF0)
val WakkuFgMuted = Color(0xFF8890A3)
// Derived: visible-enough outline for text fields / unchecked switches
// (WakkuBorder alone is under 3:1 against the background).
val WakkuOutline = Color(0xFF3A4050)

// Accent (indigo)
val WakkuAccent = Color(0xFF4F46E5)
// Used as Material3 `primary` rather than WakkuAccent: primary is also the
// text color of TextButtons, and #4F46E5 is only ~3:1 on the background.
val WakkuAccentHover = Color(0xFF6366F1)
val WakkuAccentFg = Color(0xFFF5F5FF)
// Derived indigo tints for chips / containers.
val WakkuAccentSoft = Color(0xFF818CF8)
val WakkuAccentContainer = Color(0xFF1E1B4B)
val WakkuOnAccentContainer = Color(0xFFC7D2FE)

// Danger (red)
val WakkuDanger = Color(0xFFF87171)
val WakkuDangerHover = Color(0xFFFCA5A5)
val WakkuOnDanger = Color(0xFF1F0A0A)
val WakkuDangerContainer = Color(0xFF3B1414)

// Amber ("expense" in the web app) — used here for warnings.
val WakkuAmber = Color(0xFFF59E0B)
val WakkuAmberHover = Color(0xFFFBBF24)
val WakkuAmberFg = Color(0xFF1C1206)
val WakkuAmberContainer = Color(0xFF2A1D06)
