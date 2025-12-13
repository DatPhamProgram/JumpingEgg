package com.datpv.myapplication.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.datpv.myapplication.unit.AdFrequencyStore
import com.datpv.myapplication.view.theme.JumpingEggTheme
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MobileAds.initialize(this)

        CoroutineScope(Dispatchers.IO).launch {
            AdFrequencyStore.resetAllCounts(this@MainActivity)
        }
        enableEdgeToEdge()
        setContent {
            JumpingEggTheme {
                val navController = rememberNavController()
                AppNavigation(navController = navController)
            }
        }
    }
}

@Composable
fun AppNavigation (navController: NavHostController) {

    NavHost(
        navController = navController,
        startDestination = "home"
    ){
        composable ("home") {
            HomeScreen(
                onStartGameClick = {navController.navigate("game")},
                onRankingClick = {navController.navigate("ranking")},
                onInstructionClick = {navController.navigate("instruction")},
                onDoaGameClick = {navController.navigate("doa")}

            )
        }

        // Instruction screen
        composable ("instruction") {
            InstructionScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("game") {
            GameScreen (
                onGameOver = { finalScore ->
                    navController.navigate("endGame/$finalScore") {
                        // optional: remove game khỏi backstack để khỏi quay lại game đang chạy
                        popUpTo("game") { inclusive = true }
                    }
                }
            )
        }

        composable("endgame/{score}") { backStackEntry ->
            val score = backStackEntry.arguments?.getString("score")?.toIntOrNull() ?: 0
            EndGameScreen(
                score = score,
                onPlayAgain = {
                    navController.navigate("game")
                    {
                        popUpTo("home")
                    }
                              },
                onBackHome = { navController.popBackStack("home", false) }
            )
        }


        composable("ranking") {
            RankingScreen(
                onBack = { navController.popBackStack() }
            )

        }
        composable("doa") {
           DOAGameScreen (
               onBack = {navController.popBackStack()}
           )
        }

    }
}


