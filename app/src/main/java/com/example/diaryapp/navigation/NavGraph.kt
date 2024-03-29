package com.example.diaryapp.navigation


import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder

import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.diaryapp.data.repository.MongoDb
import com.example.diaryapp.model.GalleryImage
import com.example.diaryapp.model.Mood
import com.example.diaryapp.presentation.components.DisplayAlertDialog
import com.example.diaryapp.presentation.screens.auth.AuthenticationScreen
import com.example.diaryapp.presentation.screens.auth.AuthenticationViewModel
import com.example.diaryapp.presentation.screens.home.HomeScreen
import com.example.diaryapp.presentation.screens.home.HomeViewModel
import com.example.diaryapp.presentation.screens.write.WriteScreen
import com.example.diaryapp.presentation.screens.write.WriteViewModel
import com.example.diaryapp.util.Constants.APP_ID
import com.example.diaryapp.util.Constants.WRITE_SCREEN_ARGUMENT_KEY
import com.example.diaryapp.model.RequestState
import com.example.diaryapp.model.rememberGalleryState
import com.stevdzasan.messagebar.rememberMessageBarState
import com.stevdzasan.onetap.rememberOneTapSignInState
import io.realm.kotlin.mongodb.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception

@Composable
fun SetupNavGraph(
    startDestination: String,
    navController: NavHostController,
    onDataLoaded: () -> Unit
) {
    NavHost(navController = navController, startDestination = startDestination) {
        authenticationRoute(
            navigateToHome = {
                navController.popBackStack()
                navController.navigate(Screen.home.route)
            },
            onDataLoaded = onDataLoaded
        )
        homeRoute(
            navigateToWrite = {
            navController.navigate(Screen.write.route)
        },
            navigateToAuth = {
                navController.popBackStack()
                navController.navigate(Screen.Authentication.route)
            },
            onDataLoaded = onDataLoaded,
            navigateToWriteWithArgs = {
                navController.navigate(Screen.write.passDiaryId(diaryId = it))
            }
        )
        writeRoute(
            onBackPressed = {
                navController.popBackStack()
            },
            onDataLoaded = onDataLoaded
        )
    }
}

fun NavGraphBuilder.authenticationRoute(
    navigateToHome: () -> Unit,
    onDataLoaded: () -> Unit
) {
    composable(route = Screen.Authentication.route) {
        val oneTapState = rememberOneTapSignInState()
        val messageBarState = rememberMessageBarState()
        val viewModel: AuthenticationViewModel = viewModel()
        val loadingState by viewModel.loadingState
        val authenticated by viewModel.authenticated

        LaunchedEffect(key1 = Unit) {
            onDataLoaded()
        }

        AuthenticationScreen(
            authenticated = authenticated,
            loadingState = loadingState,
            oneTapState = oneTapState,
            messageBarState = messageBarState,
            onSuccesfulFirebaseSignIn = { tokenId ->
                viewModel.signInMongoAtlas(
                    tokenId = tokenId,
                    onSucces = {
                        messageBarState.addSuccess("Succesfully authentication")
                        viewModel.setLoading(false)
                    },
                    onError = {
                        messageBarState.addError(it)
                        viewModel.setLoading(false)
                    })
            },
            onFailedFirebaseSignIn = { message ->
                messageBarState.addError(Exception(message))
                viewModel.setLoading(false)
            },
            onDialogDismiss = { message ->
                messageBarState.addError(Exception(message))
                viewModel.setLoading(false)
            },
            onButtonClicked = {
                oneTapState.open()
                viewModel.setLoading(true)
            },
            navigateToHome = navigateToHome
        )
    }
}

fun NavGraphBuilder.homeRoute(
    navigateToWrite: () -> Unit,
    navigateToWriteWithArgs: (String) -> Unit,
    navigateToAuth: () -> Unit,
    onDataLoaded: () -> Unit
) {
    composable(route = Screen.home.route) {
        val viewModel: HomeViewModel = viewModel()
        val diaries by viewModel.diaries
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        var signOutDialogOpened by remember {
            mutableStateOf(false)
        }

        LaunchedEffect(key1 = diaries) {
            if (diaries !is RequestState.Loading) {
                onDataLoaded()
            }
        }

        HomeScreen(
            diaries = diaries,
            drawerState = drawerState,
            onMenuClicked = {
                scope.launch {
                    drawerState.open()
                }
            },
            signOutClicked = { signOutDialogOpened = true },
            navigateToWrite = navigateToWrite,
            navigateToWriteWithArgs = navigateToWriteWithArgs
        )
        LaunchedEffect(key1 = Unit) {
            MongoDb.configureTheRealm()
        }

        DisplayAlertDialog(
            title = "Sign Out",
            message = "Are you sure you want to Sign Out from your Google Account",
            dialogOpened = signOutDialogOpened,
            onDialogClosed = { signOutDialogOpened = false },
            onYesClicked = {
                scope.launch(Dispatchers.IO) {
                    val user = App.Companion.create(APP_ID).currentUser
                    if (user != null) {
                        user.logOut()
                        withContext(Dispatchers.Main) {
                            navigateToAuth()
                        }
                    }
                }
            }
        )
    }
}
@OptIn(ExperimentalFoundationApi::class)
fun NavGraphBuilder.writeRoute(
    onBackPressed: () -> Unit,
    onDataLoaded: () -> Unit
) {
    composable(route = Screen.write.route,
        arguments = listOf(navArgument(name = WRITE_SCREEN_ARGUMENT_KEY) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        })
    ) {
        LaunchedEffect(key1 = Unit) {
            onDataLoaded()
        }
        val viewModel: WriteViewModel = viewModel()
        val uiState = viewModel.uiState
        val context = LocalContext.current
        val pagerState = rememberPagerState(pageCount = { Mood.values().size })
        val pageNumber by remember {
            derivedStateOf { pagerState.currentPage }
        }
        val galleryState = viewModel.galleryState


        LaunchedEffect(key1 = uiState, block = {
            Log.d("Selecteddiary", "${uiState.selectedDiaryId}")
        })

        WriteScreen(
            uiState = uiState,
            moodName = { Mood.values()[pageNumber].name },
            onTitleChanged = { viewModel.setTitle(title = it) },
            onDescriptionChanged = { viewModel.setDescription(description = it) },
            pagerState = pagerState,
            onDeleteConfirmed = { viewModel.deleteDiary(
                onSuccess = {
                    Toast.makeText(
                        context,
                        "Deleted",
                        Toast.LENGTH_SHORT
                    ).show()
                    onBackPressed()
                },
                onError = { message ->
                    Toast.makeText(
                        context,
                        message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )},
            onDateTimeUpdated = { viewModel.updateDateTime(zonedDateTime = it) },
            onBackPressed = onBackPressed,
            onSaveClicked = {
                viewModel.upsertDiary(
                    diary = it.apply { mood = Mood.values()[pageNumber].name },
                    onSuccess = { onBackPressed() },
                    onError = { message ->
                        Toast.makeText(
                            context,
                            message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            },
            galleryState = galleryState,
            onImageSelect = {
                val type = context.contentResolver.getType(it)?.split("/")?.last() ?: "jpg"
                Log.d("WriteViewModel", "URI: $it")
                viewModel.addImage(
                    image = it,
                    imageType = type
                )

            }
        )
    }
}