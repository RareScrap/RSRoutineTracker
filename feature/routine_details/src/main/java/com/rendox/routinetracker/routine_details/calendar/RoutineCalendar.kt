package com.rendox.routinetracker.routine_details.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.OutDateStyle
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.nextMonth
import com.kizitonwose.calendar.core.previousMonth
import com.rendox.routinetracker.core.model.HistoricalStatus
import com.rendox.routinetracker.core.model.PlanningStatus
import com.rendox.routinetracker.core.model.RoutineStatus
import com.rendox.routinetracker.core.model.StatusEntry
import com.rendox.routinetracker.core.ui.helpers.LocalLocale
import com.rendox.routinetracker.core.ui.theme.routineStatusColors
import com.rendox.routinetracker.feature.routine_details.R
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun RoutineCalendar(
    modifier: Modifier = Modifier,
    currentMonth: YearMonth,
    firstDayOfWeek: DayOfWeek,
    routineStatuses: List<StatusEntry>,
    streakDates: List<LocalDate>,
    today: LocalDate,
) {
    CalendarMonthlyPaged(
        modifier = modifier,
        initialMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek,
        dayContent = { calendarDay ->
            RoutineStatusDay(
                day = calendarDay,
                routineStatus = routineStatuses.find {
                    it.date.toJavaLocalDate() == calendarDay.date
                }?.status,
                includedInAStreak = streakDates.contains(calendarDay.date.toKotlinLocalDate()),
                today = today,
            )
        },
    )
}

@Composable
private fun CalendarMonthlyPaged(
    modifier: Modifier = Modifier,
    initialMonth: YearMonth,
    firstDayOfWeek: DayOfWeek,
    dayContent: @Composable BoxScope.(CalendarDay) -> Unit,
) {
    val daysOfWeek = remember { daysOfWeek(firstDayOfWeek) }
    val startMonth = remember { initialMonth.minusMonths(100) }
    val endMonth = remember { initialMonth.plusMonths(100) }

    val calendarState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = initialMonth,
        firstDayOfWeek = daysOfWeek.first(),
        outDateStyle = OutDateStyle.EndOfGrid,
    )

    val coroutineScope = rememberCoroutineScope()
    Column(modifier = modifier) {
        CalendarTitle(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            currentMonth = calendarState.firstVisibleMonth.yearMonth,
            navigateToPrevious = {
                coroutineScope.launch {
                    calendarState.animateScrollToMonth(
                        calendarState.firstVisibleMonth.yearMonth.previousMonth
                    )
                }
            },
            navigateToNext = {
                coroutineScope.launch {
                    calendarState.animateScrollToMonth(
                        calendarState.firstVisibleMonth.yearMonth.nextMonth
                    )
                }
            },
        )
        HorizontalCalendar(
            state = calendarState,
            dayContent = dayContent,
            monthHeader = {
                DaysOfWeekTitles(
                    modifier = Modifier.fillMaxWidth(), daysOfWeek = daysOfWeek
                )
            }
        )
    }
}

@Composable
private fun RoutineStatusDay(
    modifier: Modifier = Modifier,
    day: CalendarDay,
    routineStatus: RoutineStatus?,
    includedInAStreak: Boolean,
    today: LocalDate,
) {
    val completedStroke = MaterialTheme.routineStatusColors.completedStroke
    val completedBackground = MaterialTheme.routineStatusColors.completedBackground

    val failedStroke = MaterialTheme.routineStatusColors.failedStroke
    val failedBackground = MaterialTheme.routineStatusColors.failedBackground

    val skippedBackground =
        if (includedInAStreak) MaterialTheme.routineStatusColors.completedBackgroundLight
        else MaterialTheme.routineStatusColors.failedBackgroundLight
    val skippedStroke =
        if (includedInAStreak) MaterialTheme.routineStatusColors.completedStroke
        else MaterialTheme.routineStatusColors.failedStroke

    val onVacationBackground = MaterialTheme.routineStatusColors.vacationBackground
    val onVacationStroke = MaterialTheme.routineStatusColors.vacationStroke

    val currentDateColor = MaterialTheme.colorScheme.primary
    val currentDateTextColor = MaterialTheme.colorScheme.onPrimary

    val backgroundColor: Color = when (day.position) {
        DayPosition.InDate, DayPosition.OutDate -> Color.Transparent
        else -> when (routineStatus) {
            null -> Color.Transparent

            is PlanningStatus -> {
                if (day.date == today.toJavaLocalDate()) currentDateColor
                else when (routineStatus) {
                    PlanningStatus.Planned -> MaterialTheme.colorScheme.outlineVariant
                    PlanningStatus.Backlog -> MaterialTheme.colorScheme.outlineVariant
                    PlanningStatus.AlreadyCompleted -> Color.Transparent
                    PlanningStatus.NotDue -> Color.Transparent
                    PlanningStatus.OnVacation -> Color.Transparent
                }
            }

            HistoricalStatus.NotCompleted -> failedBackground
            HistoricalStatus.Completed -> completedBackground
            HistoricalStatus.OverCompleted -> completedBackground
            HistoricalStatus.OverCompletedOnVacation -> completedBackground
            HistoricalStatus.SortedOutBacklog -> completedBackground
            HistoricalStatus.SortedOutBacklogOnVacation -> completedBackground
            HistoricalStatus.Skipped -> skippedBackground
            HistoricalStatus.NotCompletedOnVacation -> onVacationBackground
            HistoricalStatus.CompletedLater -> skippedBackground
            HistoricalStatus.AlreadyCompleted -> skippedBackground
        }
    }

    val strokeColor: Color = when (day.position) {
        DayPosition.InDate, DayPosition.OutDate -> Color.Transparent
        else -> when (routineStatus) {
            null -> if (day.date == today.toJavaLocalDate()) currentDateColor else Color.Transparent

            is PlanningStatus -> {
                if (day.date == today.toJavaLocalDate()) currentDateColor
                else when (routineStatus) {
                    PlanningStatus.Planned -> MaterialTheme.colorScheme.outlineVariant
                    PlanningStatus.Backlog -> MaterialTheme.colorScheme.outlineVariant
                    PlanningStatus.AlreadyCompleted -> Color.Transparent
                    PlanningStatus.NotDue -> Color.Transparent
                    PlanningStatus.OnVacation -> Color.Transparent
                }
            }

            HistoricalStatus.NotCompleted -> failedStroke
            HistoricalStatus.Completed -> completedStroke
            HistoricalStatus.OverCompleted -> completedStroke
            HistoricalStatus.OverCompletedOnVacation -> completedStroke
            HistoricalStatus.SortedOutBacklog -> completedStroke
            HistoricalStatus.SortedOutBacklogOnVacation -> completedStroke
            HistoricalStatus.Skipped -> skippedStroke
            HistoricalStatus.NotCompletedOnVacation -> onVacationStroke
            HistoricalStatus.CompletedLater -> skippedStroke
            HistoricalStatus.AlreadyCompleted -> skippedStroke
        }
    }

    Box(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .aspectRatio(1f)
            .background(color = backgroundColor, shape = CircleShape)
            .border(border = BorderStroke(width = 2.dp, color = strokeColor), shape = CircleShape)
    ) {
        Text(
            modifier = Modifier.align(Alignment.Center),
            text = day.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = when (day.position) {
                DayPosition.InDate, DayPosition.OutDate -> MaterialTheme.colorScheme.outlineVariant
                else -> {
                    if (day.date == today.toJavaLocalDate()) currentDateTextColor
                    else MaterialTheme.colorScheme.onSurface
                }
            },
        )
    }
}

@Composable
private fun DaysOfWeekTitles(
    modifier: Modifier = Modifier, daysOfWeek: List<DayOfWeek>
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        for (dayOfWeek in daysOfWeek) {
            Text(
                modifier = Modifier.padding(bottom = 4.dp),
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, LocalLocale.current),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CalendarTitle(
    modifier: Modifier = Modifier,
    currentMonth: YearMonth,
    navigateToPrevious: () -> Unit,
    navigateToNext: () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        CalendarNavigationIcon(
            imageVector = Icons.Default.KeyboardArrowLeft,
            contentDescription = stringResource(
                id = R.string.calendar_left_navigation_icon_content_description
            ),
            onClick = navigateToPrevious,
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val locale = LocalLocale.current
            val monthDisplayName = remember(currentMonth, locale) {
                /*
                The month's number should be displayed in the FULL_STANDALONE format (LLLL),
                however, for some reason, on Android, it displays month numbers instead.
                On the other hand, FULL (MMMM) format would return incorrect names in Slavic
                languages (e.g. in Ukrainian, it would return "жовтня", not "жовтень"). Luckily,
                this issue can be worked around by leveraging java.util.Date class, although it
                is ugly.
                */
                val someDateInMonth = currentMonth.atDay(3)
                val randomOffset = ZoneOffset.MIN // it doesn't matter
                val date =
                    java.util.Date.from(someDateInMonth.atStartOfDay().toInstant(randomOffset))
                println("derived java util date = $date")
                val fullStandaloneMonthNameFormatter = SimpleDateFormat("LLLL", locale)
                fullStandaloneMonthNameFormatter.format(date).replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(locale) else it.toString()
                }
            }

            Text(
                modifier = Modifier.paddingFromBaseline(bottom = 6.dp),
                text = monthDisplayName,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
            )

            Text(
                text = currentMonth.year.toString(),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }

        CalendarNavigationIcon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = stringResource(
                id = R.string.calendar_right_navigation_icon_content_description
            ),
            onClick = navigateToNext,
        )
    }
}

@Composable
private fun CalendarNavigationIcon(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        modifier = modifier,
        onClick = onClick,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Preview(
    showSystemUi = true, showBackground = false, backgroundColor = 0xFF1A1B1E,
    wallpaper = Wallpapers.NONE,
)
@Composable
private fun RoutineCalendarPreview() {
    CompositionLocalProvider(LocalLocale provides Locale.ITALIAN) {
        Box(modifier = Modifier.fillMaxSize()) {
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .wrapContentSize(),
            ) {
                RoutineCalendar(
                    currentMonth = YearMonth.of(2023, java.time.Month.NOVEMBER),
                    firstDayOfWeek = DayOfWeek.MONDAY,
                    routineStatuses = statusList.mapIndexed { dayNumber, status ->
                        StatusEntry(
                            date = routineStartDate.plus(DatePeriod(days = dayNumber)),
                            status = status,
                        )
                    },
                    streakDates = streakDates,
                    today = LocalDate(2023, Month.NOVEMBER, 23),
                )
            }
        }
    }
}

val routineStartDate = LocalDate(2023, Month.NOVEMBER, 1)

val statusList: List<RoutineStatus> = listOf(
    HistoricalStatus.Completed,                 // 2023-11-1
    HistoricalStatus.Completed,                 // 2023-11-2
    HistoricalStatus.Skipped,                   // 2023-11-3
    HistoricalStatus.Completed,                 // 2023-11-4
    HistoricalStatus.Completed,                 // 2023-11-5
    HistoricalStatus.Skipped,                   // 2023-11-6
    HistoricalStatus.Skipped,                   // 2023-11-7

    HistoricalStatus.NotCompleted,              // 2023-11-8
    HistoricalStatus.Skipped,                   // 2023-11-9
    HistoricalStatus.Skipped,                   // 2023-11-10
    HistoricalStatus.OverCompleted,             // 2023-11-11
    HistoricalStatus.Completed,                 // 2023-11-12
    HistoricalStatus.NotCompletedOnVacation,    // 2023-11-13
    HistoricalStatus.NotCompletedOnVacation,    // 2023-11-14

    HistoricalStatus.OverCompletedOnVacation,   // 2023-11-15
    HistoricalStatus.Completed,                 // 2023-11-16
    HistoricalStatus.CompletedLater,            // 2023-11-17
    HistoricalStatus.SortedOutBacklog,          // 2023-11-18
    HistoricalStatus.OverCompleted,             // 2023-11-19
    HistoricalStatus.AlreadyCompleted,          // 2023-11-20
    HistoricalStatus.Skipped,                   // 2023-11-21

    HistoricalStatus.Completed,                 // 2023-11-22
    PlanningStatus.Backlog,                     // 2023-11-23
    PlanningStatus.Backlog,                     // 2023-11-24
    PlanningStatus.Planned,                     // 2023-11-25
    PlanningStatus.Planned,                     // 2023-11-26
    PlanningStatus.Planned,                     // 2023-11-27
    PlanningStatus.NotDue,                      // 2023-11-28

    PlanningStatus.AlreadyCompleted,            // 2023-11-29
    PlanningStatus.OnVacation,                  // 2023-11-30
)

val streakDates = mutableListOf<LocalDate>().apply {
    for (dayOfMonth in 1..7) {
        add(LocalDate(2023, 11, dayOfMonth))
    }
    for (dayOfMonth in 11..30) {
        add(LocalDate(2023, 11, dayOfMonth))
    }
}