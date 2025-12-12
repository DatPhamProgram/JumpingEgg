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
import com.datpv.myapplication.view.theme.JumpingEggTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                onBack = {
                    navController.popBackStack()
                }
            )

        }
        composable("ranking") {
            SimpleStubScreen("Ranking Screen") { navController.popBackStack() }
        }
        composable("doa") {
            SimpleStubScreen("D.O.A Game Screen") { navController.popBackStack() }
        }
    }
}

@Composable
private fun SimpleStubScreen(
    title: String,
    onBack: () -> Unit
) {
    Button(
        onClick = onBack,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Text(text = "$title - Back")
    }
}
