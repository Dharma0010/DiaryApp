package com.example.diaryapp.presentation.screens.write

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.diaryapp.model.Diary
import com.example.diaryapp.model.Mood
import java.time.ZonedDateTime

@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun WriteScreen(
    uiState: UiState,
    moodName: () -> String,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    pagerState: PagerState,
    onDeleteConfirmed: ()-> Unit,
    onDateTimeUpdated: (ZonedDateTime) -> Unit,
    onBackPressed: () -> Unit,
    onSaveClicked: (Diary) -> Unit
) {
    LaunchedEffect(key1 = uiState.mood, block = {
        pagerState.scrollToPage(Mood.valueOf(uiState.mood.name).ordinal)
    })
    Scaffold(
        topBar = {
            WriteTopBar(
                onBackPressed = onBackPressed,
                onDeleteConfirmed = onDeleteConfirmed,
                selectedDiary = uiState.selectedDiary,
                moodName = moodName,
                onDateTimeUpdated = onDateTimeUpdated
            )
        },
        content = {
            WriteContent(
                pagerState = pagerState,
                title = uiState.title,
                onTitleChanged = onTitleChanged,
                description = uiState.description,
                onDescriptionChanged = onDescriptionChanged,
                paddingValues = it,
                uiState = uiState,
                onSaveClicked = onSaveClicked
            )
        }
    )
}